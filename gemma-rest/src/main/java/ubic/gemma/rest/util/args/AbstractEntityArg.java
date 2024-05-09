package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Interface representing and API call argument that can represent various identifiers of different types. E.g a taxon
 * can be represented by Long number (ID) or multiple String properties (scientific/common name).
 *
 * @param <T> the type that the argument is expected to mutate to as per {@link Arg}
 * @param <O> the persistent object type.
 * @param <S> the service for the object type.
 * @author tesarst
 * @author poirigui
 */
public abstract class AbstractEntityArg<T, O extends Identifiable, S extends FilteringService<O>> extends AbstractArg<T> implements Arg<T> {

    private final String propertyName;
    private final Class<T> propertyType;

    protected AbstractEntityArg( String propertyName, Class<T> propertyType, T value ) {
        super( value );
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    String getPropertyName() {
        return propertyName;
    }

    Class<T> getPropertyType() {
        return propertyType;
    }

    /**
     * Defines how to retrieve an entity from a service.
     * @param service the service to retrieve the entity from
     * @return the entity matching the argument if found, otherwise null
     */
    @Nullable
    abstract O getEntity( S service );

    /**
     * Defines how to retrieve multiple entities from a service.
     * <p>
     * This is only meaningful if the argument is ambiguous, otherwise {@link #getEntity(FilteringService)} should be
     * used.
     * @see #getEntities(FilteringService)
     */
    List<O> getEntities( S service ) {
        O entity = getEntity( service );
        if ( entity != null ) {
            return Collections.singletonList( entity );
        } else {
            return Collections.emptyList();
        }
    }
}
