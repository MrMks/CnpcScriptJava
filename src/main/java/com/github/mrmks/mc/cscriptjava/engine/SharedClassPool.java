package com.github.mrmks.mc.cscriptjava.engine;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Objects;

public class SharedClassPool {

    private static int id = 0;
    private static File path = null;
    private static URL url;

    public static void init(File pa) {
        if (path == null && pa != null) {
            path = pa;
            try {
                //url = new URL("file:" + pa.getAbsolutePath() + "/");
                url = new URL(pa.toURI().toURL(), "./");
            } catch (MalformedURLException e) {
            }
            // null check;
            url.getClass();
        }
    }

    static int nextId() {
        return id++;
    }

    private static final HashMap<String, Class<?>> cache = new HashMap<>();
    private static final HashMap<String, Class<?>> saved = new HashMap<>();

    static void put(Class<?> klass) {
        String name = klass.getCanonicalName();
        if (saved.containsKey(name)) {
            String rn = name.replace('.', '/');
            File file = new File(path, rn.concat(".class"));
            try {
                byte[] bytes = IOUtils.toByteArray(Objects.requireNonNull(klass.getClassLoader().getResourceAsStream(name)));
                FileUtils.writeByteArrayToFile(file, bytes);
            } catch (IOException ignored) {
            }
            saved.put(name, klass);
        } else {
            cache.put(name, klass);
        }
    }

    public static boolean remove(String name) {
        boolean flag;
        flag = cache.remove(name) != null;
        flag = saved.remove(name) != null || flag;
        return flag;
    }

    public static boolean contain(String name) {
        return cache.containsKey(name) || saved.containsKey(name);
    }

    static Class<?> callFromLoader(String name) {
        Class<?> klass = cache.get(name);
        if (klass != null) {
            String rn = name.replace('.', '/');
            File file = new File(path, rn.concat(".class"));
            try {
                byte[] bytes = IOUtils.toByteArray(Objects.requireNonNull(klass.getClassLoader().getResourceAsStream(name)));
                FileUtils.writeByteArrayToFile(file, bytes);
            } catch (IOException ignored) {
            }
            saved.put(name, klass);
            cache.remove(name);
        } else {
            klass = saved.get(name);
            if (klass == null) {
                try {
                    klass = new URLClassLoader(new URL[]{url}, Thread.currentThread().getContextClassLoader()).loadClass(name);
                    saved.put(name, klass);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return klass;
    }

    static boolean isClassValid(Class<?> klass) {
        Class<?> klassNew = saved.get(klass.getCanonicalName());
        return klassNew == klass;
    }
}
