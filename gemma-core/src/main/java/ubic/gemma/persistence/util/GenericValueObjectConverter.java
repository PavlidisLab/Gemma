package ubic.gemma.persistence.util;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic value object converter.
 * <p>
 * Performs conversion from entity to value object using a provided {@link Converter}.
 *
 * @author poirigui
 */
public class GenericValueObjectConverter<O extends Identifiable, VO extends IdentifiableValueObject<?>> implements ConditionalGenericConverter {

    private final Converter<O, VO> converter;
    private final Set<ConvertiblePair> convertibleTypes;
    private final TypeDescriptor sourceType;
    private final TypeDescriptor sourceCollectionType;
    private final TypeDescriptor targetType;
    private final TypeDescriptor targetListType;

    public GenericValueObjectConverter( Converter<O, VO> converter, Class<O> fromClazz, Class<? super VO> clazz ) {
        this.converter = converter;
        Set<ConvertiblePair> convertibleTypes = new HashSet<>();
        convertibleTypes.add( new ConvertiblePair( Identifiable.class, IdentifiableValueObject.class ) );
        convertibleTypes.add( new ConvertiblePair( Collection.class, Collection.class ) );
        this.convertibleTypes = Collections.unmodifiableSet( convertibleTypes );
        this.sourceType = TypeDescriptor.valueOf( fromClazz );
        this.sourceCollectionType = TypeDescriptor.collection( Collection.class, this.sourceType );
        this.targetType = TypeDescriptor.valueOf( clazz );
        this.targetListType = TypeDescriptor.collection( List.class, this.targetType );
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return convertibleTypes;
    }

    @Override
    public boolean matches( TypeDescriptor sourceType, TypeDescriptor targetType ) {
        return sourceType.isAssignableTo( this.sourceType ) && this.targetType.isAssignableTo( targetType ) ||
                sourceType.isAssignableTo( this.sourceCollectionType ) && this.targetListType.isAssignableTo( targetType );
    }

    @Override
    public Object convert( @Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType ) {
        if ( sourceType.isAssignableTo( this.sourceType ) && this.targetType.isAssignableTo( targetType ) ) {
            //noinspection unchecked
            return source != null ? converter.convert( ( O ) source ) : null;
        }
        if ( sourceType.isAssignableTo( sourceCollectionType ) && this.targetListType.isAssignableTo( targetType ) ) {
            //noinspection unchecked
            return source != null ? ( ( Collection<O> ) source ).stream()
                    .map( converter::convert )
                    .collect( Collectors.toList() ) : null;
        }
        throw new ConverterNotFoundException( sourceType, targetType );
    }
}
