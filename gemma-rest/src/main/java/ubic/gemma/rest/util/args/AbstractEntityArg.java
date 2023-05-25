package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;

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
     *
     * @return the entity matching the argument if found, otherwise null
     * @throws BadRequestException if the argument is incorrectly formed
     * @deprecated specify the retrieval logic by implementing {@link AbstractEntityArgService#getEntity(AbstractEntityArg)} instead
     */
    @Nullable
    @Deprecated
    abstract O getEntity( S service ) throws BadRequestException;
}
