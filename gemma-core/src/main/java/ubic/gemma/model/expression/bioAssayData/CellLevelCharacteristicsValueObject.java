package ubic.gemma.model.expression.bioAssayData;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@CommonsLog
public class CellLevelCharacteristicsValueObject extends IdentifiableValueObject<CellLevelCharacteristics> {

    private Set<CharacteristicValueObject> characteristics;

    private List<Long> characteristicIds;

    public CellLevelCharacteristicsValueObject( CellLevelCharacteristics cellLevelCharacteristics ) {
        this.characteristics = cellLevelCharacteristics.getCharacteristics()
                .stream().map( CharacteristicValueObject::new )
                .collect( Collectors.toSet() );
        try {
            this.characteristicIds = Arrays.stream( cellLevelCharacteristics.getIndices() )
                    .mapToObj( i -> i != -1 ? requireNonNull( cellLevelCharacteristics.getCharacteristics().get( i ).getId() ) : null )
                    .collect( Collectors.toList() );
        } catch ( IndexOutOfBoundsException e ) {
            // this may happen because getCellType() can fail if the data we have is incorrect, but we don't want to
            // break the VO serialization which would break the REST API.
            log.warn( "Characteristic indices are invalid for " + cellLevelCharacteristics + "." );
        }
    }
}