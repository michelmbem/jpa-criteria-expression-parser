package org.addy.persistence.criteria;

public final class ParameterExpression implements Expression {
    private final String name;

    public ParameterExpression(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
