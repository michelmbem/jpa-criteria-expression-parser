package org.addy.persistence.criteria.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ExpressionLexer {
    private final String input;
    private int position;

    public ExpressionLexer(String input) {
        this.input = Objects.requireNonNull(input, "input cannot be null");
    }

    public List<Token> tokenize() {
        List<Token> result = new ArrayList<>();
        int limit = input.length();

        while (position < limit) {
            char c = input.charAt(position);
            
            switch (c) {
                case '(':
                    result.add(new Token(TokenType.LPAREN, "(", position++));
                    break;
                case ')':
                    result.add(new Token(TokenType.RPAREN, ")", position++));
                    break;
                case ',':
                    result.add(new Token(TokenType.COMMA, ",", position++));
                    break;
                case '=':
                    result.add(new Token(TokenType.EQ, "=", position++));
                    break;
                case '!':
                    if (peek('=', limit)) {
                        result.add(new Token(TokenType.NE, "!=", position));
                        position += 2;
                        break;
                    }
                    throw error("Expected '=' after '!'");
                case '<':
                    if (peek('=', limit)) {
                        result.add(new Token(TokenType.LE, "<=", position));
                        position += 2;
                    } else if (peek('>', limit)) {
                        result.add(new Token(TokenType.NE, "<>", position));
                        position += 2;
                    } else {
                        result.add(new Token(TokenType.LT, "<", position++));
                    }
                    break;
                case '>':
                    if (peek('=', limit)) {
                        result.add(new Token(TokenType.GE, ">=", position));
                        position += 2;
                    } else {
                        result.add(new Token(TokenType.GT, ">", position++));
                    }
                    break;
                case '?':
                    result.add(positionalParameter(limit));
                    break;
                case ':':
                    result.add(namedParameter(limit));
                    break;
                case '\'':
                    result.add(literalString(limit));
                    break;
                default:
                    if (Character.isWhitespace(c)) {
                        ++position;
                    } else if (Character.isDigit(c) || (c == '-' && position + 1 < limit
                            && Character.isDigit(input.charAt(position + 1)))) {
                        result.add(literalNumber(limit));
                    } else if (Character.isJavaIdentifierStart(c)) {
                        result.add(identifier(limit));
                    } else {
                        throw error("Unexpected character '" + c + "'");
                    }
                    break;
            }
        }

        result.add(new Token(TokenType.EOF, "", position));
        return result;
    }

    private boolean peek(char expected, int limit) {
        int nextPosition = position + 1;
        return nextPosition < limit && input.charAt(nextPosition) == expected;
    }

    private Token positionalParameter(int limit) {
        int start = position++; // skip the initial '?'

        if (position >= limit || !Character.isDigit(input.charAt(position)))
            throw error("Expected parameter number after '?'");

        int indexStart = position++;
        while (position < limit && Character.isDigit(input.charAt(position)))
            ++position;

        return new Token(TokenType.PARAMETER, input.substring(indexStart, position), start);
    }

    private Token namedParameter(int limit) {
        int start = position++; // skip the initial ':'
        
        if (position >= limit || !Character.isJavaIdentifierStart(input.charAt(position)))
            throw error("Expected parameter name after ':'");

        int nameStart = position++;
        while (position < limit && Character.isJavaIdentifierPart(input.charAt(position)))
            ++position;

        return new Token(TokenType.PARAMETER, input.substring(nameStart, position), start);
    }

    private Token literalString(int limit) {
        int start = position++; // skip the initial "'"
        StringBuilder value = new StringBuilder();

        while (position < limit) {
            char c = input.charAt(position++);

            if (c == '\'') {
                if (position < limit && input.charAt(position) == '\'') {
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

    private Token literalNumber(int limit) {
        int start = position;

        if (input.charAt(position) == '-') ++position;

        while (position < limit && Character.isDigit(input.charAt(position)))
            ++position;

        if (position < limit && input.charAt(position) == '.') {
            ++position;

            if (position >= limit || !Character.isDigit(input.charAt(position)))
                throw error("Expected digits after decimal point");

            while (position < limit && Character.isDigit(input.charAt(position)))
                ++position;
        }

        return new Token(TokenType.NUMBER, input.substring(start, position), start);
    }

    private Token identifier(int limit) {
        int start = position;

        while (position < limit) {
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
