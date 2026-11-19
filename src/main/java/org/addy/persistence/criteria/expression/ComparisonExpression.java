package org.addy.persistence.criteria.expression;

public final class ComparisonExpression implements Expression {
    private final PropertyExpression property;
    private final Operator operator;
    private final Expression value;
    private final Expression secondValue;

    public ComparisonExpression(PropertyExpression property,
                                Operator operator,
                                Expression value) {
        this(property, operator, value, null);
    }

    public ComparisonExpression(PropertyExpression property,
                                Operator operator,
                                Expression value,
                                Expression secondValue) {
        this.property = property;
        this.operator = operator;
        this.value = value;
        this.secondValue = secondValue;
    }

    public PropertyExpression getProperty() {
        return property;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getValue() {
        return value;
    }

    public Expression getSecondValue() {
        return secondValue;
    }

    public enum Operator {
        EQ, NE, LT, LE, GT, GE,
        STARTS_WITH, ENDS_WITH, CONTAINS,
        LIKE, NOT_LIKE,
        ILIKE, NOT_ILIKE,
        IN, NOT_IN,
        BETWEEN, NOT_BETWEEN,
        IS_NULL, IS_NOT_NULL
    }
}
