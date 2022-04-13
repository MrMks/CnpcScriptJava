package com.github.mrmks.mc.cscriptjava.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class ScriptJavaClassLoader extends URLClassLoader {

    Map<String, byte[]> classBytes = new HashMap<>();

    public ScriptJavaClassLoader(ClassLoader parent, Map<String, byte[]> classBytes) {
        super(new URL[0], parent);
        this.classBytes.putAll(classBytes);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] buf = classBytes.get(name);
        if (buf == null) {
            return super.findClass(name);
        }
        return defineClass(name, buf, 0, buf.length);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] bytes = classBytes.get(name.replace('/', '.'));
        if (bytes != null) return new ByteArrayInputStream(bytes);
        return super.getResourceAsStream(name);
    }
}
