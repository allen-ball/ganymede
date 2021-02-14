/*
 * $Id$
 *
 * Processed as a String format:
 *
 *      1$      galyleo-kernel JAR URL
 */
var __ = new java.net.URLClassLoader(new java.net.URL[] { new java.net.URL("%1$s") });

public void __magic_receive(String name, String magic, String code) {
    try {
        Class.forName("galyleo.shell.magic.Magic", true, __)
            .getMethod("receive", ClassLoader.class, String.class, String.class, String.class)
            .invoke(null, __, name, magic, code);
    } catch (Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}

public void print(Object object) {
    try {
        Class.forName("galyleo.shell.jshell.RemoteRuntime", true, __)
            .getMethod("print", Object.class)
            .invoke(null, object);
    } catch (Throwable throwable) {
        throwable.printStackTrace(System.err);
    }
}
