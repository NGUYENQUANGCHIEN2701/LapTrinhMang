---
quick_id: 261006-gt2
status: complete
completed: 2026-10-06
code_commit: 7be3904
---

# Quick Task 261006-gt2 Summary

## Outcome

- Repair nhặt trên bản đồ hồi đầy HP ngay và không cộng vào kho dự trữ.
- Speed nhặt trên bản đồ kích hoạt tăng tốc ngay và không cộng vào kho dự trữ.
- Pickup vẫn biến mất sau khi nhặt và phát sự kiện âm thanh pickup/hiệu ứng.
- Repair/Speed có sẵn hoặc mua trong cửa hàng vẫn sử dụng qua menu và trừ đúng một vật phẩm.

## Verification

- Regression test được chạy đỏ trước khi sửa và xanh sau khi sửa.
- 95 core tests passed.
- Docker client stage chạy `html:dist`/GWT bằng JDK 17 thành công.
- Docker multi-stage Spring build passed; container mới trả HTTP 200 tại `http://localhost:8081/`.
- `git diff --check` passed.

## Commit

- `7be3904 fix(gameplay): apply map pickups immediately`
