package org.addy.persistence.criteria;

public final class ExpressionPrinter {
    private ExpressionPrinter() {}

    public static String print(Expression e) {
        if (e instanceof PropertyExpression) {
            return ((PropertyExpression) e).getPath();
        }
        if (e instanceof LiteralExpression) {
            Object v = ((LiteralExpression) e).getValue();
            return v == null ? "NULL" : String.valueOf(v);
        }
        if (e instanceof ParameterExpression) {
            return ":" + ((ParameterExpression) e).getName();
        }
        if (e instanceof ListExpression) {
            StringBuilder b = new StringBuilder("(");
            boolean first = true;
            for (Expression x : ((ListExpression)e).getValues()) {
                if (!first) b.append(", ");
                b.append(print(x));
                first = false;
            }
            return b.append(')').toString();
        }
        if (e instanceof ComparisonExpression) {
            ComparisonExpression c = (ComparisonExpression)e;
            switch (c.getOperator()) {
                case IS_NULL: return print(c.getProperty()) + " IS NULL";
                case IS_NOT_NULL: return print(c.getProperty()) + " IS NOT NULL";
                case BETWEEN:
                case NOT_BETWEEN:
                    return print(c.getProperty()) + " "
                        + (c.getOperator() == ComparisonExpression.Operator.NOT_BETWEEN
                            ? "NOT BETWEEN " : "BETWEEN ")
                        + print(c.getValue()) + " AND " + print(c.getSecondValue());
                default:
                    return print(c.getProperty()) + " "
                        + c.getOperator() + " " + print(c.getValue());
            }
        }
        if (e instanceof NotExpression) {
            return "NOT (" + print(((NotExpression)e).getExpression()) + ")";
        }
        if (e instanceof LogicalExpression) {
            LogicalExpression l = (LogicalExpression)e;
            return "(" + print(l.getLeft()) + " "
                + l.getOperator() + " " + print(l.getRight()) + ")";
        }
        throw new IllegalArgumentException("Unknown AST node");
    }
}
