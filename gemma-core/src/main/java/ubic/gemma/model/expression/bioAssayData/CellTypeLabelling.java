package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the labelling of cell types.
 */
@Getter
@Setter
public class CellTypeLabelling extends Analysis {

    /**
     * Indicate if this labelling is the preferred one.
     */
    private boolean preferred;

    /**
     * Cell types assignment to individual cells from the {@link #cellTypeLabels} collections.
     */
    private int[] cellTypes;

    /**
     * Cell type labels.
     */
    private List<Characteristic> cellTypeLabels;

    /**
     * Number of distinct cell types.
     * <p>
     * This must always be equal to number of distinct elements of {@link #cellTypeLabels}.
     */
    private Integer numberOfCellTypeLabels;

    public Characteristic getCellTypeLabel( int index ) {
        Assert.notNull( cellTypes, "No cell types have been assigned." );
        Assert.notNull( cellTypeLabels, "No cell labels exist." );
        return cellTypeLabels.get( cellTypes[index] );
    }

    @Override
    public int hashCode() {
        return Objects.hash( Arrays.hashCode( cellTypes ), cellTypeLabels );
    }

    @Override
    public boolean equals( Object object ) {
        return super.equals( object );
    }
}
