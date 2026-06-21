package ru.last.lastitems.addons;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class AddonClassLoader extends URLClassLoader {
    public AddonClassLoader(File jarFile, ClassLoader parent) throws MalformedURLException {
        super(new URL[]{jarFile.toURI().toURL()}, parent);
    }
}
