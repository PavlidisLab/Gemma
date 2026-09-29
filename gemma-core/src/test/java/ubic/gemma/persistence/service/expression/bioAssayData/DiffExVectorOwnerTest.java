package ubic.gemma.persistence.service.expression.bioAssayData;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which id the diff-ex vectors are matched on.
 *
 * <h2>The defect this pins</h2>
 * {@code GET /datasets/{id}/expressions/differential?diffExSet=…} answered {@code 200} with an empty
 * {@code geneExpressionLevels} for EVERY result set belonging to a subset analysis — reported by UIB on
 * 2026-09-16 against GSE239820 (dataset 32294, subset 32662, result sets 573163/573164/573169), where
 * {@code /resultSets/{id}} served the same statistics with probes, genes and p-values.
 * <p>
 * Cause: {@code CachedProcessedExpressionDataVectorServiceImpl.sliceSubSet} stamps each sliced vector
 * with an {@code ExpressionExperimentSubsetValueObject}, so the vector carries the SUBSET's id while the
 * matcher compared it to the id of the dataset in the PATH. Every vector was skipped. An empty 200 is
 * indistinguishable from "no significant genes", which is why this sat unnoticed.
 * <p>
 * Reflection rather than a Spring context: the resolver is static, private and has no collaborators, so
 * a context would make a fast test slow and prove less.
 */
public class DiffExVectorOwnerTest {

    private static Long ownerIdFor( ExpressionExperiment ee, BioAssaySet analyzed ) throws Exception {
        Method m = ProcessedExpressionDataVectorServiceImpl.class
                .getDeclaredMethod( "vectorOwnerIdFor", ExpressionExperiment.class, BioAssaySet.class );
        m.setAccessible( true );
        return ( Long ) m.invoke( null, ee, analyzed );
    }

    private static ExpressionExperiment ee( Long id ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( id );
        return ee;
    }

    /** A whole-experiment analysis: the vectors carry the experiment's own id, as before. */
    @Test
    public void testWholeExperimentAnalysisMatchesOnTheExperimentId() throws Exception {
        ExpressionExperiment parent = ee( 32294L );
        assertThat( ownerIdFor( parent, parent ) ).isEqualTo( 32294L );
    }

    /** 🛑 The regression: a subset analysis must match on the SUBSET's id, not the parent's. */
    @Test
    public void testSubsetAnalysisMatchesOnTheSubsetId() throws Exception {
        ExpressionExperiment parent = ee( 32294L );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 32662L );
        subset.setSourceExperiment( parent );

        assertThat( ownerIdFor( parent, subset ) )
                .as( "vectors sliced for the subset are stamped with the subset's id" )
                .isEqualTo( 32662L );
    }

    /** A result set belonging to somebody else's dataset resolves to nothing, rather than to the caller's. */
    @Test
    public void testAForeignSubsetDoesNotResolve() throws Exception {
        ExpressionExperiment other = ee( 99999L );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 32662L );
        subset.setSourceExperiment( ee( 32294L ) );

        assertThat( ownerIdFor( other, subset ) ).isNull();
    }

    /** A subset with no source experiment is not silently treated as the caller's. */
    @Test
    public void testASubsetWithNoSourceExperimentDoesNotResolve() throws Exception {
        ExpressionExperimentSubSet orphan = new ExpressionExperimentSubSet();
        orphan.setId( 32662L );
        assertThat( ownerIdFor( ee( 32294L ), orphan ) ).isNull();
    }
}
