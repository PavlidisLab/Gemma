package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import org.springframework.lang.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generic cell-level characteristics.
 * <p>
 * For cell types, use {@link CellTypeAssignment} instead.
 * <p>
 * This is not meant to be used directly, prefer {@link CellLevelCharacteristics.Factory#newInstance} for creating
 * cell-level characteristics or {@link CellTypeAssignment} for cell types.
 *
 * @author poirigui
 */
@Getter
@Setter
@Entity
@Table(name = "CELL_LEVEL_CHARACTERISTICS")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class GenericCellLevelCharacteristics extends AbstractDescribable implements CellLevelCharacteristics {

    // The characteristics appear in the CHARACTERISTIC table; spell out the FK + ordering column.
    @MayBeUninitialized
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "CELL_LEVEL_CHARACTERISTICS_FK", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "CHARACTERISTIC_CELL_LEVEL_CHARACTERISTICS_FKC"))
    @OrderColumn(name = "CELL_LEVEL_CHARACTERISTICS_ORDERING")
    @Immutable
    private List<Characteristic> characteristics;

    @Column(name = "NUMBER_OF_CHARACTERISTICS", nullable = false, columnDefinition = "INTEGER")
    private int numberOfCharacteristics;

    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "int"))
    @Column(name = "INDICES", nullable = false, columnDefinition = "LONGBLOB")
    private int[] indices;

    // FIXME: make this field not-null
    @Nullable
    @Column(name = "NUMBER_OF_ASSIGNED_CELLS", columnDefinition = "INTEGER")
    private Integer numberOfAssignedCells;

    @Nullable
    @Override
    public Characteristic getCharacteristic( int cellIndex ) {
        int i = indices[cellIndex];
        if ( i == UNKNOWN_CHARACTERISTIC ) {
            return null;
        } else {
            return characteristics.get( i );
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof GenericCellLevelCharacteristics ) )
            return false;
        GenericCellLevelCharacteristics that = ( GenericCellLevelCharacteristics ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that )
                && Objects.equals( characteristics, that.characteristics )
                && Arrays.equals( indices, that.indices );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( characteristics != null && ModelUtils.isInitialized( characteristics ) ? " Characteristics=" + characteristics.stream().map( Characteristic::getValue ).collect( Collectors.joining( ", " ) ) : "" )
                + ( " Number of characteristics=" + numberOfCharacteristics )
                + ( numberOfAssignedCells != null ? " Number of assigned cells=" + numberOfAssignedCells : "" );
    }
}
