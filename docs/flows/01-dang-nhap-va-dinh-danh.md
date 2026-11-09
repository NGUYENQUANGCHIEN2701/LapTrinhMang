# 01. Đăng ký, đăng nhập và định danh người chơi

**Cập nhật:** phiên đăng nhập thật giữ `AccountPrincipal` kèm ID DB. `AccountStatusFilter` kiểm tra tài khoản còn tồn tại/được bật ở mỗi request bảo vệ; khóa/xóa làm phiên cũ bị từ chối, và admin đóng socket tương ứng. Đăng ký kiểm tra thêm độ dài email/tên hiển thị/password và xử lý trùng tên do race. Các trích code có số dòng bên dưới là phần mô tả lịch sử; tìm theo tên hàm khi đối chiếu source mới.

**Tóm tắt:** Spring Security quản lý đăng nhập bằng session cookie. Sau khi đăng nhập, mọi request được quy về một `playerId` dạng `user:<tên>`, và toàn bộ dữ liệu game (tiến trình, kết quả, ví) đều gắn với khoá này.

**File chính:** [SecurityConfig.java](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java), [AuthController.java](../../server-spring/src/main/java/com/tankzors/security/AuthController.java), [CustomUserDetailsService.java](../../server-spring/src/main/java/com/tankzors/security/CustomUserDetailsService.java), [PlayerIdentityResolver.java](../../server-spring/src/main/java/com/tankzors/identity/PlayerIdentityResolver.java)

## Sơ đồ

```text
ĐĂNG KÝ   POST /register
  └─ processRegister()                  AuthController.java:58
       kiểm tra tên, mật khẩu, trùng tên
       băm mật khẩu BCrypt, lưu app_user với ROLE_USER
       redirect /login

ĐĂNG NHẬP POST /login   (không có controller, Spring Security tự xử lý)
  └─ loadUserByUsername()               CustomUserDetailsService.java:27
       đọc app_user, so khớp mật khẩu BCrypt
       thành công → tạo session, cookie JSESSIONID, redirect /menu
       thất bại  → redirect /login?error=true

MỌI REQUEST SAU ĐÓ
  └─ resolve()                          PlayerIdentityResolver.java:28
       trả về "user:<tên>"
```

## Bước 1: Đăng ký

