package ubic.gemma.persistence.util;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;
import java.util.List;

/**
 * Perform conversion to value object by entity, ID and collections of entities and IDs and also to entity by ID and
 * collection of IDs.
 * <p>
 * The converter recognize two cases: converting {@link O} → {@link VO} and converting {@link Collection} of {@link O}
 * to {@link List} of {@link VO} by calling respectively {@link BaseVoEnabledService#loadValueObject(Identifiable)} and
 * {@link BaseVoEnabledService#loadValueObjects(Collection)}.
 * <p>
 * This implementation also work with supertypes of the designated {@link VO}  and subtypes of the {@link O}. For example,
 * you can perform generic conversion to {@link IdentifiableValueObject} without having to mention the specific type of
 * value object you ultimately want.
 *
 * @param <O>  the type of value object this converter consumes and also produces (as inherited from
 *             {@link ServiceBasedEntityConverter}
 * @param <VO> the type of value object this converter produces
 * @see ServiceBasedEntityConverter
 * @see BaseVoEnabledService#loadValueObject(Identifiable)
 * @see BaseVoEnabledService#loadValueObjects(Collection)
 * @see BaseVoEnabledService#loadValueObjectById(Long)
 * @see BaseVoEnabledService#loadValueObjectsByIds(Collection)
 * @author poirigui
 */
public class ServiceBasedValueObjectConverter<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends ServiceBasedEntityConverter<O> implements ConditionalGenericConverter {

    /**
     * We need a service that can produce something can can be cast into a {@link VO}.
     */
    private final BaseVoEnabledService<O, ? extends VO> service;

    /* enables very efficient type checks */
    private final TypeDescriptor sourceType;
    private final TypeDescriptor sourceIdType;
    private final TypeDescriptor targetType;
    private final TypeDescriptor sourceCollectionType;
    private final TypeDescriptor sourceIdCollectionType;
    private final TypeDescriptor targetListType;

    /**
     */
    public ServiceBasedValueObjectConverter( BaseVoEnabledService<O, ? extends VO> service, Class<O> sourceType, Class<VO> targetType ) {
        super( service, sourceType );
        this.service = service;
        this.convertibleTypes.add( new ConvertiblePair( Long.class, IdentifiableValueObject.class ) );
        this.convertibleTypes.add( new ConvertiblePair( Identifiable.class, IdentifiableValueObject.class ) );
        this.convertibleTypes.add( new ConvertiblePair( Collection.class, Collection.class ) );
        this.sourceType = TypeDescriptor.valueOf( sourceType );
        this.sourceIdType = TypeDescriptor.valueOf( Long.class );
        this.targetType = TypeDescriptor.valueOf( targetType );
        this.sourceCollectionType = TypeDescriptor.collection( Collection.class, this.sourceType );
        this.sourceIdCollectionType = TypeDescriptor.collection( Collection.class, this.sourceIdType );
        this.targetListType = TypeDescriptor.collection( List.class, this.targetType );
    }

    @Override
    public boolean matches( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        // this is necessary because Collection and List types are too broad and will result in conflicts with
        // converters for other VOs
        return voFromEntity( sourceType, targetType ) || voListFromEntities( sourceType, targetType ) ||
                voFromId( sourceType, targetType ) || voListFromIds( sourceType, targetType ) ||
                super.matches( sourceType, targetType );
    }

    @Override
    public Object convert( @Nullable Object object, TypeDescriptor sourceType, TypeDescriptor targetType ) {
        if ( voFromEntity( sourceType, targetType ) ) {
            //noinspection unchecked
            return object != null ? service.loadValueObject( ( O ) object ) : null;
        }

        if ( voListFromEntities( sourceType, targetType ) ) {
            //noinspection unchecked
            return object != null ? service.loadValueObjects( ( Collection<O> ) object ) : null;
        }

        if ( voFromId( sourceType, targetType ) ) {
            return object != null ? service.loadValueObjectById( ( Long ) object ) : null;
        }

        if ( voListFromIds( sourceType, targetType ) ) {
            //noinspection unchecked
            return object != null ? service.loadValueObjectsByIds( ( Collection<Long> ) object ) : null;
        }

        return super.convert( object, sourceType, targetType );
    }

    private boolean voFromEntity( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.sourceType ) && this.targetType.isAssignableTo( targetType );
    }

    private boolean voListFromEntities( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.sourceCollectionType ) && this.targetListType.isAssignableTo( targetType );
    }

    private boolean voFromId( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.sourceIdType ) && this.targetType.isAssignableTo( targetType );
    }

    private boolean voListFromIds( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.sourceIdCollectionType ) && this.targetListType.isAssignableTo( targetType );
    }
}
