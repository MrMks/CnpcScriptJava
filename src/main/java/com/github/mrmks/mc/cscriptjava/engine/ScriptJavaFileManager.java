package com.github.mrmks.mc.cscriptjava.engine;

import javax.tools.*;
import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ScriptJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    final Map<String, byte[]> classBytes = new HashMap<>();
    final ClassLoader loader;
    ScriptJavaFileManager(JavaFileManager fileManager, ClassLoader loader) {
        super(fileManager);
        this.loader = loader;
    }

    public Map<String, byte[]> getClassBytes() {
        return new HashMap<>(classBytes);
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof ClassLoaderJavaFileObject) {
            return ((ClassLoaderJavaFileObject)file).binaryName;
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
        Iterable<JavaFileObject> suObj = super.list(location, packageName, kinds, recurse);
        if (!suObj.iterator().hasNext() && location == StandardLocation.PLATFORM_CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)) {

            packageName = packageName.replace('.', '/');
            Enumeration<URL> urlE = loader.getResources(packageName);
            LinkedList<JavaFileObject> obj = new LinkedList<>();
            while (urlE.hasMoreElements()) {
                URL url = urlE.nextElement();
                URLConnection urlConnection = url.openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
                    String path = url.getPath();
                    if (path.startsWith("file:")) path = "jar://".concat(path.substring(5));
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry je = entries.nextElement();
                        String name = je.getName();
                        if (!je.isDirectory() && name.startsWith(packageName)) {
                            if (recurse || name.indexOf('/', packageName.length() + 1) < 0) {
                                obj.add(new ClassLoaderJavaFileObject(URI.create(path.concat(name.substring(packageName.length() + 1))), loader, name));
                            }
                        }
                    }
                }
            }
            if (!obj.isEmpty()) return obj;
        }

        return suObj;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        classBytes.clear();
        super.close();
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        if (kind == JavaFileObject.Kind.CLASS) {
            return new MemoryOutputJavaFileObject(className, super.getJavaFileForOutput(location, className, kind, sibling));
        } else {
            return super.getJavaFileForOutput(location, className, kind, sibling);
        }
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) throws IOException {
        return super.getJavaFileForInput(location, className, kind);
    }

    JavaFileObject makeStringSource(String name, String code) {
        return new MemoryInputJavaFileObject(name, code);
    }

    static class MemoryInputJavaFileObject extends SimpleJavaFileObject {

        final String code;

        MemoryInputJavaFileObject(String name, String code) {
            super(URI.create("string:///" + name), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
            return CharBuffer.wrap(code);
        }
    }

    static class ClassLoaderJavaFileObject extends SimpleJavaFileObject {

        final String binaryName, resourceName;
        final ClassLoader loader;

        /**
         * Construct a SimpleJavaFileObject of the given kind and with the
         * given URI.
         *
         * @param uri  the URI for this file object
         */
        protected ClassLoaderJavaFileObject(URI uri, ClassLoader loader, String resource) {
            super(uri, Kind.CLASS);
            this.loader = loader;
            this.resourceName = resource;
            this.binaryName = resource.replace('/', '.').substring(0, resource.length() - 6);
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return loader == null ? ClassLoader.getSystemResourceAsStream(resourceName) : loader.getResourceAsStream(resourceName);
        }
    }

    class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
        final String name;
        final JavaFileObject std;

        MemoryOutputJavaFileObject(String name, JavaFileObject std) {
            super(URI.create("string:///" + name), Kind.CLASS);
            this.name = name;
            this.std = std;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return new FilterOutputStream(new ByteArrayOutputStream()) {
                @Override
                public void close() throws IOException {
                    out.close();
                    ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                    classBytes.put(name, bos.toByteArray());
                }
            };
        }

    }

    static class CombinedOutputStream extends OutputStream{

        private OutputStream outA, outB;

        CombinedOutputStream(OutputStream outA, OutputStream outB) {
            this.outA = outA;
            this.outB = outB;
        }

        @Override
        public void write(int b) throws IOException {
            outA.write(b);
            outB.write(b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            try (OutputStream tA = outA) {
                tA.flush();
            }
            try (OutputStream tB = outB) {
                tB.flush();
            }
        }
    }
}
