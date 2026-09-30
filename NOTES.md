# Notes

## @Temporal

The enhanced converter inspects the Java field for `@Temporal`.
This works directly for field-access JPA mappings.

If the entity uses property access (`@Temporal` on the getter), adapt
`ValueConverter.findField()` to inspect the corresponding getter.

## Associations and embeddables

The path resolver validates every component through the JPA metamodel.
For an association or embeddable path such as:

    customer.address.city

the final attribute type is used for literal conversion.

## Collections

Collection-valued association paths are not intended as scalar comparison
paths. `IN` here means comparison of a scalar property against a list of
values, e.g.:

    status IN ('ACTIVE', 'PENDING')

## Null

Use:

    property IS NULL
    property IS NOT NULL

rather than:

    property = NULL

The latter is parsed but produces `cb.equal(path, null)` and should generally
be avoided in favor of SQL/JPA null semantics.
