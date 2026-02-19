package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

/**
 * Base service class for an immutable entity.
 * <p>
 * Immutable entities can be created, deleted but never updated.
 *
 * @author poirigui
 */
public interface BaseImmutableService<O extends Identifiable> extends BaseReadOnlyService<O> {

    /**
     * @see BaseDao#findOrCreate(Identifiable)
     */
    @CheckReturnValue
    O findOrCreate( O entity );

    /**
     * @see BaseDao#create(Collection)
     */
    @CheckReturnValue
    Collection<O> create( Collection<O> entities );

    /**
     * @see BaseDao#create(Identifiable)
     */
    @CheckReturnValue
    O create( O entity );

    /**
     * @see BaseDao#remove(Collection)
     */
    void remove( Collection<O> entities );

    /**
     * @see BaseDao#remove(Identifiable)
     */
    void remove( O entity );
}