[AuthController.java:60-86](../../server-spring/src/main/java/com/tankzors/security/AuthController.java#L60-L86) kiểm tra dữ liệu form: tên 3 đến 50 ký tự, mật khẩu ít nhất 6 ký tự, xác nhận mật khẩu khớp, tên chưa tồn tại. Lỗi thì quay lại `/register` kèm thông báo.

Mật khẩu không bao giờ được lưu dạng gốc:

[AuthController.java:89-101](../../server-spring/src/main/java/com/tankzors/security/AuthController.java#L89-L101)
```java
UserEntity user = new UserEntity();
user.setUsername(form.getUsername().trim());
user.setEmail(form.getEmail());
user.setPassword(passwordEncoder.encode(form.getPassword()));
// ...
user.setRole("ROLE_USER");
user.setEnabled(true);
// ...
userRepository.save(user);
```

`passwordEncoder` là [BCryptPasswordEncoder](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java#L29-L32).

## Bước 2: Đăng nhập

Không có controller nào xử lý `POST /login`. Cấu hình ở đây bảo Spring Security tự làm:

[SecurityConfig.java:51-57](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java#L51-L57)
```java
.formLogin(form -> form
    .loginPage("/login")
    .loginProcessingUrl("/login")
    .defaultSuccessUrl("/menu", true)
    .failureUrl("/login?error=true")
    .permitAll()
)
```

Spring gọi `CustomUserDetailsService` để lấy tài khoản. Nếu quản trị viên đã khoá tài khoản (`enabled = false`), đăng nhập sẽ bị từ chối:

[CustomUserDetailsService.java:36-41](../../server-spring/src/main/java/com/tankzors/security/CustomUserDetailsService.java#L36-L41)
```java
return User.builder()
    .username(entity.getUsername())
    .password(entity.getPassword())
    .disabled(!entity.isEnabled())
    .authorities(authorities)
    .build();
```

`AuthController` chỉ [hiển thị trang login](../../server-spring/src/main/java/com/tankzors/security/AuthController.java#L44-L48).

## Bước 3: Ai được vào đâu

[SecurityConfig.java:37-47](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java#L37-L47)
```java
.authorizeHttpRequests(auth -> auth
    // Static resources — public
    .requestMatchers("/css/**", "/js/**", "/images/**", "/play/**", "/game/**",
                     "/favicon.ico", "/webjars/**").permitAll()
    // Public pages
    .requestMatchers("/", "/menu", "/register", "/api/health", "/error/**").permitAll()
    // Admin-only
    .requestMatchers("/admin/**").hasRole("ADMIN")
    // Everything else requires authentication
    .anyRequest().authenticated()
)
```

| Đường dẫn | Quyền |
|---|---|
| `/`, `/menu`, `/register`, `/api/health`, `/play/**`, file tĩnh, `/game/**` (bundle game) | Công khai |
| `/admin/**` | `ROLE_ADMIN` |
| Còn lại: `/campaigns`, `/game-menu`, `/leaderboard`, `/api/game/**`, ... | Phải đăng nhập |

## Bước 4: Biến request thành playerId

Đây là hàm được gọi ở gần như mọi controller của game. Thứ tự ưu tiên:

[PlayerIdentityResolver.java:28-50](../../server-spring/src/main/java/com/tankzors/identity/PlayerIdentityResolver.java#L28-L50)
```java
public String resolve(HttpServletRequest request) {
    if (request == null) return "anon";
    // 1. Tài khoản đã đăng nhập phải luôn thắng header/session để dữ liệu
    // campaign không đổi khi JSESSIONID được tạo lại.
    Principal principal = request.getUserPrincipal();
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
        return "user:" + principal.getName();
    }
    // 2. Thuộc tính trong HttpSession (identity ứng dụng).
    HttpSession session = request.getSession(false);
    if (session != null) {
        Object attr = session.getAttribute(SESSION_ATTR);
        if (attr instanceof String s && !s.isBlank()) return s;
    }
    // 3. Fallback: tạo session mới + phát id ổn định cho phiên.
    HttpSession newSession = request.getSession(true);
    String sid = newSession.getId();
    newSession.setAttribute(SESSION_ATTR, sid);
    return sid;
}
```

Với người chơi đã đăng nhập, nhánh 1 luôn chạy, nên `playerId` không đổi dù cookie phiên được tạo lại. Server không nhận danh tính từ header do client gửi (header `X-Player-Session` cũ đã bị gỡ vì ai cũng giả được).

**Vì sao game không tự gửi playerId?** Game chạy trong iframe cùng nguồn gốc với website, nên trình duyệt tự đính kèm cookie `JSESSIONID` vào mọi lời gọi API. Server tự biết ai đang gọi. Thân yêu cầu không có trường nào quyết định danh tính, nhờ đó client không thể giả mạo tài khoản khác.

`"browser-..."` trong [HtmlLauncher.java:97-100](../../game-client/html/src/main/java/com/tankzors/client/html/HtmlLauncher.java#L97-L100) chỉ là id của xe trong mô phỏng cục bộ, không gửi lên server.

## CSRF

[SecurityConfig.java:65-72](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java#L65-L72) miễn kiểm tra CSRF cho `/api/game/**`, vì mã JavaScript của game không đọc được token trong form Thymeleaf. Nếu không miễn, `POST /api/game/results` bị chặn và màn kế không bao giờ được mở. Các form Thymeleaf và các POST khác vẫn cần token.

## Tài khoản quản trị

[DataInitializer.java:26-47](../../server-spring/src/main/java/com/tankzors/security/DataInitializer.java#L26-L47) tạo sẵn tài khoản `admin` ở lần khởi động đầu tiên, nếu chưa có. Mật khẩu cố định trong code, cần đổi khi triển khai thật.

[AdminController.java](../../server-spring/src/main/java/com/tankzors/security/AdminController.java) có 3 chức năng: [xem thống kê](../../server-spring/src/main/java/com/tankzors/security/AdminController.java#L59-L60), [khoá/mở khoá](../../server-spring/src/main/java/com/tankzors/security/AdminController.java#L107-L108) và [xoá tài khoản](../../server-spring/src/main/java/com/tankzors/security/AdminController.java#L129-L130).

## Dữ liệu thay đổi

| Bảng | Khi nào |
|---|---|
| `app_user` | Đăng ký, quản trị viên khoá hoặc xoá tài khoản |

Session đăng nhập nằm trong bộ nhớ server, không nằm trong database.

## Test kiểm chứng

- [PlayerIdentityResolverTest.java](../../server-spring/src/test/java/com/tankzors/identity/PlayerIdentityResolverTest.java): tên tài khoản được ưu tiên hơn session trình duyệt.

Lưu ý: `SecurityConfig` có [`@Profile("!test")`](../../server-spring/src/main/java/com/tankzors/security/SecurityConfig.java#L26), nên test không dùng cấu hình bảo mật này.
