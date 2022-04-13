package com.github.mrmks.mc.cscriptjava.engine;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class ScriptJavaCompiled extends CompiledScript {

    private Object instance;
    private Class<?> clazz;
    private ScriptEngine engine;

    ScriptJavaCompiled(ScriptEngine engine, Class<?> clazz, Object instance) {
        this.engine = engine;
        this.clazz = clazz;
        this.instance = instance;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        Object ret = null;
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

            if (instance instanceof Runnable) {
                ((Runnable) instance).run();
            } else if (instance instanceof Supplier<?>) {
                ret = ((Supplier<?>) instance).get();
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ScriptException(e);
        } finally {
            try {
                context.getWriter().flush();
                context.getErrorWriter().flush();
            } catch (IOException ignored) {}
        }
        return ret;
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }
}
