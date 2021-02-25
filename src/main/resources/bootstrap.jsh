/*
 * $Id$
 *
 * Processed as a String format:
 *
 *      1$      ganymede-kernel JAR URL
 *      2$      CellMethods Class Name
 */
var __ =
    new Object() {
        private final ClassLoader loader;

        {
            try {
                var url = new java.net.URL("%1$s");

                loader =
                    new java.net.URLClassLoader(new java.net.URL[] { url },
                                                getClass().getClassLoader());
            } catch (Exception exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        public ClassLoader getClassLoader() { return loader; }

        public Class<?> getClassForName(String name) throws Exception {
            return Class.forName(name, true, getClassLoader());
        }

        public Object invokeStaticMethod(String type, String method,
                                         Class<?>[] parameters, Object... arguments) {
            Object object = null;

            try {
                object =
                    getClassForName(type)
                    .getDeclaredMethod(method, parameters)
                    .invoke(null, arguments);
            } catch (Throwable throwable) {
                throwable.printStackTrace(System.err);
            }

            return object;
        }

        public Object invokeStaticMethod(String type, String method) {
            return invokeStaticMethod(type, method,
                                      new Class<?>[] { }, new Object[] { });
        }

        public void print(Object object) {
            invokeStaticMethod("%2$s", "print", new Class<?>[] { Object.class }, object);
        }
    };

public void print(Object object) { __.print(object); }
