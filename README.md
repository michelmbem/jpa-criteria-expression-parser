# JPA Criteria Expression Parser

A parser for converting text-based expressions into the JPA Criteria API, with no external dependencies.

Primary target: Java 8+ / JPA 2.x (`javax.persistence`), specifically tested with WildFly 26.1.3.

## Syntax

Supported features:

- `=`, `!=`, `<>`, `<`, `<=`, `>`, `>=`
- `LIKE`, `NOT LIKE`
- `IN`, `NOT IN`
- `BETWEEN`, `NOT BETWEEN`
- `IS NULL`, `IS NOT NULL`
- `AND`, `OR`, `NOT`
- Parentheses
- Strings (enclosed in `'...'`, with `''` used to represent an apostrophe)
- Integers and decimal numbers
- `true`, `false`, `null`
- Nested properties: `customer.address.city`
- Positional parameters: `?1`, `?2`
- Named parameters: `:minAge`, `:status`

Examples:

```hql
age BETWEEN 18 AND 65
age NOT BETWEEN 18 AND 65
name LIKE 'Mic%'
status IN ('ACTIVE', 'PENDING')
customer.address.city = 'Quebec'
(age >= 18 AND active = true) OR status = 'ADMIN'
age BETWEEN ?1 AND ?2 AND status = ?3
age BETWEEN :minAge AND :maxAge AND status = :status
```

Operator precedence:

```hql
NOT
AND
OR
```

## Usage

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> root = query.from(User.class);

Map<String, Object> parameters = new HashMap<>();
parameters.put("minAge", 18);
parameters.put("maxAge", 65);
parameters.put("status", UserStatus.ACTIVE);

Predicate predicate = CriteriaExpression.toPredicate(
        entityManager, cb, root,
        "age BETWEEN :minAge AND :maxAge AND status = :status",
        parameters);

query.where(predicate);

List<User> users = entityManager.createQuery(query).getResultList();
```

Or with positional parameters

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> root = query.from(User.class);

Predicate predicate = CriteriaExpression.toPredicate(
        entityManager, cb, root,
        "age BETWEEN ?1 AND ?2 AND status = ?3",
        18, 65, UserStatus.ACTIVE);

query.where(predicate);

List<User> users = entityManager.createQuery(query).getResultList();
```

## JPA Metamodel

The converter uses `EntityManager.getMetamodel()` to:

- verify that properties exist;
- determine their Java type;
- traverse associations;
- traverse `@Embeddable` types;
- convert literals to the actual type of the attribute.

Supported types include, among others:

- `String`
- `Boolean` / `boolean`
- `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`
- `BigInteger`, `BigDecimal`
- enums
- `java.util.Date`
- `java.sql.Date`
- `java.sql.Timestamp`
- `LocalDate`
- `LocalDateTime`
- `LocalTime`
- `Instant`
- `OffsetDateTime`
- `ZonedDateTime`

For `java.util.Date`, the `@Temporal` annotation is inspected to distinguish between `DATE`, `TIME`, and `TIMESTAMP`.

## Structure

```text
.
├── pom.xml
├── README.md
├── NOTES.md
└── src
    ├── main
    │   └── java/org/addy/persistence/criteria
    │       ├── CriteriaExpression.java
    │       ├── converter
    │       │   ├── CriteriaExpressionConverter.java
    │       │   ├── JpaPathResolver.java
    │       │   └── ValueConverter.java
    │       ├── expression
    │       │   ├── ComparisonExpression.java
    │       │   ├── Expression.java
    │       │   ├── ListExpression.java
    │       │   ├── LiteralExpression.java
    │       │   ├── LogicalExpression.java
    │       │   ├── NotExpression.java
    │       │   ├── ParameterExpression.java
    │       │   └── PropertyExpression.java
    │       ├── parser
    │       │   ├── ExpressionLexer.java
    │       │   ├── ExpressionParseException.java
    │       │   ├── ExpressionParser.java
    │       │   ├── Token.java
    │       │   └── TokenType.java
    │       └── printer
    │           └── ExpressionPrinter.java
    └── test
        └── java/org/addy/persistence/criteria
            └── ExpressionParserTest.java
```

## Note

The parser produces an AST that is independent of JPA. This allows for testing the syntax without an `EntityManager`, and subsequently converting the AST into a `Predicate` only when building the query.

Support for `?index` and`:name` parameters is intentionally included, allowing values to be kept separate from the textual expression.
