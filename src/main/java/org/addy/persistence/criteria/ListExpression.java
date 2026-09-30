package org.addy.persistence.criteria;

import java.util.Collections;
import java.util.List;

public final class ListExpression implements Expression {
    private final List<Expression> values;

    public ListExpression(List<Expression> values) {
        this.values = Collections.unmodifiableList(values);
    }

    public List<Expression> getValues() {
        return values;
    }
}
