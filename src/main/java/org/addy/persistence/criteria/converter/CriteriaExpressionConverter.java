package org.addy.persistence.criteria.converter;

import org.addy.persistence.criteria.expression.*;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import java.util.Collections;
import java.util.Map;

public final class CriteriaExpressionConverter {

    private final CriteriaBuilder criteriaBuilder;
    private final EntityManager entityManager;
    private final JpaPathResolver pathResolver;
    private final Map<String, Object> parameters;

    public CriteriaExpressionConverter(
            CriteriaBuilder criteriaBuilder,
            EntityManager entityManager) {

        this(criteriaBuilder, entityManager, Collections.emptyMap());
    }

    public CriteriaExpressionConverter(
            CriteriaBuilder criteriaBuilder,
            EntityManager entityManager,
            Map<String, Object> parameters) {

        this.criteriaBuilder = criteriaBuilder;
        this.entityManager = entityManager;
        this.pathResolver = new JpaPathResolver(entityManager);
        this.parameters = parameters == null
                ? Collections.emptyMap()
                : parameters;
    }

    public Predicate toPredicate(
            Expression expression,
            From<?, ?> root) {

        if (expression instanceof LogicalExpression) {
            return convertLogical((LogicalExpression) expression,  root);
        }

        if (expression instanceof NotExpression) {
            NotExpression not = (NotExpression) expression;
            return criteriaBuilder.not(toPredicate(not.getExpression(), root));
        }

        if (expression instanceof ComparisonExpression) {
            return convertComparison((ComparisonExpression) expression, root);
        }

        throw new IllegalArgumentException(
                "Unsupported expression node: " + expression.getClass().getName());
    }

    private Predicate convertLogical(
            LogicalExpression expression,
            From<?, ?> root) {

        Predicate left = toPredicate(expression.getLeft(), root);
        Predicate right = toPredicate(expression.getRight(), root);
        return expression.getOperator() == LogicalExpression.Operator.AND
                ? criteriaBuilder.and(left, right)
                : criteriaBuilder.or(left, right);
    }

    private Predicate convertComparison(
            ComparisonExpression expression,
            From<?, ?> root) {

        Path<?> path = pathResolver.resolve(
                root, expression.getProperty().getPath());

        Attribute<?, ?> attribute = resolveAttribute(
                root.getJavaType(), expression.getProperty().getPath());

        switch (expression.getOperator()) {
            case IS_NULL: return criteriaBuilder.isNull(path);
            case IS_NOT_NULL: return criteriaBuilder.isNotNull(path);
            case EQ: return criteriaBuilder.equal(
                    path, convert(expression.getValue(), attribute));
            case NE: return criteriaBuilder.notEqual(
                    path, convert(expression.getValue(), attribute));
            case LT: return compare(
                    path, attribute, expression.getValue(), ComparisonOperator.LT);
            case LE: return compare(
                    path, attribute, expression.getValue(), ComparisonOperator.LE);
            case GT: return compare(
                    path, attribute, expression.getValue(), ComparisonOperator.GT);
            case GE: return compare(
                    path, attribute, expression.getValue(), ComparisonOperator.GE);
            case LIKE: return criteriaBuilder.like(
                    stringExpression(path),
                    String.valueOf(convert(expression.getValue(), attribute)));
            case NOT_LIKE: return criteriaBuilder.not(criteriaBuilder.like(
                    stringExpression(path),
                    String.valueOf(convert(expression.getValue(), attribute))));
            case IN: return createIn(
                    path, attribute, (ListExpression) expression.getValue(), false);
            case NOT_IN:return createIn(
                    path, attribute, (ListExpression) expression.getValue(), true);
            case BETWEEN: return createBetween(path, attribute, expression, false);
            case NOT_BETWEEN: return createBetween(path, attribute, expression, true);
            default: throw new IllegalStateException(
                    "Unsupported operator: " + expression.getOperator());
        }
    }

