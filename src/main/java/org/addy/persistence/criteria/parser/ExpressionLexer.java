package org.addy.persistence.criteria.parser;

import java.util.ArrayList;
import java.util.List;

public final class ExpressionLexer {
    private final String input;
    private int position;

    public ExpressionLexer(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> result = new ArrayList<>();

        while (position < input.length()) {
            char c = input.charAt(position);

            if (Character.isWhitespace(c)) {
                position++;
                continue;
            }

            int start = position;

            switch (c) {
                case '(':
                    result.add(new Token(TokenType.LPAREN, "(", start));
                    position++;
                    continue;
                case ')':
                    result.add(new Token(TokenType.RPAREN, ")", start));
                    position++;
                    continue;
                case ',':
                    result.add(new Token(TokenType.COMMA, ",", start));
                    position++;
                    continue;
                case '=':
                    result.add(new Token(TokenType.EQ, "=", start));
                    position++;
                    continue;
                case '!':
                    if (peek('=')) {
                        result.add(new Token(TokenType.NE, "!=", start));
                        position += 2;
                        continue;
                    }
                    throw error("Expected '=' after '!'");
                case '<':
                    if (peek('=')) {
                        result.add(new Token(TokenType.LE, "<=", start));
                        position += 2;
                    } else if (peek('>')) {
                        result.add(new Token(TokenType.NE, "<>", start));
                        position += 2;
                    } else {
                        result.add(new Token(TokenType.LT, "<", start));
                        position++;
                    }
                    continue;
                case '>':
                    if (peek('=')) {
                        result.add(new Token(TokenType.GE, ">=", start));
                        position += 2;
                    } else {
                        result.add(new Token(TokenType.GT, ">", start));
                        position++;
                    }
                    continue;
                case ':':
                    result.add(readParameter());
                    continue;
                case '\'':
                    result.add(readString());
                    continue;
                default:
                    break;
            }

            if (Character.isDigit(c) ||
                (c == '-' && position + 1 < input.length()
                    && Character.isDigit(input.charAt(position + 1)))) {
                result.add(readNumber());
                continue;
            }

            if (Character.isJavaIdentifierStart(c)) {
                result.add(readIdentifier());
                continue;
            }

            throw error("Unexpected character '" + c + "'");
        }

        result.add(new Token(TokenType.EOF, "", position));
        return result;
    }

    private boolean peek(char expected) {
        return position + 1 < input.length()
            && input.charAt(position + 1) == expected;
    }

    private Token readParameter() {
        int start = position++;
        if (position >= input.length()
            || !Character.isJavaIdentifierStart(input.charAt(position))) {
            throw error("Expected parameter name after ':'");
        }

        int nameStart = position++;
        while (position < input.length()
            && Character.isJavaIdentifierPart(input.charAt(position))) {
            position++;
        }

        return new Token(TokenType.PARAMETER,
            input.substring(nameStart, position), start);
    }

    private Token readString() {
        int start = position++;
        StringBuilder value = new StringBuilder();

        while (position < input.length()) {
            char c = input.charAt(position++);

            if (c == '\'') {
                if (position < input.length()
                    && input.charAt(position) == '\'') {
                    value.append('\'');
                    position++;
                    continue;
                }
                return new Token(TokenType.STRING, value.toString(), start);
            }

            value.append(c);
        }

        throw error("Unterminated string literal");
    }

    private Token readNumber() {
        int start = position;

        if (input.charAt(position) == '-') {
            position++;
        }

        while (position < input.length()
            && Character.isDigit(input.charAt(position))) {
            position++;
        }

        if (position < input.length() && input.charAt(position) == '.') {
            position++;
            if (position >= input.length()
                || !Character.isDigit(input.charAt(position))) {
                throw error("Expected digits after decimal point");
            }
            while (position < input.length()
                && Character.isDigit(input.charAt(position))) {
                position++;
            }
        }

        return new Token(TokenType.NUMBER,
            input.substring(start, position), start);
    }

    private Token readIdentifier() {
        int start = position;

        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                position++;
            } else {
                break;
            }
        }

        String text = input.substring(start, position);
        return new Token(keyword(text), text, start);
    }

    private TokenType keyword(String text) {
        switch (text.toUpperCase()) {
            case "AND": return TokenType.AND;
            case "OR": return TokenType.OR;
            case "NOT": return TokenType.NOT;
            case "LIKE": return TokenType.LIKE;
            case "IN": return TokenType.IN;
            case "BETWEEN": return TokenType.BETWEEN;
            case "IS": return TokenType.IS;
            case "TRUE": return TokenType.TRUE;
            case "FALSE": return TokenType.FALSE;
            case "NULL": return TokenType.NULL;
            default: return TokenType.IDENTIFIER;
        }
    }

    private ExpressionParseException error(String message) {
        return new ExpressionParseException(message, position);
    }
}
