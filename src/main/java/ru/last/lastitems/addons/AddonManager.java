package ru.last.lastitems.addons;

import dev.by1337.yaml.YamlMap;
import ru.last.lastitems.LastItemsFree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AddonManager {
    private final LastItemsFree plugin;
    private final List<AbstractAddon> loadedAddons = new ArrayList<>();
    private final List<AddonClassLoader> classLoaders = new ArrayList<>();

    public AddonManager(LastItemsFree plugin) { this.plugin = plugin; }
    
    public List<AbstractAddon> getLoadedAddons() { return loadedAddons; }

    public void loadAddons() {
        File addonsFolder = new File(plugin.getDataFolder(), "addons");
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }

        File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            try {
                AddonDescription desc = new AddonDescription(file);
                plugin.getDebugLogger().info("Loading addon " + desc.getName() + " v" + desc.getVersion() + " by " + desc.getAuthor());

                AddonClassLoader loader = new AddonClassLoader(file, plugin.getClass().getClassLoader());
                classLoaders.add(loader);

                Class<?> mainClass = Class.forName(desc.getMainClass(), true, loader);
                if (!AbstractAddon.class.isAssignableFrom(mainClass)) {
                    plugin.getLogger().warning("Class " + desc.getMainClass() + " does not extend AbstractAddon!");
                    continue;
                }

                AbstractAddon addon = (AbstractAddon) mainClass.getDeclaredConstructor().newInstance();

                File authorFolder = new File(addonsFolder, desc.getAuthor());
                if (!authorFolder.exists()) authorFolder.mkdirs();

                File configFile = new File(authorFolder, "config.yml");
                YamlMap configMap;
                if (configFile.exists()) {
                    configMap = YamlMap.load(configFile);
                } else {
                    configMap = new YamlMap();
                }

                addon.init(desc.getName(), desc.getVersion(), desc.getAuthor(), authorFolder, configMap, loader);

                loadedAddons.add(addon);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при загрузке аддона из файла " + file.getName(), e);
            }
        }
    }

    public void enableAddons() {
        for (AbstractAddon addon : loadedAddons) {
            try {
                addon.onEnable();
                plugin.getDebugLogger().info("Addon " + addon.getName() + " successfully enabled!");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при включении аддона " + addon.getName(), e);
            }
        }
    }

    public void disableAddons() {
        for (AbstractAddon addon : loadedAddons) {
            try {
                addon.onDisable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при отключении аддона " + addon.getName(), e);
            }
        }
        loadedAddons.clear();

        for (AddonClassLoader loader : classLoaders) {
            try {
                loader.close();
            } catch (Exception ignored) {}
        }
        classLoaders.clear();
    }
}
