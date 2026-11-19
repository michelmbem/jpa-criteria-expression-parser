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
    ILIKE,
    IN,
    BETWEEN,
    IS,

    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,
    STARTS_WITH,
    ENDS_WITH,
    CONTAINS,

    LPAREN,
    RPAREN,
    COMMA,

    EOF
}
