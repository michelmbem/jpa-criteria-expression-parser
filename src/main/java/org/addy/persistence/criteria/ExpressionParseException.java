package org.addy.persistence.criteria;

public class ExpressionParseException extends IllegalArgumentException {
    private final int position;

    public ExpressionParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
