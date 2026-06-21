package ru.last.lastitems.addons;

import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import dev.by1337.yaml.YamlMap;

public class AddonDescription {
    private final String name;
    private final String version;
    private final String author;
    private final String mainClass;

    public AddonDescription(File jarFile) throws Exception {
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry entry = zip.getEntry("addon.yml");
            if (entry == null) {
                throw new Exception("Jar does not contain addon.yml");
            }
            try (InputStream is = zip.getInputStream(entry)) {
                YamlMap map = YamlMap.load(is);
                this.name = map.get("name").asString("Unknown");
                this.version = map.get("version").asString("1.0");
                this.author = map.get("author").asString("Unknown");
                this.mainClass = map.get("main").asString("");
                if (this.mainClass.isEmpty()) {
                    throw new Exception("addon.yml does not contain 'main' class path");
                }
            }
        }
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getMainClass() { return mainClass; }
}
