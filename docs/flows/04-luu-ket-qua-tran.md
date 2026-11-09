# 04. Kết thúc trận và lưu kết quả

Client tính điểm và báo thắng/thua; server kiểm tra lượt và ghi dữ liệu tài khoản. Browser không tự cộng thưởng vào ví rồi gửi snapshot.

## Luồng

```text
GameSimulation kết thúc
  LocalCampaignSession.onMatchEnd
    chốt MatchResult một lần
    flush journal shop còn lại
    đặt resultSaved=false → CampaignPersistence.submit
      GwtAppBridge: xếp sau các giao dịch trước, persist outbox, POST results
        GameApiController: resolve session identity
          ResultService.submit trong transaction:
            validate DTO → acquire PlayerStateLock
            kiểm tra game_run owner + level/version
            kết quả đã có:
              cùng SHA-256 → trả metadata hiện tại, không credit lại
              khác payload → 409
            kết quả mới:
              kiểm tra hạn và quyền màn
              insert match_result
              thưởng = map + Extra Interest từ nghiên cứu đã lưu
              cộng ví + tăng revision + finish run
              thắng campaign: markVictory + best score
            commit → 200 saved
      callback.saved: nhận số dư, resultSaved=true
      callback.failed: hiện lý do, giữ trạng thái chưa lưu
```

Nguồn: [LocalCampaignSession](../../game-client/core/src/main/java/com/tankzors/client/session/LocalCampaignSession.java), [CampaignPersistence](../../game-client/core/src/main/java/com/tankzors/client/session/CampaignPersistence.java), [GwtAppBridge](../../game-client/html/src/main/java/com/tankzors/client/html/GwtAppBridge.java), [ResultService](../../server-spring/src/main/java/com/tankzors/result/ResultService.java).

## Điểm và thưởng

Điểm vẫn tính trong core:

```text
HP còn lại × 10 + số xe hạ × 200 + max(0, 1000 − số giây × 10)
```

Server giới hạn số liệu nhưng không replay để chứng minh điểm là thật. Thưởng thực ghi lấy từ metadata map và nghiên cứu server, không lấy số Xu/Sao client yêu cầu. DEFEAT không có thưởng. Kết quả, ví, tiến trình và leaderboard nằm trong cùng transaction, dưới khóa row tài khoản đến commit.

## Retry và chuyển màn

- Cùng run/owner/payload trả 200 `alreadySaved=true`; totals, revision, best score và unlocked levels là dữ liệu hiện tại.
- Cùng lượt, nội dung khác hoặc owner sai trả 409. Browser không coi đó là thành công.
- Lỗi mạng, 429 và 5xx được retry tối đa ba lần; body giữ nguyên. Các ghi sau không vượt hàng đợi.
- UI hiển thị đang lưu/lỗi và không cho next/restart/rời kết quả trước xác nhận. `R` thử lại khi lỗi tạm thời; 400/409 yêu cầu tải lại state server.
- Outbox lưu trong sessionStorage của đúng tài khoản. Reload cùng tab gửi lại trước bootstrap; không có bảo đảm giữ sau đóng tab/xóa/cấm storage.

Các request bị server từ chối được giữ riêng để chẩn đoán, không tự áp lại lên revision mới. Chi tiết mã HTTP, trường dữ liệu và giới hạn ở [networking](../networking.md).

## Test hồi quy

`ResultServiceTest` kiểm tra run tự đặt, owner khác, metadata/hạn, credit một lần, retry metadata, rollback và submit đồng thời. `LocalCampaignSessionTest` kiểm tra callback về chậm/lỗi, UI gate và không tự credit browser. Browser smoke kiểm tra mất response sau commit, 409 và reload outbox; xem [HTML5 README](../../game-client/html/README.md#kiểm-thử-browser-tùy-chọn).
