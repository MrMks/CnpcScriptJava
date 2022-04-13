import com.github.mrmks.mc.cscriptjava.engine.ScriptJavaEngine;
import com.github.mrmks.mc.cscriptjava.engine.ScriptJavaEngineFactory;
import org.junit.Assert;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

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
}
