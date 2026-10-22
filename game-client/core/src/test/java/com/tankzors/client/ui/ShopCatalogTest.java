package com.tankzors.client.ui;

import com.tankzors.client.shop.ShopItem;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ShopCatalogTest {
    @Test void catalogueMatchesBuildsSectionsAndIncludesEveryItemExactlyOnce() {
        List<ShopCatalog.Row> rows=ShopCatalog.rows();
        assertEquals(List.of("Basic ammunition","Add. ammunition","Ally tanks","Defend","Repair","Other","Research"),
                rows.stream().filter(r->r.kind==ShopCatalog.Kind.HEADER).map(r->r.title).collect(Collectors.toList()));
        List<Integer> ids=rows.stream().filter(r->r.item!=null).map(r->r.item.getId()).collect(Collectors.toList());
        assertEquals(List.of(0,1,2,3,4,5,6,7,8,10,11,12,13,14,9,25,26,27,21,22,20,19,23,-1,-2,17,15),ids);
        Set<ShopItem> items=new HashSet<>();
        rows.stream().filter(r->r.item!=null).forEach(r->assertTrue(items.add(r.item)));
        assertEquals(ShopItem.values().length,items.size());
        assertEquals(10,rows.stream().filter(r->r.tech!=null).count());
        assertEquals("extraInterest",rows.stream().filter(r->r.tech!=null).findFirst().orElseThrow().tech.getId());
        assertFalse(rows.get(0).selectable());
        assertEquals(ShopCatalog.Kind.CONTINUE,rows.get(rows.size()-2).kind);
        assertTrue(rows.stream().anyMatch(r->r.kind==ShopCatalog.Kind.REPAIR_ALLIES));
    }
}
