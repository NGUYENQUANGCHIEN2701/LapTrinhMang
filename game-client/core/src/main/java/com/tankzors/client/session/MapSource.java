package com.tankzors.client.session;

/**
 * Nguồn dữ liệu map JSON cho {@link LocalCampaignSession}.
 * Do core không được dùng ClassLoader/InputStream (GWT không dịch), nguồn map
 * do launcher (html/desktop) cung cấp.
 *
 * <p>HtmlLauncher: lấy qua REST {@code GET /api/game/bootstrap?levelId=...} (Bước D)
 * hoặc qua biến nhúng trong host page.</p>
 * <p>DesktopLauncher: đọc file từ assets/ thư mục.</p>
 *
 * <p>Nạp map có thể bất đồng bộ (REST trên HTML5), nên ta dùng {@link #requestLevel}
 * + {@link LevelCallback} thay vì giả định nguồn trả về đồng bộ. {@link #loadMapJson}
 * chỉ còn dùng cho đường nạp-sẵn khởi tạo (desktop JVM) — không dùng cho restart/next.</p>
 */
public interface MapSource {
    /**
     * Lấy nội dung JSON của một level (đồng bộ).
     * Chỉ dùng được cho source nạp-sẵn (desktop JVM). Source HTML5 trả null
     * vì bootstrap REST bất đồng bộ — phải dùng {@link #requestLevel}.
     * @param levelId mã level, vd {@code "campaign_1"}.
     * @return chuỗi JSON, hoặc null nếu không tìm thấy / phải tải bất đồng bộ.
     */
    String loadMapJson(String levelId);

    /**
     * Yêu cầu nạp một level (có thể bất đồng bộ) rồi gọi {@code callback}.
     * Default throw {@link UnsupportedOperationException} — các source thực tế
     * (BootstrapMapSource trên HTML5, DesktopMapSource trên desktop) override.
     *
     * <p>Phải gọi đúng một callback: {@link LevelCallback#onLevelLoaded} khi thành
     * công, hoặc {@link LevelCallback#onLoadError} khi lỗi. Không được gọi cả hai,
     * không được bỏ qua (session sẽ kẹt LOADING_NEXT).</p>
     *
     * @param levelId  mã level cần nạp.
     * @param callback nhận kết quả (chạy trên thread gọi hoặc thread fetch — caller
     *                 không giả định thread, chỉ giả định "sẽ được callback 1 lần").
     */
    default void requestLevel(String levelId, LevelCallback callback) {
        throw new UnsupportedOperationException(
                "MapSource không hỗ trợ requestLevel — source phải override");
    }

    /** runId cho lần chơi (do bootstrap cấp, sinh phía server để chống trùng kết quả). */
    default String newRunId(String levelId) { return "local-" + levelId; }

    /** mapVersion (hash dữ liệu map) để chốt kết quả. */
    default String mapVersion(String levelId) { return ""; }

    /**
     * Nhận kết quả tải level bất đồng bộ từ {@link #requestLevel}.
     * Implement trên {@link LocalCampaignSession}. Mỗi request chỉ kích đúng
     * một method; caller không tự re-entrancy.
     */
    interface LevelCallback {
        /**
         * @param levelId    level thực sự tải về (server có thể normalize, vd "campaign_1").
         * @param mapJson    nội dung JSON của map.
         * @param runId      runId do server cấp (chống trùng idempotency); null → client sinh.
         * @param mapVersion hash dữ liệu map để chốt kết quả.
         */
        void onLevelLoaded(String levelId, String mapJson, String runId, String mapVersion);

        /** Gọi khi fetch/parse lỗi. Session giữ nguyên sim hiện tại, sang LOAD_ERROR. */
        void onLoadError(String reason);
    }
}
