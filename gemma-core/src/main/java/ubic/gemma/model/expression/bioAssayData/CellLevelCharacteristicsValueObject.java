package ubic.gemma.model.expression.bioAssayData;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.DescribableValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@CommonsLog
public class CellLevelCharacteristicsValueObject extends DescribableValueObject<CellLevelCharacteristics> {

    private String category;
    @Nullable
    private String categoryUri;

    private List<CharacteristicValueObject> characteristics;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Long> characteristicIds;

    /**
     * Indicate how many cells have an assigned characteristic, or {@code null} if this information is not available.
     */
    @Nullable
    private Integer numberOfAssignedCells;

    public CellLevelCharacteristicsValueObject( CellLevelCharacteristics cellLevelCharacteristics, boolean excludeCharacteristicIds ) {
        super( cellLevelCharacteristics );
        this.characteristics = cellLevelCharacteristics.getCharacteristics()
                .stream()
                .sorted( Characteristic.getComparator() )
                .map( CharacteristicValueObject::new )
                .collect( Collectors.toList() );
        if ( !cellLevelCharacteristics.getCharacteristics().isEmpty() ) {
            Characteristic firstCharacteristic = cellLevelCharacteristics.getCharacteristics().iterator().next();
            this.category = firstCharacteristic.getCategory();
            this.categoryUri = firstCharacteristic.getCategoryUri();
        }
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
        } else if ( cellLevelCharacteristics.getIndices() != null ) {
            numberOfAssignedCells = ( int ) Arrays.stream( cellLevelCharacteristics.getIndices() )
                    .filter( i -> i != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC )
                    .count();
        } else {
            numberOfAssignedCells = null;
        }
    }
}
