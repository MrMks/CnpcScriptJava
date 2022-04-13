package com.github.mrmks.mc.cscriptjava.engine;

import javax.script.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class JavaLoaderEngine extends AbstractScriptEngine implements Invocable {

    final JavaLoaderEngineFactory factory;
    Object[] ins = null;

    JavaLoaderEngine(JavaLoaderEngineFactory factory) {
        this.factory = factory;
    }

    private static Object[] compile0(Reader reader, PrintWriter pw) throws ScriptException {
        BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        String[] lines = br.lines().toArray(String[]::new);
        LinkedList<Object> list = new LinkedList<>();
        LinkedList<String> usageList = new LinkedList<>();
        LinkedList<String> crashMsg = new LinkedList<>();

        Iterator<String> it = Arrays.stream(lines).iterator();
        String tmp;
        while (it.hasNext()) {
            tmp = it.next();
            if (!tmp.isEmpty() && !(tmp = tmp.trim()).isEmpty() && !tmp.startsWith("//")) {
                String klass;
                String[] params;
                int p = tmp.indexOf('(');
                if (p < 0) {
                    klass = tmp;
                    params = null;
                } else {
                    klass = tmp.substring(0, p);
                    tmp = tmp.substring(p + 1).trim();
                    LinkedList<String> pList = new LinkedList<>();
                    StringBuilder sb = new StringBuilder();
                    label:
                    while (true) {
                        if (!tmp.isEmpty()) {
                            boolean tr = false;
                            for (int i = 0; i < tmp.length(); ++i) {
                                char c = tmp.charAt(i);
                                if (tr) {
                                    sb.append(c == 'n' ? '\n' : c);
                                    tr = false;
                                } else if (c == ',') {
                                    pList.add(sb.toString());
                                    sb = new StringBuilder();
                                } else if (c == ')') {
                                    pList.add(sb.toString());
                                    break label;
                                } else if (!(tr = c == '\\')) sb.append(c);
                            }
                            if (sb.length() != 0) sb.append('\n');
                        } else {
                            sb.append('\n');
                        }
                        if (it.hasNext()) tmp = it.next();
                        else throw new ScriptException("Can't parse params for class: " + klass);
                    }
                    params = pList.toArray(new String[0]);
                }
                Class<?> clazz = SharedClassPool.callFromLoader(klass);
                if (clazz != null) {
                    Object ins = null;
                    try {
                        ins = clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException ignored) {}
                    if (ins != null) {
                        list.add(ins);
                        if (params != null) {
                            try {
                                Method me = clazz.getMethod("setParams", String[].class);
                                try {
                                    Method usg = clazz.getMethod("usageParams");
                                    Object re = usg.invoke(ins);
                                    if (re instanceof String) usageList.add(klass + ": " + re);
                                    else {
                                        crashMsg.add("usageParams() return non string: " + klass);
                                        continue;
                                    }
                                } catch (NoSuchMethodException e) {
                                    crashMsg.add("Method usageParams() not defined: " + klass);
                                    continue;
                                    //throw new ScriptException(klass + ": You must provide usageParams() method with setParams(String[]) and return String");
                                } catch (InvocationTargetException | IllegalAccessException ignored) {}
                                me.invoke(ins, (Object) params);
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
                                pw.println("setParams(String[]) doesn't defined, params will be ignored: ".concat(klass));
                            }
                        }
                    }
                } else {
                    crashMsg.add("class not found: " + klass);
                }
            }
        }
        if (!crashMsg.isEmpty()) {
            throw new ScriptException(String.join("\n", crashMsg));
        }
        if (!list.isEmpty()) {
            if (!usageList.isEmpty()) {
                pw.println("List of param usages:");
                for (String u : usageList) pw.println(u);
                pw.println();
            }
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
