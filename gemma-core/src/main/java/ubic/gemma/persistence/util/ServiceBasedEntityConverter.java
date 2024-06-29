package ubic.gemma.persistence.util;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseReadOnlyService;

import java.util.*;

/**
 * Performs conversion by identifier and collection of identifier for a {@link BaseReadOnlyService}.
 * <p>
 * The converter recognize two cases: {@link Long} → {@link O} and {@link Collection} of {@link Long} to {@link List}
 * of {@link O} using {@link BaseReadOnlyService#load(Long)} and {@link BaseReadOnlyService#load(Collection)} respectively.
 * <p>
 * The conversion also works with supertypes of {@link O} up to {@link Identifiable}.
 *
 * @param <O> the type of entity this converter produces
 * @see BaseReadOnlyService#load(Long)
 * @see BaseReadOnlyService#load(Collection)
 * @author poirigui
 */
public class ServiceBasedEntityConverter<O extends Identifiable> implements ConditionalGenericConverter {

    private final BaseReadOnlyService<? extends O> service;
    private final TypeDescriptor entityType;
    private final TypeDescriptor entityListType;
    private final TypeDescriptor entityIdType = TypeDescriptor.valueOf( Long.class );
    private final TypeDescriptor entityIdCollectionType = TypeDescriptor.collection( Collection.class, entityIdType );
    protected final Set<ConvertiblePair> convertibleTypes = new HashSet<>();

    public ServiceBasedEntityConverter( BaseReadOnlyService<? extends O> service, Class<O> entityType ) {
        this.service = service;
        this.entityType = TypeDescriptor.valueOf( entityType );
        this.entityListType = TypeDescriptor.collection( List.class, this.entityType );
        this.convertibleTypes.add( new ConvertiblePair( Long.class, Identifiable.class ) );
        this.convertibleTypes.add( new ConvertiblePair( Collection.class, Collection.class ) );
    }


    @Override
    public boolean matches( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return entityFromId( sourceType, targetType ) || entityListFromIds( sourceType, targetType );
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.unmodifiableSet( convertibleTypes );
    }

    @Override
    public Object convert( @Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType ) {
        if ( entityFromId( sourceType, targetType ) ) {
            return source != null ? service.load( ( Long ) source ) : null;
        }

        if ( entityListFromIds( sourceType, targetType ) ) {
            //noinspection unchecked
            return source != null ? service.load( ( Collection<Long> ) source ) : null;
        }

        throw new ConverterNotFoundException( sourceType, targetType );
    }

    private boolean entityFromId( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.entityIdType ) && this.entityType.isAssignableTo( targetType );
    }

    private boolean entityListFromIds( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.entityIdCollectionType ) && this.entityListType.isAssignableTo( targetType );
    }
}
