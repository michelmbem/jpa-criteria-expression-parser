package org.addy.persistence.criteria;

public final class LogicalExpression implements Expression {
    public enum Operator { AND, OR }

    private final Expression left;
    private final Expression right;
    private final Operator operator;

    public LogicalExpression(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public Expression getLeft() { return left; }
    public Expression getRight() { return right; }
    public Operator getOperator() { return operator; }
}
