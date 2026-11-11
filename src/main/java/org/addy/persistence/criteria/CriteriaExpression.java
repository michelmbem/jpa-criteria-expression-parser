package org.addy.persistence.criteria;

import org.addy.persistence.criteria.converter.CriteriaExpressionConverter;
import org.addy.persistence.criteria.expression.Expression;
import org.addy.persistence.criteria.parser.ExpressionParser;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

public final class CriteriaExpression {
    private CriteriaExpression() {
    }

    public static <T> Predicate toPredicate(EntityManager entityManager,
                                            CriteriaBuilder criteriaBuilder,
                                            Root<T> root,
                                            String expression) {

        return toPredicate(entityManager, criteriaBuilder, root, expression,
                Collections.emptyMap());
    }

    public static <T> Predicate toPredicate(EntityManager entityManager,
                                            CriteriaBuilder criteriaBuilder,
                                            Root<T> root,
                                            String expression,
                                            Object... parameters) {

        return toPredicate(entityManager, criteriaBuilder, root, expression,
                list2Map(Arrays.asList(parameters)));
    }

    public static <T> Predicate toPredicate(EntityManager entityManager,
                                            CriteriaBuilder criteriaBuilder,
                                            Root<T> root,
                                            String expression,
                                            List<Object> parameters) {

        return toPredicate(entityManager, criteriaBuilder, root, expression,
                list2Map(parameters));
    }

    public static <T> Predicate toPredicate(EntityManager entityManager,
                                            CriteriaBuilder criteriaBuilder,
                                            Root<T> root,
                                            String expression,
                                            Map<String, Object> parameters) {

        Expression ast = ExpressionParser.parse(expression);
        CriteriaExpressionConverter converter = new CriteriaExpressionConverter(
                entityManager, criteriaBuilder, parameters);
        return converter.toPredicate(ast, root);
    }

    private static Map<String, Object> list2Map(List<Object> values) {
        Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < values.size(); ++i) {
            map.put(String.valueOf(i + 1), values.get(i));
        }

        return map;
    }
}
