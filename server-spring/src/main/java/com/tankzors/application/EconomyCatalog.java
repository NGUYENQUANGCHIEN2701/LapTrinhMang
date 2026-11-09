package com.tankzors.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/** Pricing metadata; never imports or runs the client simulation. */
@Component
public class EconomyCatalog {
    public record Item(int id, int price, int pack, int limit, String tech, int level) {}
    private final Map<Integer, Item> items = new HashMap<>();
    private final Map<String, int[]> research = new HashMap<>();
    public EconomyCatalog(ObjectMapper mapper) {
        try (var input = getClass().getResourceAsStream("/economy/catalog.json")) {
            JsonNode root = mapper.readTree(input);
            for (JsonNode n : root.get("items")) {
                Item item = new Item(n.get("id").asInt(), n.get("price").asInt(), n.get("pack").asInt(),
                        n.get("limit").asInt(), n.get("tech").isNull() ? null : n.get("tech").asText(), n.get("level").asInt());
                items.put(item.id(), item);
            }
            for (JsonNode n : root.get("research")) {
                int[] prices = new int[n.get("prices").size()];
                for (int i = 0; i < prices.length; i++) prices[i] = n.get("prices").get(i).asInt();
                research.put(n.get("id").asText(), prices);
            }
        } catch (Exception e) { throw new IllegalStateException("Invalid economy catalog", e); }
    }
    public Item item(int id) {
        Item item = items.get(id);
        if (item == null) throw new IllegalArgumentException("Vật phẩm không hợp lệ");
        return item;
    }
    public int[] research(String id) {
        int[] prices = research.get(id);
        if (prices == null) throw new IllegalArgumentException("Nghiên cứu không hợp lệ");
        return prices;
    }
    public int limit(Item item, Map<String, Integer> tech, Map<String, Integer> inventory) {
        if (inventory.getOrDefault("-1", 0) > 0 && item.id() >= 2 && item.id() <= 5) return 99;
        if (item.id() == 20) return tech.getOrDefault("powerFieldResearch", 0) >= 4 ? 7 : 5;
        if (item.id() == 21 || item.id() == 22) return tech.getOrDefault("armorResearch", 0) >= 3 ? 5 : 4;
        return item.limit();
    }
    public int price(Item item, Map<String, Integer> tech, Map<String, Integer> inventory) {
        boolean armor = tech.getOrDefault("armorResearch", 0) >= 3;
        if (item.id() == 19) return (5 + inventory.getOrDefault("21", 0) + inventory.getOrDefault("22", 0)
                + (inventory.getOrDefault("-1", 0) > 0 ? 2 : 0)) * (armor ? 5 : 8);
        if (armor && (item.id() == 20 || item.id() == 21 || item.id() == 22)) return item.price() - item.price() * 30 / 100;
        return item.price();
    }
    public void requireUnlocked(Item item, Map<String, Integer> tech, Map<String, Integer> inventory) {
        if (item.tech() != null && tech.getOrDefault(item.tech(), 0) < item.level()) throw new IllegalArgumentException("Vật phẩm chưa mở");
        if (item.id() == 9 && inventory.getOrDefault("23", 0) == 0) throw new IllegalArgumentException("Cần mua Radar");
    }
}
