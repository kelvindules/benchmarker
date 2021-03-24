package dev.dules.methodlogger.util;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This class is a shorter version of org.springframework.util.ClassUtils
 * For more details refer to
 * https://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/util/ClassUtils.html
 */
public class ClassUtils {

	private ClassUtils() {
	}

	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8);

	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);
	}

	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		assertNotNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		assertNotNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	private static void assertNotNull(final Class<?> clazz, final String message) {
		if (clazz == null) {
			throw new IllegalArgumentException(message);
		}
	}
}
