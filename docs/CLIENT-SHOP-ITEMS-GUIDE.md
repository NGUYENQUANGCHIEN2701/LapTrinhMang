# Hướng dẫn client: item shop đang hoạt động

Tài liệu này chỉ liệt kê item đã có luồng gameplay ở client: mở khóa/mua,
chọn hoặc kích hoạt trong trận, trừ kho và áp dụng hiệu ứng. Các item chỉ đang
hiển thị trong shop nhưng chưa có gameplay **không** được hướng dẫn ở đây.

## Điều khiển chung

1. Trong trận, nhấn `B` để mở Workshop, mua item hoặc nghiên cứu khi đã đủ
   vàng/sao và điều kiện mở khóa.
2. Nhấn `Q` để mở túi đồ.
3. Dùng `↑`/`↓` để chọn, nhấn `Enter` để xác nhận.
4. Với đạn/vũ khí, xác nhận trong `Q` để trang bị, sau đó nhấn `Space` hoặc
   `J` để bắn. Mỗi phát bắn trừ một đơn vị trong kho.
5. Với vật phẩm hỗ trợ, xác nhận trong `Q` để dùng ngay. Nếu điều kiện không
   phù hợp (ví dụ Repair khi HP đã đầy), item không bị trừ.

## Đạn thường

| Item | Mở khóa | Cách dùng | Hiệu quả |
|---|---:|---|---|
| Shell | Có sẵn | Chọn trong `Q`, bắn | 1 HP, đạn chuẩn. |
| Plasma | Nghiên cứu Đạn cấp 1 | Chọn, bắn | 1 HP, bay nhanh hơn Shell. |
| Double Shell | Cấp 2 | Chọn, bắn | 2 HP; hiển thị hai nòng. Gặp Shell/Plasma chỉ mất một nòng và tiếp tục bay. |
| Double Plasma | Cấp 3 | Chọn, bắn | 2 HP, hiển thị hai tia Plasma. |
| Armor Piercing | Cấp 4 | Chọn, bắn | 3 HP; khi phá đạn yếu hơn, damage giảm theo damage đã chặn nhưng viên đạn còn damage vẫn bay tiếp. |
| Reinforced AP | Cấp 5 | Chọn, bắn | 5 HP; cùng cơ chế xuyên đạn như Armor Piercing. |

Đạn Double tối đa 40 viên; Armor Piercing/Reinforced AP tối đa 10 viên. Mua
**Enh. Chassis** sẽ nâng các giới hạn này lên 99 viên.

## Vũ khí đặc biệt

| Item | Mở khóa | Cách dùng | Hiệu quả hiện có |
|---|---:|---|---|
| Artillery | Nghiên cứu Pháo binh cấp 1 | Chọn trong `Q`, bắn | Bay vượt tường/vật cản rồi nổ ở cuối tầm. Sát thương vùng giảm 1 mỗi ô tính từ tâm. Nghiên cứu cấp 2–5 tăng sức mạnh, tầm, tốc độ và bán kính. |

## Phòng thủ và hỗ trợ

| Item | Điều kiện | Cách dùng | Hiệu quả hiện có |
|---|---:|---|---|
| Temp Armor | Nghiên cứu Giáp cấp 1 | Mua tại `B` | +1 HP ngay. Giữ qua màn, nhưng mất một tấm khi xe bị phá hủy. |
| Perm Armor | Nghiên cứu Giáp cấp 2 | Mua tại `B` | +1 HP ngay và giữ qua các lần xe bị phá hủy. |
| Force Field | Nghiên cứu Khiên cấp 1 | Chọn trong `Q`, xác nhận | Lắp khiên lên xe, hấp thụ damage trước HP thân xe và tự hồi. Cấp nghiên cứu cao hơn hồi nhanh hơn; cấp 4 tăng sức chứa tối đa lên 7. Khi xe bị phá hủy, các tấm khiên đã lắp bị mất. |
| Repair Kit | Có sẵn | Chọn trong `Q`, xác nhận khi đang mất HP | Hồi đầy HP thân xe. Không dùng khi HP đã đầy. |
| Nitro Speed | Có sẵn | Chọn trong `Q`, xác nhận | Tăng tốc chạy và giảm hồi chiêu bắn trong 45 giây; Enh. Chassis tăng thêm 15 giây. |
| Freeze EMP | Có item trong kho | Chọn trong `Q`, xác nhận | Đóng băng toàn bộ xe tăng/đạn địch trong 25 giây; Enh. Chassis tăng thêm 5 giây. |
| Radar | Mua được khi mở khóa Radar | Chọn trong `Q`, xác nhận | Bật radar cho phần còn lại của trận. Radar không bị trừ khi bật. |
| Enh. Chassis | Mua một lần tại `B` | Tự áp dụng | +2 HP; mở giới hạn 99 cho Double/AP; tăng thời gian Nitro và Freeze. |
| Extra Life | Mua tại `B` | Tự áp dụng khi xe bị phá hủy | Tiêu thụ một lượt để hồi sinh tại điểm spawn. Temp Armor vẫn mất khi xe chết. |

## Item hiện chưa có hướng dẫn sử dụng

Các item sau có thể xuất hiện trong shop hoặc được khóa theo nghiên cứu, nhưng
chưa có gameplay hoàn chỉnh ở client nên không nên mua để sử dụng trong bản
hiện tại: **Warhead, Guided Missile, TMD-5 Mine, TMD-9 Mine, Dynamite
x70/x100/x160, Air Strike và Ally Tank cấp 1–3**.

## Lưu ý khi chơi lại màn

Chơi lại màn khôi phục kho đạn/vật phẩm về checkpoint đầu màn. Vàng, sao,
nghiên cứu và trang bị mua tại Workshop vẫn được lưu theo tài khoản. Armor mua
trong màn được đồng bộ vào checkpoint để không bị hoàn lại khi restart.
