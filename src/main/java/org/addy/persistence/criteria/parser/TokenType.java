package org.addy.persistence.criteria.parser;

public enum TokenType {
    IDENTIFIER,
    PARAMETER,
    STRING,
    NUMBER,

    TRUE,
    FALSE,
    NULL,

    AND,
    OR,
    NOT,
    LIKE,
    IN,
    BETWEEN,
    IS,

    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,

    LPAREN,
    RPAREN,
    COMMA,

    EOF
}
