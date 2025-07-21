package com.biotak.util;

/**
 * Utility methods related to Java {@link Enum} handling.
 */
public final class EnumUtil {
    private EnumUtil() {}

    /**
     * Parses {@code name} into the given {@code enumClass}. If the name is null or invalid, returns {@code defaultValue}.
     */
    public static <E extends Enum<E>> E safeEnum(Class<E> enumClass, String name, E defaultValue) {
        if (name == null) return defaultValue;
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
} 