package org.addy.persistence.criteria.parser;

import org.addy.persistence.criteria.expression.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class ExpressionParser {
    private final List<Token> tokens;
    private int position;

    private ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Expression parse(String expression) {
        ExpressionParser parser =
            new ExpressionParser(new ExpressionLexer(expression).tokenize());

        Expression result = parser.parseOr();
        parser.expect(TokenType.EOF);
        return result;
    }

    private Expression parseOr() {
        Expression expression = parseAnd();

        while (match(TokenType.OR)) {
            expression = new LogicalExpression(
                expression,
                LogicalExpression.Operator.OR,
                parseAnd());
        }

        return expression;
    }

    private Expression parseAnd() {
        Expression expression = parseNot();

        while (match(TokenType.AND)) {
            expression = new LogicalExpression(
                expression,
                LogicalExpression.Operator.AND,
                parseNot());
        }

        return expression;
    }

    private Expression parseNot() {
        if (match(TokenType.NOT)) {
            return new NotExpression(parseNot());
        }
        return parsePrimary();
    }

    private Expression parsePrimary() {
        if (match(TokenType.LPAREN)) {
            Expression expression = parseOr();
            expect(TokenType.RPAREN);
            return expression;
        }
        return parseComparison();
    }

    private Expression parseComparison() {
        PropertyExpression property = parseProperty();

        if (match(TokenType.IS)) {
            boolean not = match(TokenType.NOT);
            expect(TokenType.NULL);
            return new ComparisonExpression(property,
                not ? ComparisonExpression.Operator.IS_NOT_NULL
                    : ComparisonExpression.Operator.IS_NULL,
                null);
        }

        boolean not = match(TokenType.NOT);

        if (match(TokenType.LIKE)) {
            return new ComparisonExpression(property,
                not ? ComparisonExpression.Operator.NOT_LIKE
                    : ComparisonExpression.Operator.LIKE,
                parseValue());
        }

        if (match(TokenType.IN)) {
            return new ComparisonExpression(property,
                not ? ComparisonExpression.Operator.NOT_IN
                    : ComparisonExpression.Operator.IN,
                parseList());
        }

        if (match(TokenType.BETWEEN)) {
            Expression lower = parseValue();
            expect(TokenType.AND);
            Expression upper = parseValue();

            return new ComparisonExpression(property,
                not ? ComparisonExpression.Operator.NOT_BETWEEN
                    : ComparisonExpression.Operator.BETWEEN,
                lower, upper);
        }

        if (not) {
            throw error("Expected LIKE, IN or BETWEEN after NOT");
        }

        Token operator = consume();
        ComparisonExpression.Operator op;

        switch (operator.getType()) {
            case EQ: op = ComparisonExpression.Operator.EQ; break;
            case NE: op = ComparisonExpression.Operator.NE; break;
            case LT: op = ComparisonExpression.Operator.LT; break;
            case LE: op = ComparisonExpression.Operator.LE; break;
            case GT: op = ComparisonExpression.Operator.GT; break;
            case GE: op = ComparisonExpression.Operator.GE; break;
            default:
                throw error("Expected comparison operator, found "
                    + operator.getText());
        }

        return new ComparisonExpression(property, op, parseValue());
    }

    private PropertyExpression parseProperty() {
        Token token = expect(TokenType.IDENTIFIER);
        return new PropertyExpression(token.getText());
    }

    private Expression parseValue() {
        Token token = peek();

        switch (token.getType()) {
            case STRING:
                consume();
                return new LiteralExpression(token.getText());

            case NUMBER:
                consume();
                return new LiteralExpression(parseNumber(token.getText()));

            case TRUE:
                consume();
                return new LiteralExpression(Boolean.TRUE);

            case FALSE:
                consume();
                return new LiteralExpression(Boolean.FALSE);

            case NULL:
                consume();
                return new LiteralExpression(null);

            case PARAMETER:
                consume();
                return new ParameterExpression(token.getText());

            default:
                throw error("Expected value, found " + token.getText());
        }
    }

    private ListExpression parseList() {
        expect(TokenType.LPAREN);
        List<Expression> values = new ArrayList<>();

        if (!check(TokenType.RPAREN)) {
            do {
                values.add(parseValue());
            } while (match(TokenType.COMMA));
        }

        expect(TokenType.RPAREN);
        return new ListExpression(values);
    }

    private Number parseNumber(String text) {
        if (text.indexOf('.') >= 0) {
            return new BigDecimal(text);
        }

        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ignored) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored2) {
                return new BigInteger(text);
            }
        }
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            consume();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private Token expect(TokenType type) {
        if (!check(type)) {
            throw error("Expected " + type + ", found " + peek());
        }
        return consume();
    }

    private Token consume() {
        return tokens.get(position++);
    }

    private Token peek() {
        return tokens.get(position);
    }

    private ExpressionParseException error(String message) {
        return new ExpressionParseException(message, peek().getPosition());
    }
}
