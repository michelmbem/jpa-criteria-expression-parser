package org.addy.persistence.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.Map;

public final class CriteriaExpression {
    private CriteriaExpression() {
    }

    public static <T> Predicate toPredicate(
            EntityManager entityManager,
            CriteriaBuilder criteriaBuilder,
            Root<T> root,
            String expression) {
        return toPredicate(entityManager, criteriaBuilder, root,
            expression, Collections.<String, Object>emptyMap());
    }

    public static <T> Predicate toPredicate(
            EntityManager entityManager,
            CriteriaBuilder criteriaBuilder,
            Root<T> root,
            String expression,
            Map<String, Object> parameters) {

        Expression ast = ExpressionParser.parse(expression);

        CriteriaExpressionConverter converter =
            new CriteriaExpressionConverter(
                criteriaBuilder, entityManager, parameters);

        return converter.toPredicate(ast, root);
    }
}
