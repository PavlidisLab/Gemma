package ubic.gemma.model.expression.bioAssayData;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Objects;

/**
 * Denormalized link row that records the {@link SingleCellDimension} attached to a given
 * {@link ExpressionExperiment} / {@link QuantitationType} pair.
 * <p>
 * Backed by the {@code SINGLE_CELL_DIMENSION_EXPERIMENT} table introduced in MySQL migration V7 /
 * H2 migration V9. Exists to replace the 30+ "scan SCEDV, group by SingleCellDimension" HQLs in
 * {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl} with
 * single-row index lookups against a ~528-row link table — see {@code PERF_PROBE_REPORT_ROUND4.md}
 * finding B1.
 * <p>
 * Today every {@code (EE, QT)} maps to exactly one {@code SingleCellDimension}; the schema enforces
 * that via a unique constraint on {@code (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK)}.
 */
@Entity
@Table(name = "SINGLE_CELL_DIMENSION_EXPERIMENT")
@Getter
@Setter
public class SingleCellDimensionExperiment extends AbstractIdentifiable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EXPRESSION_EXPERIMENT_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_SCDE_EXPRESSION_EXPERIMENT"))
    private ExpressionExperiment expressionExperiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QUANTITATION_TYPE_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_SCDE_QUANTITATION_TYPE"))
    private QuantitationType quantitationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SINGLE_CELL_DIMENSION_FK", nullable = false, columnDefinition = "BIGINT",
            foreignKey = @ForeignKey(name = "FK_SCDE_SINGLE_CELL_DIMENSION"))
    private SingleCellDimension singleCellDimension;

    @Override
    public int hashCode() {
        return Objects.hash( expressionExperiment, quantitationType, singleCellDimension );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SingleCellDimensionExperiment ) ) {
            return false;
        }
        SingleCellDimensionExperiment other = ( SingleCellDimensionExperiment ) object;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( expressionExperiment, other.expressionExperiment )
                && Objects.equals( quantitationType, other.quantitationType )
                && Objects.equals( singleCellDimension, other.singleCellDimension );
    }
}
