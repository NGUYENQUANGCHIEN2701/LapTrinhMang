package com.tankzors.client.ui;

import com.tankzors.client.shop.ResearchTech;
import com.tankzors.client.shop.ShopItem;
import java.util.ArrayList;
import java.util.List;

/** The single scrolling catalogue, in Builds game.gui.Shop element order. */
public final class ShopCatalog {
    public enum Kind { SPACE, HEADER, ITEM, RESEARCH, REPAIR_TANK, REPAIR_ALLIES, CONTINUE }
    public static final class Row {
        public final Kind kind;
        public final String title;
        public final int icon;
        public final ShopItem item;
        public final ResearchTech tech;
        private Row(Kind kind, String title, int icon, ShopItem item, ResearchTech tech) {
            this.kind = kind; this.title = title; this.icon = icon; this.item = item; this.tech = tech;
        }
        public boolean selectable() { return kind != Kind.SPACE && kind != Kind.HEADER; }
    }
    private ShopCatalog() {}
    public static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        space(rows); space(rows);
        header(rows, "Đạn cơ bản", 0); items(rows, 0,1,2,3,4,5);
        header(rows, "Vũ khí đặc biệt", 7); items(rows, 6,7,8,10,11,12,13,14,9);
        header(rows, "Xe đồng minh", 25); items(rows, 25,26,27);
        header(rows, "Phòng thủ", 22); items(rows, 21,22,20);
        header(rows, "Sửa chữa", 19); items(rows, 19);
        rows.add(new Row(Kind.REPAIR_TANK, "Sửa xe của bạn", 19, null, null));
        rows.add(new Row(Kind.REPAIR_ALLIES, "Sửa xe đồng minh", 19, null, null));
        header(rows, "Hỗ trợ", 17); items(rows, 23,-1,-2,17,15);
        header(rows, "Nghiên cứu", 20);
        String[] ids = {"extraInterest","shellResearch","artilleryResearch","missileResearch","mineResearch",
                "dynamiteResearch","armorResearch","powerFieldResearch","airRaidResearch","allyTankResearch"};
        for (String id : ids) {
            ResearchTech tech = ResearchTech.findById(id);
            rows.add(new Row(Kind.RESEARCH, tech.getDisplayName(), 20, null, tech));
        }
        space(rows); rows.add(new Row(Kind.CONTINUE, "Tiếp tục", -1, null, null)); space(rows);
        return rows;
    }
    private static void space(List<Row> rows) { rows.add(new Row(Kind.SPACE, "", -1, null, null)); }
    private static void header(List<Row> rows, String title, int icon) { rows.add(new Row(Kind.HEADER, title, icon, null, null)); }
    private static void items(List<Row> rows, int... ids) {
        for (int id : ids) {
            ShopItem item = ShopItem.findById(id);
            String title = item.getDisplayName();
            switch (item) {
                case ARMOR: title = "Giáp tạm thời"; break;
                case ARMOR2: title = "Giáp vĩnh cửu"; break;
                case FORCE_FIELD: title = "Khiên năng lượng"; break;
                case REPAIR: title = "Bộ sửa xe"; break;
                case EX_CHASSIS: title = "Khung gầm mở rộng"; break;
                case ADD_LIFE: title = "Mạng bổ sung"; break;
                case SPEED_UP: title = "Tăng tốc Nitro"; break;
                case FREEZE: title = "Đóng băng"; break;
                default: break;
            }
            rows.add(new Row(Kind.ITEM, title, id, item, null));
        }
    }
}
