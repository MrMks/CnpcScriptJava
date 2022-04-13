package com.github.mrmks.mc.cscriptjava.engine;

import javax.script.*;
import javax.tools.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScriptJavaEngine implements ScriptEngine, Compilable, Invocable {

    private Class<?> klass = null;
    private ScriptContext context = new SimpleScriptContext();
    private final ScriptJavaEngineFactory factory;
    private final int id;

    public ScriptJavaEngine(ScriptJavaEngineFactory factory) {
        this.factory = factory;
        this.id = SharedClassPool.nextId();
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        return compile(new StringReader(script));
    }

    @Override
    public CompiledScript compile(Reader script) throws ScriptException {
        String[] re;

        try (BufferedReader br = new BufferedReader(script)) {
            re = construct(br, id);
        } catch (IOException e) {
            throw new ScriptException(e);
        }

        Class<?> clazz = compile0(re[0], re[1], re[2]);
        if (clazz == null) {
            throw new ScriptException("Unable to compile java file");
        }
        this.klass = clazz;
        if (!re[1].equals(re[0].substring(0, 1).concat(Integer.toString(id)))) {
            SharedClassPool.put(clazz);
        }
        try {
            insertIO(klass);
            Object instance = clazz.newInstance();
            return new ScriptJavaCompiled(this, clazz, instance);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ScriptException(e);
        }
    }

    protected Class<?> compile0(String full, String clazz, String body) throws ScriptException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler == null) {
            throw new ScriptException("Compiler not found, may running in jre. Please use jdk instead of jre.");
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager stdManager = compiler.getStandardFileManager(diagnostics, null, null);
        ScriptJavaFileManager manager = new ScriptJavaFileManager(stdManager, loader);
        JavaFileObject object = manager.makeStringSource(clazz.concat(".java"), body);
        JavaCompiler.CompilationTask task = compiler.getTask(getContext().getErrorWriter(), manager, diagnostics, null, null, Collections.singletonList(object));

        Boolean flag = task.call();
        if (flag == null || !flag) {
            throw new ScriptException(diagnostics.getDiagnostics().stream().map(Object::toString).collect(Collectors.joining("\n")));
        }

        ScriptJavaClassLoader classLoader = new ScriptJavaClassLoader(loader, manager.getClassBytes());

        try {
            manager.close();
        } catch (IOException ignored) {}

        try {
            return classLoader.loadClass(full);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static String[] constructTest(Reader r) throws ScriptException {
        try (BufferedReader br = r instanceof BufferedReader ? (BufferedReader) r : new BufferedReader(r)) {
            return construct(br, 0);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    private static final char headStop = '!', headRun = '+', headSupplier = '=', headAno = '?';
    private static final String preHead = "#", preEnd = preHead + headStop;
    protected static String[] construct(BufferedReader br, int id) throws ScriptException {
        try {
            String line = br.readLine();
            if (line == null) return new String[]{"R", "R", "public final class R implements Runnable {public void run(){}}"};
            boolean isClass = false;

            StringBuilder sb = new StringBuilder();

            String full, clazz;
            if (line.startsWith(preHead)) {
                String pack, ext, impl, token = "Object";
                boolean hasImp = line.length() <= preHead.length() || line.charAt(preEnd.length() - 1) != headStop;
                line = line.substring(hasImp ? preHead.length() : preEnd.length()).trim();
                boolean isRun = line.isEmpty() || line.charAt(0) == headRun;
                boolean isSupplier = !line.isEmpty() && line.charAt(0) == headSupplier;
                isClass = !isRun && !isSupplier;
                if (isClass) {
                    int p = line.indexOf(':'), p2 = line.lastIndexOf(':');
                    full = p < 0 ? line : line.substring(0, p);
                    ext = p < 0 ? null : (p == p2 ? line.substring(p + 1) : line.substring(p + 1, p2)).trim();
                    impl = p2 < 0 || p == p2 ? null : line.substring(p2 + 1).trim();
                    clazz = (p = full.lastIndexOf('.')) < 0 ? full : full.substring(p + 1);
                    pack = p < 0 ? null : full.substring(0, p).trim();
                    if (clazz.charAt(0) == headAno) clazz = full = "A" + id;
                } else {
                    if (isSupplier) {
                        int p = line.indexOf(':');
                        token = p < 0 ? "Object" : line.substring(p + 1);
                        impl = "java.util.function.Supplier<".concat(token).concat(">");
                        ext = null;
                        if (p >= 0) line = line.substring(0, p);
                        if (line.isEmpty() || (full = line.substring(1)).isEmpty()) {
                            full = clazz = "S" + id;
                            pack = null;
                        } else {
                            p = full.lastIndexOf('.');
                            clazz = p < 0 ? full : full.substring(p + 1);
                            pack = p < 0 ? null : full.substring(0, p);
                        }
                    } else {
                        impl = "java.lang.Runnable";
                        ext = null;
                        if (line.isEmpty() || (full = line.substring(1)).isEmpty()) {
                            full = clazz = "R" + id;
                            pack = null;
                        } else {
                            int p = full.lastIndexOf('.');
                            clazz = p < 0 ? full : full.substring(p + 1);
                            pack = p < 0 ? null : full.substring(0, p);
                        }
                    }
                }
                if (pack != null && !pack.isEmpty()) sb.append("package ").append(pack).append(';');
                if (hasImp) {
                    sb.append('\n');
                    while ((line = br.readLine()) != null && !(line = line.trim()).startsWith(preEnd)) {
                        if (line.startsWith("//")) {
                            sb.append("//");
                            line = line.substring(2);
                        }
                        if (!line.startsWith("import")) sb.append("import");
                        sb.append(" ").append(line);
                        if (line.charAt(line.length() - 1) != ';') sb.append(';');
                        sb.append('\n');
                    }
                    if (line == null) throw new ScriptException("Illegal header format: find eof before \"" + preEnd + "\"");
                }
                sb.append("public final class ").append(clazz);
                if (ext != null && !ext.isEmpty()) sb.append(" extends ").append(ext);
                if (impl != null && !impl.isEmpty()) sb.append(" implements ").append(impl);
                sb.append(" {");
                if (isRun) sb.append("public void run() {");
                else if (isSupplier) sb.append("public ").append(token).append(" get() {");
                sb.append('\n');
            } else {
                full = clazz = "R" + id;
                sb.append("public final class R").append(id).append(" implements java.lang.Runnable{public void run(){").append(line).append('\n');
            }
            while ((line = br.readLine()) != null && (isClass || !line.trim().equals(preEnd)))
                sb.append(line).append('\n');

            if (!isClass) {
                sb.append("}\n");
                if (line != null) {
                    while ((line = br.readLine()) != null) sb.append(line).append('\n');
                }
            }

            attachPrint(sb);
            sb.append("}\n");

            return new String[]{full, clazz, sb.toString()};
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    protected static void attachPrint(StringBuilder sb) {
        sb.append("static java.io.Reader sji = null;\nstatic java.io.Writer sjo = null, sje = null;\n");
        sb.append("static void print(Object obj) {try {sjo.write(obj.toString());sjo.write(\"\\n\");sjo.flush();} catch (java.io.IOException e) {}}\n");
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        setContext(context);
        return compile(script).eval();
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        setContext(context);
        return compile(reader).eval();
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return compile(script).eval();
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return compile(reader).eval();
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
        setBindings(n, ScriptContext.ENGINE_SCOPE);
        return compile(script).eval();
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException {
        setBindings(n, ScriptContext.ENGINE_SCOPE);
        return compile(reader).eval();
    }

    @Override
    public void put(String key, Object value) {
        getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key) {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return context.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        context.setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }

    @Override
    public void setContext(ScriptContext context) {
        Objects.requireNonNull(context);
        this.context = context;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    private void insertIO(Class<?> clazz) {
        try {
            Field sji, sjo, sje;
            sji = clazz.getDeclaredField("sji");
            sjo = clazz.getDeclaredField("sjo");
            sje = clazz.getDeclaredField("sje");
            sji.setAccessible(true);
            sjo.setAccessible(true);
            sje.setAccessible(true);

            sji.set(null, context.getReader());
            sjo.set(null, context.getWriter());
            sje.set(null, context.getErrorWriter());
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (klass == null) throw new ScriptException("invokeMethod called before eval");
        if (klass.isInstance(thiz)) {
            try {
                Class<?>[] klasses = new Class[args.length];
                for (int i = 0; i < args.length; ++i) {
                    klasses[i] = args[i] == null ? null : args[i].getClass();
                }
                insertIO(klass);
                return klass.getDeclaredMethod(name, klasses).invoke(thiz, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException("Not an invalid instance");
        }
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (klass == null) throw new ScriptException("invokeFunction called before eval");
        try {
            Object ins = klass.newInstance();
            return invokeMethod(ins, name, args);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public <T> T getInterface(Class<T> clasz) {
        return null;
    }

    @Override
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        return null;
    }
}
