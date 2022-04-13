package com.github.mrmks.mc.cscriptjava.engine;

import javax.script.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

public class JavaLoaderEngine extends AbstractScriptEngine implements Invocable {

    final JavaLoaderEngineFactory factory;
    Object[] ins = null;

    JavaLoaderEngine(JavaLoaderEngineFactory factory) {
        this.factory = factory;
    }

    private static String[] parseParams(String str) {
        LinkedList<String> list = new LinkedList<>();
        boolean lastTr = false;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (lastTr) builder.append(c);
            else if (c == ',') {
                list.add(builder.toString());
                builder = new StringBuilder();
            } else if (!(lastTr = c == '\\')) {
                builder.append(c);
            }
        }
        list.add(builder.toString());
        return list.toArray(new String[0]);
    }

    private static Object[] compile0(Reader reader, PrintWriter pw) throws ScriptException {
        BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        String[] lines = br.lines().toArray(String[]::new);
        LinkedList<Object> list = new LinkedList<>();
        LinkedList<String> usageList = new LinkedList<>();
        for (String line : lines) {
            if (!line.isEmpty()) line = line.trim();
            if (!line.isEmpty() && !line.startsWith("//")) {
                int p = line.indexOf(':');
                String klass = p < 0 ? line : line.substring(0, p);
                String params = p < 0 ? null : line.substring(p + 1);
                Class<?> clazz = SharedClassPool.callFromLoader(klass);
                if (clazz != null) {
                    Object ins = null;
                    try {
                        ins = clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException ignored) {}
                    if (ins != null) {
                        list.add(ins);
                        if (params != null) {
                            String[] paramAry = parseParams(params);
                            try {
                                Method me = clazz.getMethod("setParams", String[].class);
                                try {
                                    Method usg = clazz.getMethod("usageParams");
                                    Object re = usg.invoke(ins);
                                    if (!(re instanceof String)) throw new ScriptException("Your usageParams() method must return String in non-null");
                                    usageList.add(klass + ": " + re);
                                } catch (NoSuchMethodException e) {
                                    throw new ScriptException("You must provide usageParams() method with setParams(String[]) and return String");
                                } catch (InvocationTargetException | IllegalAccessException ignored) {}
                                me.invoke(ins, (Object) paramAry);
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                            }
                        }
                    }
                } else {
                    throw new ScriptException("class not found: " + klass);
                }
            }
        }
        if (!list.isEmpty()) {
            pw.println("List of param usages:");
            for (String u : usageList) pw.println(u);
            pw.println();
            return list.toArray();
        }
        return null;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        StringReader sr = new StringReader(script);
        Object obj = eval(sr, context);
        sr.close();
        return obj;
    }

    private static void insertIO(Object ins, ScriptContext context) {
        Class<?> clazz = ins.getClass();
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
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        if (this.context != context) this.context = context;
        PrintWriter pw = new PrintWriter(context.getWriter());
        Object[] objs = compile0(reader, pw);
        try {
            reader.close();
        } catch (IOException ignored) {}
        if (objs != null && objs.length > 0) {
            ins = objs;
            for (Object obj : objs) {
                insertIO(obj, context);
            }
        }
        return null;
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        return null;
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        if (ins != null) {
            boolean outdated = false;
            for (int i = 0; i < ins.length; ++i) {
                boolean f = !SharedClassPool.isClassValid(ins[i].getClass());
                if (f) ins[i] = null;
                outdated = outdated || f;
            }
            if (outdated) throw new ScriptException("Outdated class used, please re-compile the script");
            Class<?>[] types = new Class[args.length];
            for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
            int count = 0;
            for (Object obj : ins) {
                try {
                    Method method = obj.getClass().getMethod(name, types);
                    insertIO(obj, context);
                    method.invoke(obj, args);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    count++;
                }
            }
            if (count >= ins.length) throw new NoSuchMethodException();
        }
        return null;
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