    /*
     * The generic type is deliberately resolved from the Path's Java type.
     *
     * CriteriaBuilder declares:
     *
     * <Y extends Comparable<? super Y>>
     * Predicate lessThan(Expression<? extends Y> x, Y y)
     *
     * Letting Java infer Y through several independent generic helper
     * methods makes the invocation ambiguous with some JDK/compiler
     * combinations. The explicit ComparableExpression<T> path below
     * gives the compiler one concrete type variable.
     */
    private Predicate compare(
            Path<?> path,
            Attribute<?, ?> attribute,
            Expression valueExpression,
            ComparisonOperator operator) {

        Class<?> type = box(path.getJavaType());
        Object value = convert(valueExpression, attribute);

        if (!Comparable.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Property '"
                    + attribute.getName()
                    + "' of type "
                    + type.getName()
                    + " cannot be used with an ordered "
                    + "comparison operator");
        }

        return compareComparable(path, value, operator);
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private Predicate compareComparable(
            Path<?> path,
            Object value,
            ComparisonOperator operator) {

        /*
         * Using raw Comparable here is intentional and localized.
         * It avoids exposing the CriteriaBuilder's recursive generic
         * bound to the rest of the converter.
         */
        javax.persistence.criteria.Expression<? extends Comparable> expression =
                (javax.persistence.criteria.Expression<? extends Comparable>) path;

        Comparable comparableValue = (Comparable) value;

        switch (operator) {
            case LT: return criteriaBuilder.lessThan(expression, comparableValue);
            case LE: return criteriaBuilder.lessThanOrEqualTo(expression, comparableValue);
            case GT: return criteriaBuilder.greaterThan(expression, comparableValue);
            case GE: return criteriaBuilder.greaterThanOrEqualTo(expression, comparableValue);
            default: throw new IllegalArgumentException("Not an ordered comparison: " + operator);
        }
    }

    private Predicate createBetween(
            Path<?> path,
            Attribute<?, ?> attribute,
            ComparisonExpression expression,
            boolean negate) {

        Object lower = convert(expression.getValue(), attribute);
        Object upper = convert(expression.getSecondValue(), attribute);
        Predicate predicate = betweenComparable(path, lower, upper);
        return negate ? criteriaBuilder.not(predicate) : predicate;
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private Predicate betweenComparable(
            Path<?> path,
            Object lower,
            Object upper) {

        javax.persistence.criteria.Expression<? extends Comparable> expression =
                (javax.persistence.criteria.Expression<? extends Comparable>) path;

        return criteriaBuilder.between(expression, (Comparable) lower, (Comparable) upper);
    }

    private Predicate createIn(
            Path<?> path,
            Attribute<?, ?> attribute,
            ListExpression list,
            boolean negate) {

        CriteriaBuilder.In<Object> in = criteriaBuilder.in(path);

        for (Expression expression : list.getValues()) {
            in.value(convert(expression, attribute));
        }

        return negate ? criteriaBuilder.not(in) : in;
    }

    private Object convert(
            Expression expression,
            Attribute<?, ?> attribute) {

        Object raw;

        if (expression instanceof LiteralExpression) {
            raw = ((LiteralExpression) expression).getValue();
        } else if (expression instanceof ParameterExpression) {
            String name = ((ParameterExpression) expression).getName();
            if (!parameters.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Missing expression parameter :" + name);
            }
            raw = parameters.get(name);
        } else {
            throw new IllegalArgumentException("Expected literal or parameter");
        }

        return ValueConverter.convertForAttribute(raw, attribute);
    }

    private Attribute<?, ?> resolveAttribute(
            Class<?> rootType,
            String pathExpression) {

        String[] parts = pathExpression.split("\\.");
        Class<?> currentType = rootType;
        Attribute<?, ?> attribute = null;

        for (int i = 0; i < parts.length; i++) {
            ManagedType<?> managedType = entityManager
                    .getMetamodel()
                    .managedType(currentType);

            try {
                attribute = managedType.getAttribute(parts[i]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown property '"
                        + parts[i]
                        + "' in path '"
                        + pathExpression
                        + "'",
                        e);
            }

            if (i < parts.length - 1) {
                currentType = attribute.getJavaType();
            }
        }

        return attribute;
    }

    @SuppressWarnings("unchecked")
    private javax.persistence.criteria.Expression<String>
    stringExpression(Path<?> path) {
        return (javax.persistence.criteria.Expression<String>) path;
    }

    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }

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

    private enum ComparisonOperator {
        LT,
        LE,
        GT,
        GE
    }
}
