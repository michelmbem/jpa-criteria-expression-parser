# JPA Criteria Expression Parser

Parser d'expressions textuelles vers JPA Criteria API, sans bibliothèque externe.

Cible principale : Java 8+ / JPA 2.x (`javax.persistence`), notamment WildFly 26.1.3.

## Syntaxe

Supporté :

- `=`, `!=`, `<>`, `<`, `<=`, `>`, `>=`
- `LIKE`, `NOT LIKE`
- `IN`, `NOT IN`
- `BETWEEN`, `NOT BETWEEN`
- `IS NULL`, `IS NOT NULL`
- `AND`, `OR`, `NOT`
- parenthèses
- chaînes `'...'`, avec `''` pour une apostrophe
- nombres entiers et décimaux
- `true`, `false`, `null`
- propriétés imbriquées : `customer.address.city`
- paramètres nommés : `:minAge`, `:status`

Exemples :

```text
age BETWEEN 18 AND 65
age NOT BETWEEN 18 AND 65
name LIKE 'Mic%'
status IN ('ACTIVE', 'PENDING')
customer.address.city = 'Quebec'
(age >= 18 AND active = true) OR status = 'ADMIN'
age BETWEEN :minAge AND :maxAge AND status = :status
```

La priorité est :

```text
NOT
AND
OR
```

## Utilisation

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();

CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> root = query.from(User.class);

Map<String, Object> parameters = new HashMap<>();
parameters.put("minAge", 18);
parameters.put("maxAge", 65);
parameters.put("status", UserStatus.ACTIVE);

Predicate predicate = CriteriaExpression.toPredicate(
    entityManager,
    cb,
    root,
    "age BETWEEN :minAge AND :maxAge AND status = :status",
    parameters
);

query.where(predicate);

List<User> users = entityManager
    .createQuery(query)
    .getResultList();
```

## Métamodèle JPA

Le convertisseur utilise `EntityManager.getMetamodel()` pour :

- vérifier que les propriétés existent ;
- déterminer leur type Java ;
- traverser les associations ;
- traverser les `@Embeddable` ;
- convertir les littéraux vers le type réel de l'attribut.

Les types pris en charge incluent notamment :

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

Pour `java.util.Date`, l'annotation `@Temporal` est inspectée afin de distinguer `DATE`, `TIME` et `TIMESTAMP`.

## Structure

```text
src/main/java/org/addy/bookstore/criteria/
    TokenType.java
    Token.java
    ExpressionLexer.java
    Expression.java
    LiteralExpression.java
    PropertyExpression.java
    ListExpression.java
    ComparisonExpression.java
    LogicalExpression.java
    NotExpression.java
    ExpressionParser.java
    JpaPathResolver.java
    ValueConverter.java
    CriteriaExpressionConverter.java
    CriteriaExpression.java
    ExpressionParseException.java

src/test/java/
    ExpressionParserTest.java
```

## Remarque

Le parser produit un AST indépendant de JPA. Cela permet de tester la syntaxe sans `EntityManager`, puis de convertir l'AST en `Predicate` seulement au moment de construire la requête.

Le support des paramètres `:name` est volontairement inclus : les valeurs peuvent ainsi être séparées de l'expression textuelle.
