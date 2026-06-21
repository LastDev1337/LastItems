package ru.last.lastitems.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class AttributeUtil {

    public static Attribute getAttribute(String name) {
        if (name == null || name.isEmpty()) return null;
        String upperName = name.toUpperCase();

        try {
            // 1.16.5 - 1.21.2
            Method valueOfMethod = Attribute.class.getMethod("valueOf", String.class);
            try {
                return (Attribute) valueOfMethod.invoke(null, upperName);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof IllegalArgumentException) {
                    if (!upperName.startsWith("GENERIC_")) {
                        try {
                            return (Attribute) valueOfMethod.invoke(null, "GENERIC_" + upperName);
                        } catch (Exception ignored) {}
                    }
                    if (upperName.startsWith("GENERIC_")) {
                        try {
                            return (Attribute) valueOfMethod.invoke(null, upperName.replace("GENERIC_", ""));
                        } catch (Exception ignored) {}
                    }
                }
                return null;
            }
        } catch (NoSuchMethodException e) {
            // 1.21.3+
            try {
                Class<?> registryClass = Class.forName("org.bukkit.Registry");
                Object registry = registryClass.getField("ATTRIBUTE").get(null);
                Method getMethod = registryClass.getMethod("get", NamespacedKey.class);

                String lowerName = name.toLowerCase();
                
                Attribute attr = (Attribute) getMethod.invoke(registry, NamespacedKey.minecraft(lowerName));
                
                if (attr == null && !lowerName.startsWith("generic_")) {
                    attr = (Attribute) getMethod.invoke(registry, NamespacedKey.minecraft("generic_" + lowerName));
                }
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