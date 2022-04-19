package com.github.mrmks.mc.cscriptjava.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.HashMap;

public class SharedClassPool {

    private static int id = 0;
    private static File path = null;
    private static URL url;

    static {
        try {
            url = new URL("./");
        } catch (MalformedURLException e) {
            // this should never happen
            e.printStackTrace();
        }
    }

    public static void init(File pa) {
        if (path == null && pa != null) {
            path = pa;
            try {
                //url = new URL("file:" + pa.getAbsolutePath() + "/");
                url = new URL(pa.toURI().toURL(), "./");
            } catch (MalformedURLException e) {
                // this should never happen, so we throw it as a runtimeException;
                throw new RuntimeException(e);
            }
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
            (writeTo(path, klass, name) ? saved : cache).put(name, klass);
        } else {
            cache.put(name, klass);
        }
    }

    public static boolean remove(String name) {
        boolean flag;
        flag = cache.remove(name) != null;
        flag = removeSaved(name) || flag;
        return flag;
    }

    public static boolean contain(String name) {
        return cache.containsKey(name) || saved.containsKey(name);
    }

    private static boolean removeSaved(String name) {
        boolean f = saved.remove(name) != null;
        if (f) {
            try {
                f = Files.deleteIfExists(new File(path, name.replace('.', '/')).toPath());
            } catch (IOException e) {
                f = false;
                e.printStackTrace();
            }
        }
        return f;
    }

    static Class<?> callFromLoader(String name) {
        Class<?> klass = cache.get(name);
        if (klass != null && writeTo(path, klass, name)) {
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

    private static int errCount = 0;
    private static boolean writeTo(File file, Class<?> klass, String name) {
        ClassLoader loader = klass.getClassLoader();
        if (loader instanceof ScriptJavaClassLoader) {
            byte[] bytes = ((ScriptJavaClassLoader) loader).classBytes.get(name);
            if (bytes != null) {
                try {
                    file = new File(file, name.replace('.', '/').concat(".class"));
                    if (!file.exists()) {
                        File parent = file.getParentFile();
                        if (parent != null) {
                            if (!parent.isDirectory() && !parent.mkdirs()) {
                                throw new IOException("Directory '" + parent + "' could not be created");
                            }
                        }
                    } else if (!file.canWrite()) throw new IOException("File '" + file + "' cannot be written to");

                    OutputStream out = new FileOutputStream(file);
                    out.write(bytes);
                    out.flush();
                    out.close();
                    return true;
                } catch (IOException e) {
                    errCount++;
                    if (errCount < 32 || (errCount & ((errCount >>> 4) - 1)) == 0) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    static boolean isClassValid(Class<?> klass) {
        Class<?> klassNew = saved.get(klass.getCanonicalName());
        return klassNew == klass;
    }
}
