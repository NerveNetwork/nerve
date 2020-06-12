package io.nuls.api.utils;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import io.nuls.core.log.Log;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class PropertyUtils {

    private static final String GET = "get";
    private static final String IS = "is";
    private static final String SET = "set";

    public PropertyUtils() {
    }

    public static void copyProperties(Object dest, Object src) {
        String[] srcPropertyNames = getPropertyNames(src);
        String[] destNames = getPropertyNames(dest);
        String[] var4 = srcPropertyNames;
        int var5 = srcPropertyNames.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            String property = var4[var6];
            Object value = getValue(src, property);
            if (!(value instanceof Map) && !(value instanceof Collection) && isStringInArray(property, destNames)) {
                setValue(dest, property, value);
            }
        }

    }

    public static void copyProperties(Object dest, Map<String, Object> src) {
        String[] destNames = getPropertyNames(dest);
        Iterator var3 = src.keySet().iterator();

        while (var3.hasNext()) {
            String property = (String) var3.next();
            Object value = src.get(property);
            if (!(value instanceof Map) && !(value instanceof Collection) && isStringInArray(property, destNames)) {
                setValue(dest, property, value);
            }
        }

    }

    public static void copyPropertiesIgnore(Object dest, Object src, String[] ignore) {
        String[] srcPropertyNames = getPropertyNames(src);
        String[] destNames = getPropertyNames(dest);
        String[] var5 = srcPropertyNames;
        int var6 = srcPropertyNames.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            String property = var5[var7];
            Object value = getValue(src, property);
            if (!(value instanceof Map) && !(value instanceof Collection) && isStringInArray(property, destNames) && !isStringInArray(property, ignore)) {
                setValue(dest, property, value);
            }
        }

    }

    public static void copyProperties(Object dest, Object src, String[] include) {
        String[] srcPropertyNames = getPropertyNames(src);
        String[] destNames = getPropertyNames(dest);
        String[] var5 = srcPropertyNames;
        int var6 = srcPropertyNames.length;

        for (int var7 = 0; var7 < var6; ++var7) {
            String property = var5[var7];
            Object value = getValue(src, property);
            if (!(value instanceof Map) && !(value instanceof Collection) && isStringInArray(property, destNames) && isStringInArray(property, include)) {
                setValue(dest, property, value);
            }
        }

    }

    public static void copyNotNullProperties(Object dest, Object src) {
        if (dest != null && src != null) {
            String[] properties = getPropertyNames(src);
            String[] destNames = getPropertyNames(dest);
            String[] var4 = properties;
            int var5 = properties.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                String property = var4[var6];
                Object value = getValue(src, property);
                if (value != null && isStringInArray(property, destNames)) {
                    setValue(dest, property, value);
                }
            }
        }

    }

    public static void copyPropertiesIfNull(Object dest, Object src) {
        if (dest != null && src != null) {
            String[] properties = getPropertyNames(src);
            String[] destNames = getPropertyNames(dest);
            String[] var4 = properties;
            int var5 = properties.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                String property = var4[var6];
                Object srcValue = getValue(src, property);
                Object destValue = getValue(dest, property);
                if (destValue == null && isStringInArray(property, destNames)) {
                    setValue(dest, property, srcValue);
                }
            }
        }

    }

    public static void copyNotNullPropertiesIgnore(Object dest, Object src, String[] ignore) {
        if (dest != null && src != null) {
            String[] properties = getPropertyNames(src);
            String[] destNames = getPropertyNames(dest);
            String[] var5 = properties;
            int var6 = properties.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                String property = var5[var7];
                Object value = getValue(src, property);
                if (value != null && isStringInArray(property, destNames) && !isStringInArray(property, ignore)) {
                    setValue(dest, property, value);
                }
            }
        }

    }

    public static Map<String, Object> getValues(Object obj, boolean ignoreNull) {
        Map<String, Object> values = new HashMap();
        String[] propertyNames = getPropertyNames(obj);
        if (propertyNames != null) {
            String[] var4 = propertyNames;
            int var5 = propertyNames.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                String property = var4[var6];
                Object value = getValue(obj, property);
                if (!ignoreNull || value != null) {
                    values.put(property, value);
                }
            }
        }

        return values;
    }

    public static String[] getPropertyNames(Object obj) {
        try {
            BeanInfo info = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] properties = info.getPropertyDescriptors();
            List<String> result = new ArrayList();
            PropertyDescriptor[] var4 = properties;
            int var5 = properties.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                PropertyDescriptor property = var4[var6];
                if (!property.getName().equals("class")) {
                    result.add(property.getName());
                }
            }

            return (String[]) result.toArray(new String[result.size()]);
        } catch (IntrospectionException var8) {
            return new String[0];
        }
    }

    public static boolean setValue(Object object, String property, Object value) {
        if (property != null && object != null) {
            StringTokenizer st = new StringTokenizer(property, ".");
            if (st.countTokens() == 0) {
                return false;
            } else {
                Object current = object;

                try {
                    for (int i = 0; st.hasMoreTokens(); ++i) {
                        String currentPropertyName = st.nextToken();
                        if (i >= st.countTokens()) {
                            try {
                                PropertyDescriptor pd = new PropertyDescriptor(currentPropertyName, current.getClass());
                                pd.getWriteMethod().invoke(current, value);
                                return true;
                            } catch (Exception var8) {
                                return false;
                            }
                        }

                        current = invokeProperty(current, currentPropertyName);
                    }

                    return true;
                } catch (NullPointerException var9) {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static Object getValue(Object object, String property) {
        if (property != null && object != null) {
            StringTokenizer st = new StringTokenizer(property, ".");
            if (st.countTokens() == 0) {
                return null;
            } else {
                Object result = object;

                try {
                    while (st.hasMoreTokens()) {
                        String currentPropertyName = st.nextToken();
                        result = invokeProperty(result, currentPropertyName);
                    }

                    return result;
                } catch (NullPointerException var5) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static Class getPropertyClass(Object obj, String property) {
        String[] propertyNames = getPropertyNames(obj);
        if (!isStringInArray(property, propertyNames)) {
            return null;
        } else {
            Class cls = obj.getClass();

            try {
                PropertyDescriptor pd = new PropertyDescriptor(property, cls);
                return pd.getPropertyType();
            } catch (IntrospectionException var5) {
                return null;
            }
        }
    }

    public static boolean isPropertyAnnotatedBy(Class cls, String propertyName, Class<? extends Annotation> ann) {
        Annotation[] annotations = getPropertyAnnotations(cls, propertyName);
        Annotation[] var4 = annotations;
        int var5 = annotations.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            Annotation annotation = var4[var6];
            if (annotation.getClass().equals(ann) || ann.isAssignableFrom(annotation.getClass())) {
                return true;
            }
        }

        return false;
    }

    public static Annotation[] getPropertyAnnotations(Class cls, String proeprtyName) {
        List<Annotation> list = new ArrayList();
        Field field = getField(cls, proeprtyName);
        if (field != null) {
            list.addAll(Arrays.asList(field.getDeclaredAnnotations()));
        }

        Method getter = getGetMethod(cls, proeprtyName);
        if (getter != null) {
            list.addAll(Arrays.asList(getter.getAnnotations()));
        }

        Method setter = getSetMethod(cls, proeprtyName);
        if (setter != null) {
            list.addAll(Arrays.asList(setter.getAnnotations()));
        }

        return (Annotation[]) list.toArray(new Annotation[list.size()]);
    }

    private static Object invokeProperty(Object obj, String property) {
        if (property != null && property.length() != 0) {
            Class cls = obj.getClass();
            Object[] oParams = new Object[0];
            Class[] cParams = new Class[0];

            try {
                Method method = cls.getMethod(createMethodName("get", property), cParams);
                return method.invoke(obj, oParams);
            } catch (Exception var12) {
                try {
                    Method method = cls.getMethod(createMethodName("is", property), cParams);
                    return method.invoke(obj, oParams);
                } catch (Exception var11) {
                    try {
                        Method method = cls.getMethod(property, cParams);
                        return method.invoke(obj, oParams);
                    } catch (Exception var10) {
                        try {
                            Field field = cls.getField(property);
                            return field.get(obj);
                        } catch (Exception var9) {
                            return null;
                        }
                    }
                }
            }
        } else {
            return null;
        }
    }

    private static String createMethodName(String prefix, String propertyName) {
        return prefix + propertyName.toUpperCase().charAt(0) + propertyName.substring(1);
    }

    public static Class getGenericType(Class targetClass, String property) {
        try {
            Field field = getField(targetClass, property);
            Type genericType = null;
            if (field != null) {
                genericType = field.getGenericType();
            }

            Method getter;
            if (genericType == null || !(genericType instanceof ParameterizedType)) {
                getter = getSetMethod(targetClass, property);
                if (getter != null) {
                    genericType = getter.getGenericParameterTypes()[0];
                }
            }

            if (genericType == null || !(genericType instanceof ParameterizedType)) {
                getter = getGetMethod(targetClass, property);
                if (getter != null) {
                    genericType = getter.getGenericReturnType();
                }
            }

            if (genericType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericType;
                int index = 0;
                Type resultType = type.getActualTypeArguments()[index];
                if (resultType instanceof ParameterizedType) {
                    return resultType.getClass();
                }

                return (Class) resultType;
            }
        } catch (Exception var7) {
            Log.warn(var7.getMessage());
        }

        return null;
    }

    public static Field getField(Class targetClass, String property) {
        try {
            return targetClass.getDeclaredField(property);
        } catch (Exception var3) {
            return null;
        }
    }

    public static Method getGetMethod(Class targetClass, String property) {
        try {
            return targetClass.getDeclaredMethod(createMethodName("get", property));
        } catch (NoSuchMethodException var5) {
            try {
                return targetClass.getDeclaredMethod(createMethodName("is", property));
            } catch (NoSuchMethodException var4) {
                return null;
            }
        }
    }

    public static Method getSetMethod(Class targetClass, String property) {
        Field field = getField(targetClass, property);
        if (field != null) {
            try {
                return targetClass.getDeclaredMethod(createMethodName("set", property), field.getType());
            } catch (NoSuchMethodException var8) {
                return null;
            }
        } else {
            Method[] methods = targetClass.getDeclaredMethods();
            Method[] var4 = methods;
            int var5 = methods.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                Method method = var4[var6];
                if (method.getName().equals(createMethodName("set", property))) {
                    return method;
                }
            }

            return null;
        }
    }

    public static boolean isStringInArray(String str, String[] strArray) {
        String[] var2 = strArray;
        int var3 = strArray.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String a = var2[var4];
            if (a.equals(str)) {
                return true;
            }
        }

        return false;
    }


}
