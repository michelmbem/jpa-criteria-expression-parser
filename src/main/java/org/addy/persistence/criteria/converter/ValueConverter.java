package org.addy.persistence.criteria.converter;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.metamodel.Attribute;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

public final class ValueConverter {

    private ValueConverter() {
    }

    /**
     * Converts a value to the Java type of the JPA attribute.
     *
     * @param value the source value
     * @param attribute the JPA metamodel attribute
     * @return the converted value
     */
    public static Object convertForAttribute(Object value, Attribute<?, ?> attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("attribute cannot be null");
        }

        TemporalType temporalType = findTemporalType(attribute);
        return convert(value, attribute.getJavaType(), temporalType);
    }

    /**
     * Converts a value to the requested Java type.
     *
     * @param value source value
     * @param targetType target Java type
     * @param temporalType @Temporal mapping, when applicable
     */
    public static Object convert(
            Object value,
            Class<?> targetType,
            TemporalType temporalType) {

        if (targetType == null) {
            throw new IllegalArgumentException("targetType cannot be null");
        }

        if (value == null) {
            return null;
        }

        Class<?> boxedType = box(targetType);

        /*
         * Already the correct type.
         */
        if (boxedType.isInstance(value)) {
            return value;
        }

        /*
         * Character.
         */
        if (boxedType == Character.class) {
            return convertCharacter(value);
        }

        /*
         * String.
         */
        if (boxedType == String.class) {
            return String.valueOf(value);
        }

        /*
         * Boolean.
         */
        if (boxedType == Boolean.class) {
            return convertBoolean(value);
        }

        /*
         * Numeric types.
         */
        if (Number.class.isAssignableFrom(boxedType)) {
            return convertNumber(value, boxedType);
        }

        /*
         * Enum.
         */
        if (boxedType.isEnum()) {
            return convertEnum(value, boxedType);
        }

        /*
         * java.util.Date and java.sql date/time types.
         */
        if (Date.class.isAssignableFrom(boxedType)) {
            return convertDate(value, boxedType, temporalType);
        }

        /*
         * java.time.
         */
        if (boxedType == LocalDate.class) {
            return convertLocalDate(value);
        }

        if (boxedType == LocalDateTime.class) {
            return convertLocalDateTime(value);
        }

        if (boxedType == LocalTime.class) {
            return convertLocalTime(value);
        }

        if (boxedType == Instant.class) {
            return convertInstant(value);
        }

        if (boxedType == OffsetDateTime.class) {
            return convertOffsetDateTime(value);
        }

        if (boxedType == ZonedDateTime.class) {
            return convertZonedDateTime(value);
        }

        /*
         * No conversion known.
         *
         * This deliberately returns the original value rather than
         * performing an unsafe reflective conversion.
         */
        return value;
    }

    /**
     * Finds the JPA @Temporal mapping of an attribute.
     *
     * JPA's metamodel does not expose TemporalType directly, so the
     * annotation has to be obtained from the mapped field or getter.
     *
     * Both field access and property access are supported.
     */
    private static TemporalType findTemporalType(Attribute<?, ?> attribute) {
        Class<?> declaringType = attribute.getDeclaringType().getJavaType();
        String propertyName = attribute.getName();

        /*
         * Field access.
         */
        Field field = findField(declaringType, propertyName);
        if (field != null) {
            Temporal temporal = field.getAnnotation(Temporal.class);
            if (temporal != null) {
                return temporal.value();
            }
        }

        /*
         * Property access.
         */
        Method getter = findGetter(declaringType, propertyName);
        if (getter != null) {
            Temporal temporal = getter.getAnnotation(Temporal.class);
            if (temporal != null) {
                return temporal.value();
            }
        }

        return null;
    }

    /**
     * Finds a field in the class hierarchy.
     */
    private static Field findField(Class<?> type, String propertyName) {
        Class<?> current = type;

        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField( propertyName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }

    /**
     * Finds a JavaBean getter in the class hierarchy.
     *
     * Supports:
     *
     *     getCreatedAt()
     *     isActive()
     */
    private static Method findGetter(Class<?> type, String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return null;
        }

        String suffix = Character.toUpperCase(propertyName.charAt(0))
                + propertyName.substring(1);
        String getterName = "get" + suffix;
        String booleanGetterName = "is" + suffix;
        Class<?> current = type;

        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(getterName);
            } catch (NoSuchMethodException ignored) {
                // Try boolean getter below.
            }

            try {
                return current.getDeclaredMethod(booleanGetterName);
            } catch (NoSuchMethodException ignored) {
                // Continue with superclass.
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Object convertCharacter(Object value) {
        if (value instanceof Character) {
            return value;
        }

        String text = value.toString();
        if (text.length() != 1) {
            throw new IllegalArgumentException(
                    "Cannot convert '" + text + "' to Character");
        }

        return text.charAt(0);
    }

    private static Boolean convertBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        String text = value.toString();

        if ("true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }

        if ("false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }

        throw new IllegalArgumentException(
                "Cannot convert '" + text + "' to Boolean");
    }

    private static Object convertNumber(
            Object value,
            Class<?> targetType) {

        String text = value.toString();

        try {
            if (targetType == Byte.class) {
                return Byte.valueOf(text);
            }

            if (targetType == Short.class) {
                return Short.valueOf(text);
            }

            if (targetType == Integer.class) {
                return Integer.valueOf(text);
            }

            if (targetType == Long.class) {
                return Long.valueOf(text);
            }

            if (targetType == Float.class) {
                return Float.valueOf(text);
            }

            if (targetType == Double.class) {
                return Double.valueOf(text);
            }

            if (targetType == BigInteger.class) {
                return new BigInteger(text);
            }

            if (targetType == BigDecimal.class) {
                return new BigDecimal(text);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + text
                    + "' to "
                    + targetType.getName(),
                    e);
        }

        throw new IllegalArgumentException(
                "Unsupported numeric type: " + targetType.getName());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object convertEnum(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) {
            return value;
        }

        String text = value.toString();

        try {
            return Enum.valueOf((Class<? extends Enum>) targetType, text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown enum value '"
                    + text
                    + "' for "
                    + targetType.getName(),
                    e);
        }
    }

    private static Object convertDate(
            Object value,
            Class<?> targetType,
            TemporalType temporalType) {

        /*
         * java.sql.Date
         */
        if (targetType == java.sql.Date.class) {
            return convertSqlDate(value);
        }

        /*
         * java.sql.Time
         */
        if (targetType == Time.class) {
            return convertSqlTime(value);
        }

        /*
         * java.sql.Timestamp
         */
        if (targetType == Timestamp.class) {
            return convertTimestamp(value);
        }

        /*
         * java.util.Date.
         *
         * @Temporal tells us which semantic representation the
         * application expects.
         */
        if (targetType == Date.class) {
            if (temporalType == TemporalType.DATE) {
                return convertUtilDate(value);
            }

            if (temporalType == TemporalType.TIME) {
                return convertUtilDateTime(value);
            }

            if (temporalType == TemporalType.TIMESTAMP) {
                return convertUtilDateTime(value);
            }

            /*
             * No @Temporal information.
             *
             * Try ISO instant first, then ISO local date.
             */
            return convertUtilDate(value);
        }

        /*
         * Subclasses of java.util.Date.
         */
        return convertUtilDate(value);
    }

    private static java.sql.Date convertSqlDate(Object value) {
        if (value instanceof java.sql.Date) {
            return (java.sql.Date) value;
        }

        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) value);
        }

        try {
            return java.sql.Date.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to java.sql.Date",
                    e);
        }
    }

    private static Time convertSqlTime(Object value) {
        if (value instanceof Time) {
            return (Time) value;
        }

        if (value instanceof LocalTime) {
            return Time.valueOf((LocalTime) value);
        }

        try {
            return Time.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to java.sql.Time",
                    e);
        }
    }

    private static Timestamp convertTimestamp(Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }

        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }

        if (value instanceof Instant) {
            return Timestamp.from((Instant) value);
        }

        String text = value.toString();

        /*
         * ISO-8601 timestamp.
         */
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text));
        } catch (RuntimeException ignored) {
            // Try a JDBC timestamp below.
        }

        try {
            return Timestamp.valueOf(text.replace('T', ' '));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + text
                    + "' to java.sql.Timestamp",
                    e);
        }
    }

    private static Date convertUtilDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof LocalDate) {
            return java.sql.Date.valueOf((LocalDate) value);
        }

        if (value instanceof Instant) {
            return Date.from((Instant) value);
        }

        String text = value.toString();

        /*
         * ISO instant.
         */
        try {
            return Date.from(Instant.parse(text));
        } catch (RuntimeException ignored) {
            // Try local date below.
        }

        /*
         * ISO local date.
         */
        try {
            return java.sql.Date.valueOf(LocalDate.parse(text));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + text
                    + "' to java.util.Date",
                    e);
        }
    }

    private static Date convertUtilDateTime(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof Instant) {
            return Date.from((Instant) value);
        }

        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant());
        }

        String text = value.toString();

        /*
         * ISO instant.
         */
        try {
            return Date.from(Instant.parse(text));
        } catch (RuntimeException ignored) {
            // Try LocalDateTime below.
        }

        try {
            return Date.from(LocalDateTime.parse(text)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + text
                    + "' to java.util.Date",
                    e);
        }
    }

    private static LocalDate convertLocalDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }

        if (value instanceof Date) {
            return ((Date) value)
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        try {
            return LocalDate.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to LocalDate",
                    e);
        }
    }

    private static LocalDateTime convertLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }

        if (value instanceof Date) {
            return LocalDateTime.ofInstant(
                    ((Date) value).toInstant(),
                    java.time.ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to LocalDateTime",
                    e);
        }
    }

    private static LocalTime convertLocalTime(Object value) {
        if (value instanceof LocalTime) {
            return (LocalTime) value;
        }

        try {
            return LocalTime.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to LocalTime",
                    e);
        }
    }

    private static Instant convertInstant(Object value) {
        if (value instanceof Instant) {
            return (Instant) value;
        }

        if (value instanceof Date) {
            return ((Date) value).toInstant();
        }

        try {
            return Instant.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Cannot convert '"
                    + value
                    + "' to Instant",
                    e);
        }
    }

    private static OffsetDateTime convertOffsetDateTime(Object value) {
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }

        try {
            return OffsetDateTime.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to OffsetDateTime",
                    e);
        }
    }

    private static ZonedDateTime convertZonedDateTime(Object value) {
        if (value instanceof ZonedDateTime) {
            return (ZonedDateTime) value;
        }

        try {
            return ZonedDateTime.parse(value.toString());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '"
                    + value
                    + "' to ZonedDateTime",
                    e);
        }
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

        if (type == byte.class) {
            return Byte.class;
        }

        if (type == short.class) {
            return Short.class;
        }

        if (type == int.class) {
            return Integer.class;
        }

        if (type == long.class) {
            return Long.class;
        }

        if (type == float.class) {
            return Float.class;
        }

        if (type == double.class) {
            return Double.class;
        }

        if (type == boolean.class) {
            return Boolean.class;
        }

        if (type == char.class) {
            return Character.class;
        }

        return type;
    }
}
