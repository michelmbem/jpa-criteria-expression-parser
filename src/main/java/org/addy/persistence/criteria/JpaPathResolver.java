package org.addy.persistence.criteria;

import javax.persistence.EntityManager;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

public final class JpaPathResolver {
    private final EntityManager entityManager;

    public JpaPathResolver(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Path<?> resolve(From<?, ?> root, String pathExpression) {
        String[] parts = pathExpression.split("\\.");
        Path<?> path = root;
        Class<?> currentType = root.getJavaType();

        for (int i = 0; i < parts.length; i++) {
            String property = parts[i];

            ManagedType<?> managedType = managedType(currentType);

            Attribute<?, ?> attribute;
            try {
                attribute = managedType.getAttribute(property);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Unknown property '" + property
                    + "' in path '" + pathExpression + "'", e);
            }

            path = path.get(property);

            if (i < parts.length - 1) {
                currentType = attribute.getJavaType();
            }
        }

        return path;
    }

    private ManagedType<?> managedType(Class<?> type) {
        try {
            return entityManager.getMetamodel().managedType(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Type '" + type.getName()
                + "' cannot be traversed as an entity or embeddable", e);
        }
    }
}
