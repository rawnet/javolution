/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2005 - Javolution (http://javolution.org/)
 * All rights reserved.
 * 
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package _templates.javolution.lang;

import _templates.java.lang.CharSequence;
import _templates.java.util.Iterator;
import _templates.javolution.context.ObjectFactory;
import _templates.javolution.text.TextBuilder;
import _templates.javolution.util.FastComparator;
import _templates.javolution.util.FastMap;
import _templates.javolution.util.FastSet;

/**
 * <p> This utility class greatly facilitates the use of reflection to invoke 
 *     constructors or methods which may or may not exist at runtime or
 *     may be loaded dynamically (such as when running on a
 *     <a href="http://www.osgi.org/">OSGI Platform</a>).</p>
 *
 * <p> Applications using custom class loaders may add them to the research
 *     tree. For example:[code]
 *         public class Activator implements BundleActivator {
 *              public void start(BundleContext context) throws Exception {
 *                   Reflection.getInstance().add(Activator.class.getClassLoader());
 *                   ...
 *              }
 *              public void stop(BundleContext context) throws Exception {
 *                   Reflection.getInstance().remove(Activator.class.getClassLoader());
 *                   ...
 *              }
 *         }[/code]</p>
 *
 * <p> The constructors/methods are identified through their signatures
 *     represented as a {@link String}. When the constructor/method does
 *     not exist (e.g. class not found) or when the platform does not support
 *     reflection, the constructor/method is <code>null</code> 
 *     (no exception raised). Here is an example of timer taking advantage 
 *     of the new (JRE1.5+) high resolution time when available:[code]
 *     public static long microTime() {
 *         if (NANO_TIME_METHOD != null) { // JRE 1.5+
 *             Long time = (Long) NANO_TIME_METHOD.invoke(null); // Static method.
 *             return time.longValue() / 1000;
 *         } else { // Use the less accurate time in milliseconds.
 *             return System.currentTimeMillis() * 1000;
 *         }
 *     }
 *     private static final Reflection.Method NANO_TIME_METHOD 
 *         = Reflection.getInstance().getMethod("java.lang.System.nanoTime()");[/code]</p>
 *   
 * <p> Arrays and primitive types are supported. For example:[code]
 *     Reflection.Constructor sbc = Reflection.getInstance().getConstructor("java.lang.StringBuilder(int)");
 *     if (sbc != null) { // JDK 1.5+
 *        Object sb = sbc.newInstance(new Integer(32));
 *        Reflection.Method append = Reflection.getInstance().getMethod("java.lang.StringBuilder.append(char[], int, int)");
 *        append.invoke(sb, new char[] { 'h', 'i' }, new Integer(0), new Integer(2));
 *        System.out.println(sb);
 *    }
 * 
 *    > hi[/code]</p>
 * 
 * @author  <a href="mailto:jean-marie@dautelle.com">Jean-Marie Dautelle</a>
 * @version 5.4, December 3, 2009
 */
public abstract class Reflection {

    /**
     * Holds the default implementation (configurable).
     */
    private static volatile Reflection INSTANCE = new Default();

    /**
     * Holds the XMLOutputFactory default implementation (configurable).
     */
    public static final Configurable/*<Class<? extends Reflection>>*/CLASS
        = new Configurable(Default.class) {
           protected void notifyChange(Object oldValue, Object newValue) {
                 INSTANCE = (Reflection) ObjectFactory.getInstance((Class) newValue).object();
          }
    };

    /**
     * Returns the current reflection instance. The implementation class
     * is defined by {@link #CLASS} (configurable}.
     *
     * @return the reflection instance.
     */
    public static final Reflection getInstance() {
        return INSTANCE;
    }

    /**
     * Adds the specified class loader to the research tree.
     *
     * @param classLoader the class loader being added.
     */
    public abstract void add(Object classLoader);

    /**
     * Removes the specified class loader from the research tree.
     * This method clears any cache data to allow for classes
     * associated to the specified class loader to be garbage collected.
     *
     * @param classLoader the class loader being removed.
     */
    public abstract void remove(Object classLoader);

    /**
     * Returns the class having the specified name. This method searches the
     * class loader of the reflection implementation, then any
     * {@link #add additional} class loaders.
     * If the class is found, it is initialized
     * and returned; otherwise <code>null</code> is returned.
     * The class may be cached for performance reasons.
     *
     * @param name the name of the class to search for.
     * @return the corresponding class or <code>null</code>
     */
    public abstract Class getClass(CharSequence name);

    /**
     * Equivalent to {@link #getClass(CharSequence)} (for J2ME compatibility).
     */
    public Class getClass(String name) {
        Object obj = name;
        if (obj instanceof CharSequence)
            return getClass((CharSequence) obj);
        // String not a CharSequence on J2ME
        TextBuilder tmp = TextBuilder.newInstance();
        try {
            tmp.append(name);
            return getClass(tmp);
        } finally {
            TextBuilder.recycle(tmp);
        }
    }

    /**
     * Returns the parent class of the specified class or interface.
     *
     * @param forClass the class for which the parent class is returned.
     * @return the parent class of the specified class or <code>null</code>
     *         if none (e.g. Object.class or top interface).
     */
    public abstract Class getSuperclass(Class forClass);

    /**
     * Returns the interfaces implemented by the specified class or interface.
     *
     * @param forClass the class for which the interfaces are returned.
     * @return an array holding the interfaces implemented (empty if none).
     */
    public abstract Class[] getInterfaces(Class forClass);

    /**
     * Returns the constructor having the specified signature.
     *
     * @param signature the textual representation of the constructor signature.
     * @return the corresponding constructor or <code>null</code> if none
     *         found.
     */
    public abstract Constructor getConstructor(String signature);

    /**
     * Returns the method having the specified signature.
     *
     * @param signature the textual representation of the method signature.
     * @return the corresponding constructor or <code>null</code> if none
     *         found.
     */
    public abstract Method getMethod(String signature);

    /**
     * This class represents a run-time constructor obtained through reflection.
     *
     * Here are few examples of utilization:[code]
     * // Default constructor (fastList = new FastList())
     * Reflection.Constructor fastListConstructor
     *     = Reflection.getInstance().getConstructor("javolution.util.FastList()");
     * Object fastList = fastListConstructor.newInstance();
     *
     * // Constructor with arguments (fastMap = new FastMap(64))
     * Reflection.Constructor fastMapConstructor
     *     = Reflection.getInstance().getConstructor("javolution.util.FastMap(int)");
     * Object fastMap = fastMapConstructor.newInstance(new Integer(64));
     * [/code]
     */
    public abstract class Constructor {

        /**
         * Holds the parameter types.
         */
        private final Class[] _parameterTypes;

        /**
         * Creates a new constructor having the specified parameter types.
         *
         * @param parameterTypes the parameters types.
         */
        protected Constructor(Class[] parameterTypes) {
            _parameterTypes = parameterTypes;
        }

        /**
         * Returns an array of <code>Class</code> objects that represents
         * the formal parameter types, in declaration order of this constructor.
         *
         * @return the parameter types for this constructor.
         */
        public Class[] getParameterTypes() {
            return _parameterTypes;
        }

        /**
         * Allocates a new object using this constructor with the specified
         * arguments.
         *
         * @param args the constructor arguments.
         * @return the object being instantiated.
         */
        protected abstract Object allocate(Object[] args);

        /**
         * Invokes this constructor with no argument (convenience method).
         *
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 0</code>
         */
        public final Object newInstance() {
            if (_parameterTypes.length != 0)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            return allocate(EMPTY_ARRAY);
        }

        /**
         * Invokes this constructor with the specified single argument.
         *
         * @param arg0 the first argument.
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 1</code>
         */
        public final Object newInstance(Object arg0) {
            if (_parameterTypes.length != 1)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0};
            return allocate(args);
        }

        /**
         * Invokes this constructor with the specified two arguments.
         *
         * @param arg0 the first argument.
         * @param arg1 the second argument.
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 2</code>
         */
        public final Object newInstance(Object arg0, Object arg1) {
            if (_parameterTypes.length != 2)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1};
            return allocate(args);
        }

        /**
         * Invokes this constructor with the specified three arguments.
         *
         * @param arg0 the first argument.
         * @param arg1 the second argument.
         * @param arg2 the third argument.
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 3</code>
         */
        public final Object newInstance(Object arg0, Object arg1, Object arg2) {
            if (_parameterTypes.length != 3)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1, arg2};
            return allocate(args);
        }

        /**
         * Invokes this constructor with the specified four arguments.
         *
         * @param arg0 the first argument.
         * @param arg1 the second argument.
         * @param arg2 the third argument.
         * @param arg3 the fourth argument.
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 4</code>
         */
        public final Object newInstance(Object arg0, Object arg1, Object arg2, Object arg3) {
            if (_parameterTypes.length != 4)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1, arg2, arg3};
            return allocate(args);
        }
        /**
         * Invokes this constructor with the specified arguments.
         *
         * @param args the arguments.
         * @return the object being instantiated.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != args.length</code>
        @JVM-1.5+@
        public final Object newInstance(Object... args) {
        if (_parameterTypes.length != args.length)
        throw new IllegalArgumentException(
        "Expected number of parameters is " + _parameterTypes.length);
        return allocate(args);
        }
        /**/
    }

    /**
     * This class represents a run-time method obtained through reflection.
     *
     * Here are few examples of utilization:[code]
     * // Non-static method: fastMap.put(myKey, myValue)
     * Reflection.Method putKeyValue
     *     = Reflection.getInstance().getMethod(
     *         "javolution.util.FastMap.put(java.lang.Object, java.lang.Object)");
     * Object previous = putKeyValue.invoke(fastMap, myKey, myValue);
     *
     * // Static method: System.nanoTime()  (JRE1.5+)
     * Reflection.Method nanoTime
     *     = Reflection.getInstance().getMethod("java.lang.System.nanoTime()");
     * long time = ((Long)nanoTime.invoke(null)).longValue();[/code]
     */
    public abstract class Method {

        /**
         * Holds the parameter types.
         */
        private final Class[] _parameterTypes;

        /**
         * Creates a new constructor having the specified parameter types.
         *
         * @param parameterTypes the parameters types.
         */
        protected Method(Class[] parameterTypes) {
            _parameterTypes = parameterTypes;
        }

        /**
         * Returns an array of <code>Class</code> objects that represents
         * the formal parameter types, in declaration order of this constructor.
         *
         * @return the parameter types for this constructor.
         */
        public Class[] getParameterTypes() {
            return _parameterTypes;
        }

        /**
         * Executes this method with the specified arguments.
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param args the method arguments.
         * @return the result of the execution.
         */
        protected abstract Object execute(Object thisObject, Object[] args);

        /**
         * Invokes this method on the specified object which might be
         * <code>null</code> if the method is static (convenience method).
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 0</code>
         */
        public final Object invoke(Object thisObject) {
            if (_parameterTypes.length != 0)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            return execute(thisObject, EMPTY_ARRAY);
        }

        /**
         * Invokes this method with the specified single argument
         * on the specified object which might be <code>null</code>
         * if the method is static (convenience method).
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param arg0 the single argument.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 1</code>
         */
        public final Object invoke(Object thisObject, Object arg0) {
            if (_parameterTypes.length != 1)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0};
            return execute(thisObject, args);
        }

        /**
         * Invokes this method with the specified two arguments
         * on the specified object which might be <code>null</code>
         * if the method is static (convenience method).
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param arg0 the first argument.
         * @param arg1 the second argument.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 2</code>
         */
        public final Object invoke(Object thisObject, Object arg0, Object arg1) {
            if (_parameterTypes.length != 2)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1};
            return execute(thisObject, args);
        }

        /**
         * Invokes this method with the specified three arguments
         * on the specified object which might be <code>null</code>
         * if the method is static.
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param arg0 the first argument (convenience method).
         * @param arg1 the second argument.
         * @param arg2 the third argument.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 3</code>
         */
        public final Object invoke(Object thisObject, Object arg0, Object arg1,
                Object arg2) {
            if (_parameterTypes.length != 3)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1, arg2};
            return execute(thisObject, args);
        }

        /**
         * Invokes this method with the specified four arguments
         * on the specified object which might be <code>null</code>
         * if the method is static (convenience method).
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param arg0 the first argument.
         * @param arg1 the second argument.
         * @param arg2 the third argument.
         * @param arg3 the fourth argument.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != 4</code>
         */
        public final Object invoke(Object thisObject, Object arg0, Object arg1,
                Object arg2, Object arg3) {
            if (_parameterTypes.length != 4)
                throw new IllegalArgumentException(
                        "Expected number of parameters is " + _parameterTypes.length);
            Object[] args = {arg0, arg1, arg2, arg3};
            return execute(thisObject, args);
        }
        /**
         * Invokes this method with the specified arguments
         * on the specified object which might be <code>null</code>
         * if the method is static.
         *
         * @param thisObject the object upon which this method is invoked
         *        or <code>null</code> for static methods.
         * @param args the arguments.
         * @return the result of the invocation.
         * @throws IllegalArgumentException if
         *          <code>this.getParametersTypes().length != args.length</code>
        @JVM-1.5+@
        public final Object invoke(Object thisObject, Object... args) {
        if (_parameterTypes.length != args.length)
        throw new IllegalArgumentException(
        "Expected number of parameters is " + _parameterTypes.length);
        return execute(thisObject, args);
        }
        /**/
    }
    private static final Object[] EMPTY_ARRAY = new Object[0]; // Immutable.

    //Holds default implementation.
    private static final class Default extends Reflection {

        /**
         * Holds the additional class loaders (actually a set).
         */
        private final FastSet _classLoaders = new FastSet();

        /**
         * Holds the name-to-class mapping (cache).
         */
        private final FastMap _nameToClass = new FastMap().setShared(true).setKeyComparator(FastComparator.LEXICAL);

        // Implements abstract method.
        public void add(Object classLoader) {
            _classLoaders.add(classLoader);
        }

        // Implements abstract method.
        public void remove(Object classLoader) {
            _classLoaders.remove(classLoader);
            _nameToClass.clear(); // Clear cache.
        }

        // Implements abstract method.
        public Class getClass(CharSequence name) {
            Class cls = (Class) _nameToClass.get(name);
            return (cls != null) ? cls : searchClass(name.toString());
        }

        private Class searchClass(String name) {
            Class cls = null;
            try {
                cls = Class.forName(name);
            } catch (ClassNotFoundException e1) {
                /* @JVM-1.4+@
                for (Iterator i = _classLoaders.iterator(); i.hasNext();) {
                    ClassLoader classLoader = (ClassLoader) i.next();
                    try {
                        cls = Class.forName(name, true, classLoader);
                    } catch (ClassNotFoundException e2) {
                        // Not found, continue.
                    }
                }
                /**/
            }
            if (cls != null) { // Cache the result.
                _nameToClass.put(name, cls);
            }
            return cls;
        }

        // Implements abstract method.
        public Constructor getConstructor(String signature) {
            int argStart = signature.indexOf('(') + 1;
            if (argStart < 0) {
                throw new IllegalArgumentException("Parenthesis '(' not found");
            }
            int argEnd = signature.indexOf(')');
            if (argEnd < 0) {
                throw new IllegalArgumentException("Parenthesis ')' not found");
            }
            String className = signature.substring(0, argStart - 1);
            Class theClass = getClass(className);
            if (theClass == null) {
                return null;
            }
            String args = signature.substring(argStart, argEnd);
            if (args.length() == 0) {
                return new DefaultConstructor(theClass);
            }
            /*@JVM-1.4+@
            Class[] argsTypes = classesFor(args);
            if (argsTypes == null) return null;
            try {
            return new ReflectConstructor(theClass.getConstructor(argsTypes),
            signature);
            } catch (NoSuchMethodException e) {
            }
            /**/
            return null;
        }

        public Class[] getInterfaces(Class cls) {
        /*@JVM-1.4+@
        if (true) return cls.getInterfaces();
        /**/
        return new Class[0];
        }

        public Class getSuperclass(Class cls) {
        /*@JVM-1.4+@
        if (true) return cls.getSuperclass();
        /**/
        return null;
        }

        private class DefaultConstructor extends Constructor {

            final Class _class;

            DefaultConstructor(Class cl) {
                super(new Class[0]); // No arguments.
                _class = cl;
            }

            public Object allocate(Object[] args) {
                try {
                    return _class.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("Default constructor instantiation error for " + _class.getName() + " (" + e.getMessage() + ")");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Default constructor illegal access error for " + _class.getName() + " (" + e.getMessage() + ")");
                }
            }

            public String toString() {
                return _class + " default constructor";
            }
        }

        /*@JVM-1.4+@
        private final class ReflectConstructor extends Constructor {
        private final java.lang.reflect.Constructor _value;
        
        private final String _signature;
        
        public ReflectConstructor(java.lang.reflect.Constructor value,
        String signature) {
        super(value.getParameterTypes());
        _value = value;
        _signature = signature;
        }
        
        public Object allocate(Object[] args) {
        try {
        return _value.newInstance(args);
        } catch (InstantiationException e) {
        throw new RuntimeException("Instantiation error for "
        + _signature + " constructor", e);
        } catch (IllegalAccessException e) {
        throw new RuntimeException("Illegal access error for "
        + _signature + " constructor", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException)e.getCause();
        throw new RuntimeException("Invocation exception  for "
        + _signature + " constructor",
        (java.lang.reflect.InvocationTargetException) e.getCause());
        }
        }
        
        public String toString() {
        return _signature + " constructor";
        }
        }
        /**/
        // Implements abstract method.
        public Method getMethod(String signature) {
            /*@JVM-1.4+@
            int argStart = signature.indexOf('(') + 1;
            if (argStart < 0) {
            throw new IllegalArgumentException("Parenthesis '(' not found");
            }
            int argEnd = signature.indexOf(')');
            if (argEnd < 0) {
            throw new IllegalArgumentException("Parenthesis ')' not found");
            }
            int nameStart = signature.substring(0, argStart).lastIndexOf('.') + 1;
            try {
            
            String className = signature.substring(0, nameStart - 1);
            Class theClass = getClass(className);
            if (theClass == null) return null;
            String methodName = signature.substring(nameStart, argStart - 1);
            String args = signature.substring(argStart, argEnd);
            Class[] argsTypes = classesFor(args);
            if (argsTypes == null) return null;
            return new ReflectMethod(theClass.getMethod(methodName, argsTypes),
            signature);
            } catch (Throwable t) {
            }
            /**/
            return null;
        }

        /*@JVM-1.4+@
        private final class ReflectMethod extends Method {
        
        private final java.lang.reflect.Method _value;
        
        private final String _signature;
        
        public ReflectMethod(java.lang.reflect.Method value, String signature) {
        super(value.getParameterTypes());
        _value = value;
        _signature = signature;
        }
        
        public Object execute(Object that, Object[] args) {
        try {
        return _value.invoke(that, args);
        } catch (IllegalAccessException e) {
        throw new IllegalAccessError("Illegal access error for " + _signature + " method");
        } catch (java.lang.reflect.InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
        throw new RuntimeException(
        "Invocation exception for " + _signature + " method",
        (java.lang.reflect.InvocationTargetException) e.getCause());
        }
        }
        
        public String toString() {
        return _signature + " method";
        }
        }
        /**/
        /**
         * Returns the classes for the specified argument.
         *
         * @param args the comma separated arguments.
         * @return the classes or <code>null</code> if one of the class is not found.
        @JVM-1.4+@
        private Class[] classesFor(String args) {
        args = args.trim();
        if (args.length() == 0) {
        return new Class[0];
        }
        // Counts commas.
        int commas = 0;
        for (int i=0;;) {
        i = args.indexOf(',', i);
        if (i++ < 0) break;
        commas++;
        }
        Class[] classes = new Class[commas + 1];
        
        int index = 0;
        for (int i = 0; i < commas; i++) {
        int sep = args.indexOf(',', index);
        classes[i] = classFor(args.substring(index, sep).trim());
        if (classes[i] == null) return null;
        index = sep + 1;
        }
        classes[commas] = classFor(args.substring(index).trim());
        if (classes[commas] == null) return null;
        return classes;
        }
        
        private Class classFor(String className)  {
        int arrayIndex = className.indexOf("[]");
        if (arrayIndex >= 0) {
        if (className.indexOf("[][]") >= 0) {
        if (className.indexOf("[][][]") >= 0) {
        if (className.indexOf("[][][][]") >= 0) {
        throw new UnsupportedOperationException(
        "The maximum array dimension is 3");
        } else { // Dimension three.
        return getClass("[[["
        + descriptorFor(className.substring(0,
        arrayIndex)));
        }
        } else { // Dimension two.
        return getClass("[["
        + descriptorFor(className.substring(0, arrayIndex)));
        }
        } else { // Dimension one.
        return getClass("["
        + descriptorFor(className.substring(0, arrayIndex)));
        }
        }
        if (className.equals("boolean")) {
        return boolean.class;
        } else if (className.equals("byte")) {
        return byte.class;
        } else if (className.equals("char")) {
        return char.class;
        } else if (className.equals("short")) {
        return short.class;
        } else if (className.equals("int")) {
        return int.class;
        } else if (className.equals("long")) {
        return long.class;
        } else if (className.equals("float")) {
        return float.class;
        } else if (className.equals("double")) {
        return double.class;
        } else {
        return getClass(className);
        }
        }
        
        private static String descriptorFor(String className) {
        if (className.equals("boolean")) {
        return "Z";
        } else if (className.equals("byte")) {
        return "B";
        } else if (className.equals("char")) {
        return "C";
        } else if (className.equals("short")) {
        return "S";
        } else if (className.equals("int")) {
        return "I";
        } else if (className.equals("long")) {
        return "J";
        } else if (className.equals("float")) {
        return "F";
        } else if (className.equals("double")) {
        return "D";
        } else {
        return "L" + className + ";";
        }
        }
        /**/
    }

    /**
     * @deprecated To be replaced by <code>Reflection.getInstance().getConstructor(signature)
     */
    public static Constructor getConstructor(Object signature) {
        return Reflection.getInstance().getConstructor((String)signature);
    }

    /**
     * @deprecated To be replaced by <code>Reflection.getInstance().getMethod(signature)
     */
    public static Method getMethod(Object signature) {
        return Reflection.getInstance().getMethod((String)signature);
    }
}
