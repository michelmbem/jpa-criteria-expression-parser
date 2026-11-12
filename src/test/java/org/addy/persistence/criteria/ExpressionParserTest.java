package org.addy.persistence.criteria;

import org.addy.persistence.criteria.expression.*;
import org.addy.persistence.criteria.parser.ExpressionParseException;
import org.addy.persistence.criteria.parser.ExpressionParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ExpressionParserTest {

    @Test
    void parsesBetweenAndLogicalAnd() {
        Expression e = ExpressionParser.parse("age BETWEEN 18 AND 65 AND active = true");

        assertTrue(e instanceof LogicalExpression);

        LogicalExpression root = (LogicalExpression)e;
        assertEquals(LogicalExpression.Operator.AND, root.getOperator());
        assertTrue(root.getLeft() instanceof ComparisonExpression);

        ComparisonExpression between = (ComparisonExpression)root.getLeft();
        assertEquals(ComparisonExpression.Operator.BETWEEN, between.getOperator());
        assertEquals(18, ((LiteralExpression)between.getValue()).getValue());
        assertEquals(65, ((LiteralExpression)between.getSecondValue()).getValue());
    }

    @Test
    void parsesNotBetween() {
        ComparisonExpression e = (ComparisonExpression)ExpressionParser
                .parse("salary NOT BETWEEN 1.5E2 AND 65000e-1");

        assertEquals(ComparisonExpression.Operator.NOT_BETWEEN, e.getOperator());
        assertThat(((LiteralExpression)e.getValue()).getValue()).isInstanceOf(BigDecimal.class);
        assertThat(((LiteralExpression)e.getSecondValue()).getValue()).isInstanceOf(BigDecimal.class);
    }

    @Test
    void parsesNestedProperty() {
        PropertyExpression e = ((ComparisonExpression)ExpressionParser
                .parse("customer.address.city = 'Quebec'"))
                .getProperty();

        assertEquals("customer.address.city", e.getPath());
    }

    @Test
    void parsesPositionalParameters() {
        ComparisonExpression e = (ComparisonExpression)ExpressionParser
                .parse("weight between ?1 and ?2");

        assertEquals("1", ((ParameterExpression)e.getValue()).getName());
        assertEquals("2", ((ParameterExpression)e.getSecondValue()).getName());
    }

    @Test
    void parsesNamedParameters() {
        ComparisonExpression e = (ComparisonExpression)ExpressionParser
                .parse("age BETWEEN :minAge AND :maxAge");

        assertEquals("minAge", ((ParameterExpression)e.getValue()).getName());
        assertEquals("maxAge", ((ParameterExpression)e.getSecondValue()).getName());
    }

    @Test
    void parsesInList() {
        ComparisonExpression e = (ComparisonExpression)ExpressionParser
                .parse("status IN ('ACTIVE', 'PENDING')");

        assertEquals(ComparisonExpression.Operator.IN, e.getOperator());

        ListExpression list = (ListExpression)e.getValue();
        assertEquals(2, list.getValues().size());
    }

    @Test
    void parsesNotPrecedence() {
        Expression e = ExpressionParser.parse(
            "NOT active = false OR age >= 18 AND enabled = true");

        assertTrue(e instanceof LogicalExpression);
        assertEquals(LogicalExpression.Operator.OR, ((LogicalExpression)e).getOperator());
    }

    @Test
    void rejectsInvalidBetween() {
        assertThrows(ExpressionParseException.class,
                () -> ExpressionParser.parse("age BETWEEN 18 65"));
    }

    @Test
    void parsesEscapedQuote() {
        ComparisonExpression e = (ComparisonExpression)ExpressionParser
                .parse("name = 'O''Connor'");

        assertEquals("O'Connor", ((LiteralExpression)e.getValue()).getValue());
    }
}
