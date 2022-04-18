msj脚本总是会被编译为一个特定的类，为了减少文件编译次数提升性能，您可以使用JavaLoader来使用这些类。
例如，有如下msj脚本
#!+foo.bat.Test
print("Hello World");

您可以如此在JavaLoader中使用:
foo.bat.Test

为了提高类的复用程度，您可以提供一对成对的方法来接收参数:

public void setParams(String[] args) {}
public String usageParams() {}

在setParams中，您可以按需要解析收到的字符串内容。同时，您也必须在usageParams中向可能的其他开发者表述这些参数是如何被解析的。
在提供setParams方法时，您必须提供usageParams方法。
例如下面的msj脚本:

#!+foo.bar.Test
print(msg);
#!
private String msg = "Hello World";
public void setParams(String[] args) {
    if (args.length > 0) msg = args[0];
}

public String usageParams() {
    return "[msg]";
}

在jl中，您可以如此设置参数:
foo.bar.Test("Good Morning")

参数间使用 ',' 隔开，使用 '\"'表现 '"' 本身，同时亦可使用 '\n' 表示换行符

此外，有一些特殊规则:
不提供包名，同时以 'R', 'S' 或 'A' 做开头并以数字结尾的类名的类无法被jl调用