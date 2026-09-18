package com.tankzors.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tankzors.web.PageModelFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Controller xử lý đăng nhập / đăng ký.
 *
 * <p>Login form submit được Spring Security tự xử lý tại POST /login
 * (xem {@link SecurityConfig}). Controller này chỉ render trang login.</p>
 *
 * <p>Register: tạo tài khoản {@code ROLE_USER} mới trong database.</p>
 */
@Controller
@RequestMapping
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PageModelFactory pageModelFactory;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          PageModelFactory pageModelFactory) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pageModelFactory = pageModelFactory;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        pageModelFactory.apply(model, "Login", "login");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        pageModelFactory.apply(model, "Register", "register");
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute RegisterForm form,
                                  RedirectAttributes redirectAttributes) {
        // Validate
        if (form.getUsername() == null || form.getUsername().isBlank()
            || form.getUsername().length() < 3) {
            redirectAttributes.addFlashAttribute("flashError",
                "Tên đăng nhập phải có ít nhất 3 ký tự.");
            return "redirect:/register";
        }
        if (form.getUsername().length() > 50) {
            redirectAttributes.addFlashAttribute("flashError",
                "Tên đăng nhập tối đa 50 ký tự.");
            return "redirect:/register";
        }
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            redirectAttributes.addFlashAttribute("flashError",
                "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("flashError",
                "Mật khẩu xác nhận không khớp.");
            return "redirect:/register";
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            redirectAttributes.addFlashAttribute("flashError",
                "Tên đăng nhập đã tồn tại.");
            return "redirect:/register";
        }

        // Create user
        UserEntity user = new UserEntity();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setDisplayName(form.getDisplayName() != null && !form.getDisplayName().isBlank()
            ? form.getDisplayName().trim() : form.getUsername().trim());
        user.setRole("ROLE_USER");
        user.setEnabled(true);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);
        logger.info("[AuthController] New user registered: {}", user.getUsername());

        redirectAttributes.addFlashAttribute("flashSuccess",
            "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }

    /**
     * Form backing object cho trang register.
     */
    public static class RegisterForm {
        private String username;
        private String displayName;
        private String email;
        private String password;
        private String confirmPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}
