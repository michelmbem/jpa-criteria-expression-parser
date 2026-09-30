package org.addy.persistence.criteria;

import org.addy.persistence.criteria.expression.*;
import org.addy.persistence.criteria.parser.ExpressionParseException;
import org.addy.persistence.criteria.parser.ExpressionParser;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExpressionParserTest {

    @Test
    public void parsesBetweenAndLogicalAnd() {
        Expression e = ExpressionParser.parse(
            "age BETWEEN 18 AND 65 AND active = true");

        assertTrue(e instanceof LogicalExpression);

        LogicalExpression root = (LogicalExpression)e;
        assertEquals(LogicalExpression.Operator.AND, root.getOperator());

        assertTrue(root.getLeft() instanceof ComparisonExpression);
        ComparisonExpression between =
            (ComparisonExpression)root.getLeft();

        assertEquals(
            ComparisonExpression.Operator.BETWEEN,
            between.getOperator());

        assertEquals(18, ((LiteralExpression)between.getValue()).getValue());
        assertEquals(65, ((LiteralExpression)between.getSecondValue()).getValue());
    }

    @Test
    public void parsesNotBetween() {
        ComparisonExpression e =
            (ComparisonExpression)ExpressionParser.parse(
                "age NOT BETWEEN 18 AND 65");

        assertEquals(
            ComparisonExpression.Operator.NOT_BETWEEN,
            e.getOperator());
    }

    @Test
    public void parsesNestedProperty() {
        PropertyExpression e =
                ((ComparisonExpression)
                    ExpressionParser.parse(
                        "customer.address.city = 'Quebec'"))
                    .getProperty();

        assertEquals("customer.address.city", e.getPath());
    }

    @Test
    public void parsesParameters() {
        ComparisonExpression e =
            (ComparisonExpression)ExpressionParser.parse(
                "age BETWEEN :minAge AND :maxAge");

        assertEquals("minAge",
            ((ParameterExpression)e.getValue()).getName());

        assertEquals("maxAge",
            ((ParameterExpression)e.getSecondValue()).getName());
    }

    @Test
    public void parsesInList() {
        ComparisonExpression e =
            (ComparisonExpression)ExpressionParser.parse(
                "status IN ('ACTIVE', 'PENDING')");

        assertEquals(
            ComparisonExpression.Operator.IN,
            e.getOperator());

        ListExpression list = (ListExpression)e.getValue();
        assertEquals(2, list.getValues().size());
    }

    @Test
    public void parsesNotPrecedence() {
        Expression e = ExpressionParser.parse(
            "NOT active = false OR age >= 18 AND enabled = true");

        assertTrue(e instanceof LogicalExpression);
        assertEquals(
            LogicalExpression.Operator.OR,
            ((LogicalExpression)e).getOperator());
    }

    @Test(expected = ExpressionParseException.class)
    public void rejectsInvalidBetween() {
        ExpressionParser.parse("age BETWEEN 18 65");
    }

    @Test
    public void parsesEscapedQuote() {
        ComparisonExpression e =
            (ComparisonExpression)ExpressionParser.parse(
                "name = 'O''Connor'");

        assertEquals(
            "O'Connor",
            ((LiteralExpression)e.getValue()).getValue());
    }
}
