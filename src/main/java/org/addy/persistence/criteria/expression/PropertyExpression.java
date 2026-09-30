package org.addy.persistence.criteria.expression;

public final class PropertyExpression implements Expression {
    private final String path;

    public PropertyExpression(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
