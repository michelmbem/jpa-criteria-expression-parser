package org.addy.persistence.criteria.converter;

import javax.persistence.EntityManager;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.metamodel.Attribute;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

public final class ValueConverter {
    private final EntityManager entityManager;

    public ValueConverter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Object convert(Object value, Class<?> targetType, TemporalType temporalType) {
        if (value == null) {
            return null;
        }

        Class<?> boxed = box(targetType);

        if (boxed.isInstance(value)) {
            return value;
        }

        if (boxed == String.class) {
            return String.valueOf(value);
        }

        if (boxed == Boolean.class) {
            return Boolean.valueOf(value.toString());
        }

        if (Number.class.isAssignableFrom(boxed)) {
            return convertNumber(value.toString(), boxed);
        }

        if (boxed.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Class<? extends Enum> enumType = (Class<? extends Enum>) boxed;
            return Enum.valueOf(enumType, value.toString());
        }

        if (Date.class.isAssignableFrom(boxed)) {
            return convertDate(value.toString(), boxed, temporalType);
        }

        if (boxed == LocalDate.class) return LocalDate.parse(value.toString());
        if (boxed == LocalDateTime.class) return LocalDateTime.parse(value.toString());
        if (boxed == LocalTime.class) return LocalTime.parse(value.toString());
        if (boxed == Instant.class) return Instant.parse(value.toString());
        if (boxed == OffsetDateTime.class) return OffsetDateTime.parse(value.toString());
        if (boxed == ZonedDateTime.class) return ZonedDateTime.parse(value.toString());

        return value;
    }

    public TemporalType temporalTypeFor(String pathExpression) {
        String[] parts = pathExpression.split("\\.");
        Class<?> currentType = null;
        Attribute<?, ?> attribute = null;

        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                throw new IllegalStateException(
                    "temporalTypeFor requires path metadata resolution");
            }
        }

        return null;
    }

    public Object convertForAttribute(Object value, Attribute<?, ?> attribute) {
        TemporalType temporal = null;
        try {
            Class<?> declaring = attribute.getDeclaringType().getJavaType();
            java.lang.reflect.Field field = findField(declaring, attribute.getName());
            if (field != null) {
                Temporal annotation = field.getAnnotation(Temporal.class);
                if (annotation != null) {
                    temporal = annotation.value();
                }
            }
        } catch (Exception ignored) {
            // Accessor-based mappings can be handled by the fallback conversion.
        }

        return convert(value, attribute.getJavaType(), temporal);
    }

    private java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(name);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object convertDate(String text, Class<?> targetType, TemporalType temporal) {
        if (temporal == TemporalType.DATE || targetType == java.sql.Date.class) {
            return java.sql.Date.valueOf(LocalDate.parse(text));
        }

        if (temporal == TemporalType.TIME || targetType == java.sql.Time.class) {
            return java.sql.Time.valueOf(LocalTime.parse(text));
        }

        if (temporal == TemporalType.TIMESTAMP || targetType == Timestamp.class) {
            String normalized = text.endsWith("Z")
                ? text.substring(0, text.length() - 1)
                : text;
            return Timestamp.valueOf(normalized.replace('T', ' '));
        }

        try {
            return Date.from(Instant.parse(text));
        } catch (RuntimeException ignored) {
            try {
                return java.sql.Date.valueOf(LocalDate.parse(text));
            } catch (RuntimeException ignored2) {
                throw new IllegalArgumentException(
                    "Cannot convert '" + text + "' to " + targetType.getName());
            }
        }
    }

    private Object convertNumber(String text, Class<?> targetType) {
        if (targetType == Byte.class) return Byte.valueOf(text);
        if (targetType == Short.class) return Short.valueOf(text);
        if (targetType == Integer.class) return Integer.valueOf(text);
        if (targetType == Long.class) return Long.valueOf(text);
        if (targetType == Float.class) return Float.valueOf(text);
        if (targetType == Double.class) return Double.valueOf(text);
        if (targetType == BigInteger.class) return new BigInteger(text);
        if (targetType == BigDecimal.class) return new BigDecimal(text);
        return text;
    }

    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
