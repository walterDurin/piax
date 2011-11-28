/*
 * MethodUtil.java
 * 
 * Copyright (c) 2006- Osaka University
 * Copyright (c) 2004-2005 BBR Inc, Osaka University
 * 
 * Permission is hereby granted, free of charge, to any person obtaining 
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including 
 * without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to 
 * permit persons to whom the Software is furnished to do so, subject to 
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Revision History:
 * ---
 * 2007/11/01 designed and implemented by M. Yoshida.
 * 
 * $Id: MethodUtil.java 183 2010-03-03 11:41:21Z yos $
 */

package org.piax.trans.util;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.grlea.log.SimpleLogger;

/**
 * Method invocation utility
 * 
 * @author     Mikio Yoshida
 * @version    1.1.0
 */
public class MethodUtil {
    /*--- logger ---*/
    private static final SimpleLogger log = 
        new SimpleLogger(MethodUtil.class);
    
    /** key object for cache map */
    private static class MethodKey {
        final Class clazz;
        final Class superIf;
        final String methodName;
        final int paramNum;
        MethodKey(Class clazz, Class superIf, String methodName, int paramNum) {
            this.clazz = clazz;
            this.superIf = superIf;
            this.methodName = methodName;
            this.paramNum = paramNum;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof MethodKey)) 
                return false;
            MethodKey mkey = (MethodKey) o;
            return clazz == mkey.clazz
                && superIf == superIf
                && methodName.equals(mkey.methodName)
                && paramNum == mkey.paramNum;
        }
        
        @Override
        public int hashCode() {
            return clazz.hashCode() 
                ^ ((superIf == null) ? 0 : superIf.hashCode()) 
                ^ methodName.hashCode() ^ paramNum;
        }
    }

    /** map for cache use */
    private static WeakHashMap<MethodKey, Method[]> methodCache =
        new WeakHashMap<MethodKey, Method[]>();
    
    /**
     * 
     * @param clazz
     * @param superIf
     * @param methodName
     * @param paramNum
     * @return
     */
    private static Method[] getMethods(Class clazz, Class superIf, 
            String methodName, int paramNum) {
        MethodKey key = new MethodKey(clazz, superIf, methodName, paramNum);
        Method[] invocableMethods = methodCache.get(key);
        
        // if null, make invocable Method list
        if (invocableMethods == null) {
            // if superIf is not null, target classes will be sub interfaces
            // of superIf.
            Class[] classes = (superIf == null)? new Class[]{clazz}
                : ClassUtil.gatherLowerBoundSuperInterfaces(clazz, superIf);
            
            List<Method> methods = new ArrayList<Method>();
            for (Class cls : classes) {
                Method[] ms = cls.getMethods();
                for (Method method : ms) {
                    if (method.getName().equals(methodName)) {
                        // if declared class is not public, skip
                        if (!Modifier.isPublic(
                                method.getDeclaringClass().getModifiers()))
                            continue;
                        if (method.isVarArgs()) {
                            /*
                             * 可変長引数の場合はOKとする
                             */
                            methods.add(method);
                            continue;
                        }
                        Class[] params = method.getParameterTypes();
                        if (params.length == paramNum) {
                            methods.add(method);
                        }
                    }
                }
            }
            invocableMethods = new Method[methods.size()];
            methods.toArray(invocableMethods);
            methodCache.put(key, invocableMethods);
        }
        return invocableMethods;
    }
    
    /**
     * Converts primitive type to wrapper class.
     * 
     * @param type primitive type
     * @return wrapper class type
     */
    private static Class boxing(Class type) {
        if (type == Boolean.TYPE) return Boolean.class;
        if (type == Character.TYPE) return Character.class;
        if (type == Byte.TYPE) return Byte.class;
        if (type == Short.TYPE) return Short.class;
        if (type == Integer.TYPE) return Integer.class;
        if (type == Long.TYPE) return Long.class;
        if (type == Float.TYPE) return Float.class;
        if (type == Double.TYPE) return Double.class;
        return null;
    }
    
    /**
     * Tests whether the type represented by the specified argument can be
     * converted to the specified parameter type.
     * 
     * @param <T> parameter type
     * @param arg argument object
     * @param paramType parameter type
     * @return <code>true</code> if arg is assignable, <code>false</code> otherwise.
     */
    private static <T> boolean isAssignable(Object arg, Class<T> paramType) {
        /*
         *      param type <-- arg type
         * -----------------------------
         * OK   class          null
         * NG   primitive      null
         * OK   class          same or subclass
         * NG   class          superclass
         * OK   primitive      class (same mean by unboxing)
         * NG   primitive      class (other case)
         * 
         * Note: the arg type will not be primitive by autoboxing.
         */
        if (arg == null) {
            if (paramType.isPrimitive()) return false;
            return true;
        }
        Class argType = arg.getClass();
        if (paramType == argType) return true;
        Class<?> _paramType = paramType.isPrimitive() ? boxing(paramType)
                : paramType;
        if (_paramType.isAssignableFrom(argType)) return true;
        return false;
    }
    
    /**
     * Searches <code>Method</code> object which has the specified declared class
     * and the specified name and the parameters that are assignable from
     * the specified arguments.
     * <p>
     * if superIf is not <code>null</code>, the target methods will be restricted
     * of the interfaces which are super of specified class and are sub of
     * the specified superIf.
     * 
     * @param clazz declared class
     * @param superIf super interface
     * @param methodName method name
     * @param args the arguments used for the method call
     * @return the matched <code>Method</code> object,
     *          <code>null</code> if not matched.
     */
    private static Method getMethod(Class clazz, Class superIf,
            String methodName, Object... args) {
        int argNum = (args == null) ? 0 : args.length;
        Method[] invocableMethods = getMethods(clazz, superIf, methodName, argNum);

        for (Method method : invocableMethods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean matched = true;
            for (int i = 0; i < argNum; i++) {
                if (!isAssignable(args[i], paramTypes[i])) {
                    matched = false;
                    break;
                }
            }
            if (matched) return method;
            // さらに可変長引数のケースもチェック
            if (method.isVarArgs()) {
                matched = true;
                for (int i = 0; i < argNum; i++) {
                    if (!isAssignable(args[i], paramTypes[0].getComponentType())) {
                        matched = false;
                        break;
                    }
                }
                if (matched) return method;
            }
        }
        return null;
    }

    /**
     * Note: the specified method and its declared class should have 
     * the public modifier.
     * 
     * @param target target object
     * @param methodName method name
     * @param args the arguments used for the method call
     * @return the result of invoking the method
     * @throws NoSuchMethodException if a matching method is not found
     * @throws InvocationTargetException if the underlying method
     *          throws an exception.
     */
    public static Object invoke(Object target,
            String methodName, Object... args)
            throws NoSuchMethodException, InvocationTargetException {
        return invoke(target, null, methodName, args);
    }
    
    /**
     * Note: the specified method and its declared class should have 
     * the public modifier.
     * 
     * @param target target object
     * @param superIf super interface
     * @param methodName method name
     * @param args the arguments used for the method call
     * @return the result of invoking the method
     * @throws NoSuchMethodException if a matching method is not found
     * @throws InvocationTargetException if the underlying method
     *          throws an exception.
     */
    public static Object invoke(Object target, Class superIf,
            String methodName, Object... args) 
            throws NoSuchMethodException, InvocationTargetException {
        Method method = getMethod(target.getClass(), superIf, methodName, args);
        if (method == null)
            throw new NoSuchMethodException(methodName + " in " 
                    + target.getClass().getName());
        try {
            if (method.isVarArgs()) {
                /*
                 * 可変数引数を扱う場合、配列として扱うため、引数要素が代入
                 * 可能であっても配列としてcastできないという問題がある。
                 * このため、配列を用意し、互換性を保つ。
                 */
                Class[] ptypes = method.getParameterTypes();
                int arrIx = ptypes.length - 1;
                Object arr = Array.newInstance(
                        ptypes[arrIx].getComponentType(), 
                        args.length - arrIx);
                for (int i = 0; i < args.length - arrIx; i++) {
                    Array.set(arr, i, args[i + arrIx]);
                }
                Object[] args1 = new Object[arrIx + 1];
                for (int i = 0; i < arrIx; i++) {
                    args1[i] = args[i];
                }
                args1[arrIx] = arr;
                return method.invoke(target, args1);
            }
            return method.invoke(target, args);
        } catch (IllegalArgumentException e) {
            log.errorException(e);
            throw e;
        } catch (IllegalAccessException e) {
            log.errorException(e);
            throw new NoSuchMethodException(methodName + " in "
                    + target.getClass().getName());
        }
    }
}
