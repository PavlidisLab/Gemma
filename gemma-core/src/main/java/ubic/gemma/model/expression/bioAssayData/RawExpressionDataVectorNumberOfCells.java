package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import java.util.Objects;

/**
 * @author poirigui
 */
@Getter
@Setter
@Entity
@Table(name = "RAW_EXPRESSION_DATA_VECTOR_NUMBER_OF_CELLS")
class RawExpressionDataVectorNumberOfCells extends BulkExpressionDataVectorNumberOfCells {

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ID", columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "RAW_EXPRESSION_DATA_VECTOR_FKC"))
    private RawExpressionDataVector vector;

    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "int"))
    @Column(name = "NUMBER_OF_CELLS", nullable = false, columnDefinition = "LONGBLOB")
    private int[] numberOfCells;

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof RawExpressionDataVectorNumberOfCells ) ) {
            return false;
        }
        RawExpressionDataVectorNumberOfCells that = ( RawExpressionDataVectorNumberOfCells ) object;
        return Objects.equals( getId(), that.getId() );
    }

    static class Factory {
        static RawExpressionDataVectorNumberOfCells newInstance( RawExpressionDataVector vector, int[] numberOfCells ) {
            RawExpressionDataVectorNumberOfCells result = new RawExpressionDataVectorNumberOfCells();
            result.setVector( vector );
            result.setNumberOfCells( numberOfCells );
            return result;
        }
    }
}
