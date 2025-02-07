package ubic.gemma.model.expression.bioAssayData;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> characteristicIds;

    /**
     * Indicate how many cells have an assigned characteristic.
     */
    private int numberOfAssignedCells;

    public CellLevelCharacteristicsValueObject( CellLevelCharacteristics cellLevelCharacteristics, boolean excludeCharacteristicIds ) {
        this.characteristics = cellLevelCharacteristics.getCharacteristics()
                .stream().map( CharacteristicValueObject::new )
                .collect( Collectors.toSet() );
        if ( !excludeCharacteristicIds ) {
            try {
                this.characteristicIds = Arrays.stream( cellLevelCharacteristics.getIndices() )
                        .mapToObj( i -> i != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC ? requireNonNull( cellLevelCharacteristics.getCharacteristics().get( i ).getId() ) : null )
                        .collect( Collectors.toList() );
            } catch ( IndexOutOfBoundsException e ) {
                // this may happen because getCellType() can fail if the data we have is incorrect, but we don't want to
                // break the VO serialization which would break the REST API.
                log.warn( "Characteristic indices are invalid for " + cellLevelCharacteristics + "." );
            }
        }
        if ( cellLevelCharacteristics.getNumberOfAssignedCells() != null ) {
            numberOfAssignedCells = cellLevelCharacteristics.getNumberOfAssignedCells();
        } else {
            numberOfAssignedCells = ( int ) Arrays.stream( cellLevelCharacteristics.getIndices() )
                    .filter( i -> i != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC )
                    .count();
        }
    }
}
