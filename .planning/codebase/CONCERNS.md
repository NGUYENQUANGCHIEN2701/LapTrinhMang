# Mối quan tâm hiện tại

1. Co-op lobby và LibGDX session chưa dùng chung lifecycle/socket context.
2. Result campaign là client-reported; server clamp/idempotency có nhưng không chống cheat mạnh.
3. GWT/Gradle có deprecation warning; chưa ảnh hưởng build hiện tại nhưng cần theo dõi trước Gradle 9.
4. Map JSON tồn tại ở client assets và server resources; thay đổi schema cần kiểm tra drift.
5. Generated HTML5 bundle được commit để hỗ trợ chạy Spring local; Docker chủ động thay bằng bundle vừa build.
