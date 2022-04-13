package com.github.mrmks.mc.cscriptjava.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JavaLoaderEngineFactory implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        return "JavaLoader Engine";
    }

    @Override
    public String getEngineVersion() {
        return "0.0.1";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("jl");
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.singletonList("text/plain");
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList("JavaLoader", "jl");
    }

    @Override
    public String getLanguageName() {
        return "JavaLoader";
    }

    @Override
    public String getLanguageVersion() {
        return "0.0.1";
    }

    @Override
    public Object getParameter(String key) {
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        return "";
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "";
    }

    @Override
    public String getProgram(String... statements) {
        return "";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new JavaLoaderEngine(this);
    }
}
