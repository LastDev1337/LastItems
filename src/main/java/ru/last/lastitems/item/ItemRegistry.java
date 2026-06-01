package ru.last.lastitems.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.last.lastitems.LastItemsFree;

import java.util.*;

public class ItemRegistry {
    private final Map<String, CustomItem> registry = new HashMap<>(64);
    private final Map<String, List<String>> folderRegistry = new HashMap<>();
    private final NamespacedKey idKey;

    public ItemRegistry(@NotNull LastItemsFree plugin) {
        this.idKey = new NamespacedKey(plugin, "lastitems_free");
    }

    public void register(CustomItem item, String folder) {
        String id = item.getId().toLowerCase(Locale.ROOT);
        registry.put(id, item);
        folderRegistry.computeIfAbsent(folder, k -> new ArrayList<>()).add(id);
    }

    public void clear() {
        registry.clear();
        folderRegistry.clear();
    }

    @Nullable
    public CustomItem getCustomItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(idKey, PersistentDataType.STRING)) return null;
        String id = pdc.get(idKey, PersistentDataType.STRING);
        return id == null ? null : registry.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> getAllIds() {
        return registry.keySet();
    }

    public CustomItem getById(String id) {
        if (id == null) return null;
        return registry.get(id.toLowerCase(Locale.ROOT));
    }

    public int size() {
        return registry.size();
    }

    public Set<String> getFolders() {
        return folderRegistry.keySet();
    }

    public List<String> getItemsInFolder(String folderName) {
        return folderRegistry.getOrDefault(folderName, Collections.emptyList());
    }
}