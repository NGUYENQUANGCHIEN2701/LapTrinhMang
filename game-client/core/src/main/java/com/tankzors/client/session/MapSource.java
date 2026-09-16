package com.tankzors.client.session;

/**
 * Nguồn dữ liệu map JSON cho {@link LocalCampaignSession}.
 * Do core không được dùng ClassLoader/InputStream (GWT không dịch), nguồn map
 * do launcher (html/desktop) cung cấp.
 *
 * <p>HtmlLauncher: lấy qua REST {@code GET /api/game/bootstrap?levelId=...} (Bước D)
 * hoặc qua biến nhúng trong host page.</p>
 * <p>DesktopLauncher: đọc file từ assets/ thư mục.</p>
 */
public interface MapSource {
    /**
     * Lấy nội dung JSON của một level.
     * @param levelId mã level, vd {@code "campaign_1"}.
     * @return chuỗi JSON, hoặc null nếu không tìm thấy.
     */
    String loadMapJson(String levelId);

    /** runId cho lần chơi (do bootstrap cấp, sinh phía server để chống trùng kết quả). */
    default String newRunId(String levelId) { return "local-" + levelId; }

    /** mapVersion (hash dữ liệu map) để chốt kết quả. */
    default String mapVersion(String levelId) { return ""; }
}
