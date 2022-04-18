import com.github.mrmks.mc.cscriptjava.engine.JavaLoaderEngine;
import com.github.mrmks.mc.cscriptjava.engine.ScriptJavaEngine;
import com.github.mrmks.mc.cscriptjava.engine.ScriptJavaEngineFactory;
import org.junit.Assert;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

public class Test {

    @org.junit.Test
    public void testCompile() throws ScriptException, NoSuchMethodException {
        InputStream stream = getClass().getResourceAsStream("example2.msj");
        InputStreamReader reader = new InputStreamReader(stream);

        String[] str = ScriptJavaEngine.constructTest(reader);
        System.out.println(Arrays.toString(str));

//        long stamp = System.currentTimeMillis();
//        ScriptEngine engine = new ScriptJavaEngineFactory().getScriptEngine();
//        Object obj = engine.eval(reader);
//        Assert.assertTrue(obj instanceof String);
//        Assert.assertEquals("*******", obj);
//        System.out.println(System.currentTimeMillis() - stamp);

        //((Invocable) engine).invokeFunction("init", "a");
    }

    @org.junit.Test
    public void testJLParams() throws ScriptException {

        InputStream stream = getClass().getResourceAsStream("params.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        Iterator<String> it = br.lines().iterator();

        it.hasNext();
        String tmp = it.next();

        String[] strings = JavaLoaderEngine.parseParams(tmp, it);
    }
}
