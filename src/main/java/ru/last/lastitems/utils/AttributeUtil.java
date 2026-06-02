package ru.last.lastitems.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import java.lang.reflect.Method;

public class AttributeUtil {

    public static Attribute getAttribute(String name) {
        if (name == null || name.isEmpty()) return null;

        try {
            // 1.16.5 - 1.21.1
            Method valueOfMethod = Attribute.class.getMethod("valueOf", String.class);
            return (Attribute) valueOfMethod.invoke(null, name.toUpperCase());
            
        } catch (NoSuchMethodException e) {
            // 1.21.3+
            try {
                Class<?> registryClass = Class.forName("org.bukkit.Registry");
                Object registry = registryClass.getField("ATTRIBUTE").get(null);
                Method getMethod = registryClass.getMethod("get", NamespacedKey.class);

                String lowerName = name.toLowerCase();
                
                Attribute attr = (Attribute) getMethod.invoke(registry, NamespacedKey.minecraft(lowerName));
                
                if (attr == null && lowerName.startsWith("generic_")) {
                    String modernName = lowerName.replace("generic_", "");
                    attr = (Attribute) getMethod.invoke(registry, NamespacedKey.minecraft(modernName));
                }
                
                return attr;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}