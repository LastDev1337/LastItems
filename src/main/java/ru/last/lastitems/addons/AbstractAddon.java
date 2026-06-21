package ru.last.lastitems.addons;

import dev.by1337.yaml.YamlMap;
import org.bukkit.plugin.java.JavaPlugin;
import ru.last.lastitems.LastItemsFree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

public abstract class AbstractAddon {
    private String name;
    private String version;
    private String author;
    private File dataFolder;
    private YamlMap config;
    private AddonClassLoader classLoader;

    public final void init(String name, String version, String author, File dataFolder, YamlMap config, AddonClassLoader classLoader) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.dataFolder = dataFolder;
        this.config = config;
        this.classLoader = classLoader;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public File getDataFolder() { return dataFolder; }
    public YamlMap getConfig() { return config; }

    public JavaPlugin getPlugin() { return LastItemsFree.getInstance(); }

    public Logger getLogger() { return getPlugin().getLogger(); }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }
        return classLoader.getResourceAsStream(filename);
    }

    public void saveResourceToFile(String resourcePath, File outFile) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }
        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }
        
        File outDir = outFile.getParentFile();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            getLogger().severe("Could not save " + outFile.getName() + " to " + outFile + ": " + e.getMessage());
        }
    }

    protected abstract void onEnable();
    protected abstract void onDisable();
}
