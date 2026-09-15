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
 * <p>
 * <b>Cross-version compatibility.</b> This branch shares its database with the deployed production
 * Gemma, which has no mapping for this table. All three foreign keys are therefore declared
 * {@code ON DELETE CASCADE} at the schema level (MySQL migration V23 / H2 V24) so that the older
 * code can delete an {@link ExpressionExperiment}, a {@link QuantitationType} or a
 * {@link SingleCellDimension} without tripping over link rows it cannot see. JPA's
 * {@link ForeignKey} annotation cannot express a delete rule, so the cascade lives only in the
 * migrations — do not regenerate these constraints from the annotations alone, and keep the
 * {@code ON DELETE CASCADE} if this mapping is ever ported to a different DDL source.
 * <p>
 * The cascade is a safety net, not the primary teardown path: callers still clear rows explicitly
 * via {@code SingleCellDimensionExperimentDao.removeByEE}, {@code removeByEEAndQt} and
 * {@code removeBySingleCellDimension}. Because this is a cache derived from
 * {@code SINGLE_CELL_EXPRESSION_DATA_VECTOR}, a cascaded row is never a loss of source data.
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
