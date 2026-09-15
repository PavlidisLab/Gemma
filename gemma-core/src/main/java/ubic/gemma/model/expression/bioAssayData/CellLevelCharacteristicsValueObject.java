package ubic.gemma.model.expression.bioAssayData;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import org.springframework.lang.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class CellLevelCharacteristicsValueObject extends IdentifiableValueObject<CellLevelCharacteristics> {

    /**
     * The distinct labels cells are grouped under; see {@link CellLevelCharacteristics}.
     */
    @Schema(description = "The distinct labels cells are grouped under, one entry per label and not one per cell.")
    private Set<CharacteristicValueObject> characteristics;

    /**
     * For each cell, the id of its label in {@link #characteristics}.
     */
    @Schema(description = "For each cell of the single-cell dimension, in its cell order, the id of that cell's label "
            + "in `characteristics`, or null when the cell has none.")
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
        } else if ( cellLevelCharacteristics.getIndices() != null ) {
            numberOfAssignedCells = ( int ) Arrays.stream( cellLevelCharacteristics.getIndices() )
                    .filter( i -> i != CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC )
                    .count();
        } else {
            numberOfAssignedCells = null;
        }
    }
}
