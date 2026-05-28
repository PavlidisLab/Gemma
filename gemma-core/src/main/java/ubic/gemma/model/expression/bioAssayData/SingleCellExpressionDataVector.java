package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.hibernate.ByteArrayType;

import org.springframework.lang.Nullable;
import java.util.Objects;

/**
 * An expression data vector that contains data at the resolution of individual cells.
 * <p>
 * This is achieved by storing cell metadata such as IDs and cell types in a {@link SingleCellDimension} that is shared
 * among all vectors of a given {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and individual
 * non-zero cell expression in a sparse data structure similar to the rows of a CSR matrix.
 *
 * @author poirigui
 */
@Getter
@Setter
@Entity
@Table(name = "SINGLE_CELL_EXPRESSION_DATA_VECTOR")
@Immutable
@AssociationOverrides({
        @AssociationOverride(name = "expressionExperiment",
                joinColumns = @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", nullable = false, columnDefinition = "BIGINT"),
                foreignKey = @ForeignKey(name = "SINGLE_CELL_EXPRESSION_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC")),
        @AssociationOverride(name = "designElement",
                joinColumns = @JoinColumn(name = "DESIGN_ELEMENT_FK", nullable = false, columnDefinition = "BIGINT"),
                foreignKey = @ForeignKey(name = "SINGLE_CELL_EXPRESSION_DATA_VECTOR_DESIGN_ELEMENT_FKC"))
})
public class SingleCellExpressionDataVector extends DesignElementDataVector {

    // Single-cell hbm fetched QT eagerly (lazy="false"); declared here rather than on DataVector
    // because bulk vectors keep it LAZY. @MappedSuperclass + @AssociationOverride cannot change
    // fetch mode, so each leaf hierarchy declares the field with the fetch policy it needs.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "QUANTITATION_TYPE_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "SINGLE_CELL_EXPRESSION_DATA_VECTOR_QUANTITATION_TYPE_FKC"))
    private QuantitationType quantitationType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SINGLE_CELL_DIMENSION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "SINGLE_CELL_EXPRESSION_DATA_VECTOR_SINGLE_CELL_DIMENSION_FKC"))
    private SingleCellDimension singleCellDimension;

    @Nullable
    @Column(name = "ORIGINAL_DESIGN_ELEMENT", columnDefinition = "VARCHAR(255)")
    private String originalDesignElement;

    /**
     * Positions of the non-zero data in the {@link #getData()} vector.
     * <p>
     * This is mapped in the database using {@link ByteArrayType}.
     */
    @Type(value = ByteArrayType.class, parameters = @Parameter(name = "arrayType", value = "int"))
    @Column(name = "DATA_INDICES", nullable = false, columnDefinition = "LONGBLOB")
    private int[] dataIndices;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SingleCellExpressionDataVector ) ) {
            return false;
        }
        SingleCellExpressionDataVector other = ( SingleCellExpressionDataVector ) object;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( getExpressionExperiment(), other.getExpressionExperiment() )
                && Objects.equals( getQuantitationType(), other.getQuantitationType() )
                && Objects.equals( getDesignElement(), other.getDesignElement() );
    }

    @Override
    public String toString() {
        return String.format( "%s%s%s%s%s%s%s", this.getClass().getSimpleName(),
                this.getId() != null ? " Id=" + this.getId() : "",
                this.getDesignElement() != null ? " DE=" + this.getDesignElement().getName() : "",
                this.getExpressionExperiment() != null ? " EE=" + this.getExpressionExperiment().getName() : "",
                this.getQuantitationType() != null ? " QT=" + this.getQuantitationType().getName() : "",
                this.getSingleCellDimension() != null ? " SCD=" + this.getSingleCellDimension().getId() : "",
                this.getData() != null ? ", " + this.getData().length + " bytes" : "" );
    }
}
