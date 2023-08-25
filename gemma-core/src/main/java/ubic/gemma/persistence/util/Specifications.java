package ubic.gemma.persistence.util;

import lombok.Value;
import ubic.gemma.model.common.Identifiable;

public final class Specifications {

    /**
     * Create a specification for an {@link Identifiable} entity.
     */
    public static <T extends Identifiable> Specification<T> byIdentifiable( T entity ) {
        return new IdentifiableSpecification<>( entity );
    }

    @Value
    private static class IdentifiableSpecification<T extends Identifiable> implements Specification<T> {
        T entity;

        @Override
        public String toString() {
            return "An entity matching " + entity;
        }
    }
}
