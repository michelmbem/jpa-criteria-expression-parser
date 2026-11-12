package org.addy.persistence.criteria.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ExpressionLexer {
    private final String input;
    private final int inputLength;
    private int position;

    public ExpressionLexer(String expression) {
        input = Objects.requireNonNull(expression, "The expression cannot be null");
        inputLength = input.length();
        position = 0;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < inputLength) {
            char c = input.charAt(position);

            switch (c) {
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", position++));
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", position++));
                    break;
                case ',':
                    tokens.add(new Token(TokenType.COMMA, ",", position++));
                    break;
                case '=':
                    tokens.add(new Token(TokenType.EQ, "=", position++));
                    break;
                case '!':
                    if (peek('=')) {
                        tokens.add(new Token(TokenType.NE, "!=", position));
                        position += 2;
                        break;
                    }
                    throw error("Expected '=' after '!'");
                case '<':
                    if (peek('=')) {
                        tokens.add(new Token(TokenType.LE, "<=", position));
                        position += 2;
                    } else if (peek('>')) {
                        tokens.add(new Token(TokenType.NE, "<>", position));
                        position += 2;
                    } else {
                        tokens.add(new Token(TokenType.LT, "<", position++));
                    }
                    break;
                case '>':
                    if (peek('=')) {
                        tokens.add(new Token(TokenType.GE, ">=", position));
                        position += 2;
                    } else {
                        tokens.add(new Token(TokenType.GT, ">", position++));
                    }
                    break;
                case '?':
                    tokens.add(positionalParameter());
                    break;
                case ':':
                    tokens.add(namedParameter());
                    break;
                case '\'':
                    tokens.add(literalString());
                    break;
                default:
                    if (Character.isWhitespace(c)) {
                        ++position;
                    } else if (Character.isDigit(c) || (c == '-' && position + 1 < inputLength
                            && Character.isDigit(input.charAt(position + 1)))) {
                        tokens.add(literalNumber());
                    } else if (Character.isJavaIdentifierStart(c)) {
                        tokens.add(identifier());
                    } else {
                        throw error("Unexpected character '" + c + "'");
                    }
                    break;
            }
        }

        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }

    private boolean peek(char expected) {
        int nextPosition = position + 1;
        return nextPosition < inputLength && input.charAt(nextPosition) == expected;
    }

    private Token positionalParameter() {
        int start = position++; // skip the initial '?'

        if (position >= inputLength || !Character.isDigit(input.charAt(position)))
            throw error("Expected parameter index after '?'");

        int indexStart = position++;
        while (position < inputLength && Character.isDigit(input.charAt(position)))
            ++position;

        return new Token(TokenType.PARAMETER, input.substring(indexStart, position), start);
    }

    private Token namedParameter() {
        int start = position++; // skip the initial ':'
        
        if (position >= inputLength || !Character.isJavaIdentifierStart(input.charAt(position)))
            throw error("Expected parameter name after ':'");

        int nameStart = position++;
        while (position < inputLength && Character.isJavaIdentifierPart(input.charAt(position)))
            ++position;

        return new Token(TokenType.PARAMETER, input.substring(nameStart, position), start);
    }

    private Token literalString() {
        int start = position++; // skip the initial "'"
        StringBuilder value = new StringBuilder();

        while (position < inputLength) {
            char c = input.charAt(position++);

            if (c == '\'') {
                if (position < inputLength && input.charAt(position) == '\'') {
                    value.append('\'');
                    ++position;
                    continue;
                }
                return new Token(TokenType.STRING, value.toString(), start);
            }

            value.append(c);
        }

        throw error("Unterminated string literal");
    }

    private Token literalNumber() {
        int start = position;

        if (input.charAt(position) == '-') ++position;

        while (position < inputLength && Character.isDigit(input.charAt(position)))
            ++position;

        if (position < inputLength && input.charAt(position) == '.') {
            ++position;

            if (position >= inputLength || !Character.isDigit(input.charAt(position)))
                throw error("Expected digits after decimal point");

            while (position < inputLength && Character.isDigit(input.charAt(position)))
                ++position;
        }

        if (position < inputLength && Character.toLowerCase(input.charAt(position)) == 'e') {
            ++position;

            if (position < inputLength &&
                    (input.charAt(position) == '-' || input.charAt(position) == '+'))
                ++position;

            if (position >= inputLength || !Character.isDigit(input.charAt(position)))
                throw error("Expected digits after exponent sign");

            while (position < inputLength && Character.isDigit(input.charAt(position)))
                ++position;
        }

        return new Token(TokenType.NUMBER, input.substring(start, position), start);
    }

    private Token identifier() {
        int start = position;

        while (position < inputLength) {
            char c = input.charAt(position);
            if (Character.isJavaIdentifierPart(c) || c == '.')
                ++position;
            else
                break;
        }

        String text = input.substring(start, position);
        return new Token(keyword(text), text, start);
    }

    private TokenType keyword(String text) {
        switch (text.toUpperCase(Locale.ROOT)) {
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
