package org.addy.persistence.criteria;

public final class LiteralExpression implements Expression {
    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
