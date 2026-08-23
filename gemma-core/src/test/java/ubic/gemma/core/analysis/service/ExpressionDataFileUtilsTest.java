package ubic.gemma.core.analysis.service;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExpressionDataFileUtils#getDiffExArchiveFileName}, which resolves an
 * analysis' {@code experimentAnalyzed} down to a short name.
 * <p>
 * The proxy case is the one that matters. {@code experimentAnalyzed} is routinely an
 * uninitialized Hibernate proxy, and the resolver's {@code instanceof} chain does not see through
 * one: a {@code BioAssaySet$HibernateProxy} is an instance of neither {@link ExpressionExperiment}
 * nor {@link ExpressionExperimentSubSet}, so it fell through to
 * {@code UnsupportedOperationException: Don't know about ...}. That aborted
 * {@code makeProcessedData} in its first step ({@code removeInvalidatedData} →
 * {@code deleteDiffExArchiveFile}) for every dataset carrying an existing analysis.
 *
 * @author claude
 */
public class ExpressionDataFileUtilsTest {

    private static ExpressionExperiment ee( Long id, String shortName ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        ee.setShortName( shortName );
        return ee;
    }

    /**
     * Stands in for what Hibernate hands back for a lazy association: a {@link BioAssaySet} that is
     * an instance of neither concrete subclass and resolves to the target only through its
     * {@link LazyInitializer}.
     * <p>
     * A real class rather than a Mockito mock on purpose. {@code Hibernate.unproxy} does not test
     * {@code instanceof HibernateProxy}; it goes through {@code asHibernateProxy()}, a default
     * method that returns {@code this}. A mock stubs that to {@code null} and the proxy silently
     * passes through unresolved — which looks exactly like the bug this test guards.
     */
    private static class LazyProxy extends BioAssaySet implements HibernateProxy {

        private final LazyInitializer li;

        private LazyProxy( BioAssaySet target ) {
            this.li = mock( LazyInitializer.class );
            when( li.getImplementation() ).thenReturn( target );
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return li;
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        // AbstractDescribable declares equals abstract; identity is the right answer for a stand-in.
        @Override
        public boolean equals( Object obj ) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode( this );
        }

        @Override
        public Set<BioAssay> getBioAssays() {
            throw new UnsupportedOperationException( "Nothing should reach through this stand-in." );
        }

        @Override
        public void setBioAssays( Set<BioAssay> bioAssays ) {
            throw new UnsupportedOperationException( "Nothing should reach through this stand-in." );
        }
    }

    private static BioAssaySet lazyProxyFor( BioAssaySet target ) {
        return new LazyProxy( target );
    }

    private static DifferentialExpressionAnalysis analysis( Long id, BioAssaySet experimentAnalyzed ) {
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( id );
        dea.setExperimentAnalyzed( experimentAnalyzed );
        return dea;
    }

    @Test
    public void initializedExperiment_resolvesToItsShortName() {
        assertThat( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis( 42L, ee( 1L, "GSE123" ) ) ) )
                .isEqualTo( "1_GSE123_diffExpAnalysis_42.zip" );
    }

    @Test
    public void lazyProxiedExperiment_resolvesRatherThanThrowing() {
        BioAssaySet proxy = lazyProxyFor( ee( 1L, "GSE123" ) );
        // guard: the proxy really is opaque, otherwise this test proves nothing
        assertThat( proxy ).isNotInstanceOf( ExpressionExperiment.class );

        assertThat( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis( 42L, proxy ) ) )
                .isEqualTo( "1_GSE123_diffExpAnalysis_42.zip" );
    }

    @Test
    public void initializedSubset_resolvesToItsSourceExperimentShortName() {
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 7L );
        subset.setSourceExperiment( ee( 1L, "GSE123" ) );

        assertThat( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis( 42L, subset ) ) )
                .isEqualTo( "7_GSE123_diffExpAnalysis_42.zip" );
    }

    @Test
    public void lazyProxiedSubset_resolvesToItsSourceExperimentShortName() {
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( 7L );
        subset.setSourceExperiment( ee( 1L, "GSE123" ) );

        assertThat( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis( 42L, lazyProxyFor( subset ) ) ) )
                .isEqualTo( "7_GSE123_diffExpAnalysis_42.zip" );
    }

    @Test
    public void transientAnalysis_omitsTheAnalysisId() {
        assertThat( ExpressionDataFileUtils.getDiffExArchiveFileName( analysis( null, ee( 1L, "GSE123" ) ) ) )
                .isEqualTo( "1_GSE123_diffExpAnalysis.zip" );
    }
}
