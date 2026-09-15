package ubic.gemma.model.analysis;

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
 * Unit tests for {@link SingleExperimentAnalysis#getSecurityOwner()}, the hook the ACL machinery
 * consults to decide which ACL an analysis inherits.
 * <p>
 * The proxy case is the one that matters, and it is the one that fails quietly.
 * {@code experimentAnalyzed} is mapped with {@code targetEntity = BioAssaySet.class}, so Hibernate
 * hands back a {@code BioAssaySet$HibernateProxy} — an instance of neither concrete subclass, which
 * fell through to {@code return null}. A null owner makes
 * {@code AclEventListener.resolveParentAcl} give the analysis its own ROOT ACL instead of one
 * inheriting from the experiment, so the analysis ends up with the wrong visibility and nothing
 * throws.
 *
 * @author claude
 */
public class SingleExperimentAnalysisTest {

    private static ExpressionExperiment ee( Long id ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        ee.setShortName( "GSE" + id );
        return ee;
    }

    private static ExpressionExperimentSubSet subset( Long id, ExpressionExperiment source ) {
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setId( id );
        subset.setSourceExperiment( source );
        return subset;
    }

    private static DifferentialExpressionAnalysis analysis( BioAssaySet experimentAnalyzed ) {
        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setId( 42L );
        dea.setExperimentAnalyzed( experimentAnalyzed );
        return dea;
    }

    /**
     * Stands in for what Hibernate hands back for the {@code experimentAnalyzed} association: a
     * {@link BioAssaySet} that is an instance of neither concrete subclass and resolves to the target
     * only through its {@link LazyInitializer}.
     * <p>
     * A real class rather than a Mockito mock on purpose. {@code Hibernate.unproxy} does not test
     * {@code instanceof HibernateProxy}; it goes through {@code asHibernateProxy()}, a default method
     * that returns {@code this}. A mock stubs that to {@code null}, the proxy passes through
     * unresolved, and the test then passes against the unfixed code. Same stand-in as
     * {@code ExpressionDataFileUtilsTest.LazyProxy}.
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

    @Test
    public void initializedExperiment_isItsOwnSecurityOwner() {
        ExpressionExperiment ee = ee( 1L );
        assertThat( analysis( ee ).getSecurityOwner() ).isSameAs( ee );
    }

    @Test
    public void lazyProxiedExperiment_resolvesRatherThanReturningNull() {
        ExpressionExperiment ee = ee( 1L );
        BioAssaySet proxy = new LazyProxy( ee );
        // guard: the proxy really is opaque, otherwise this test proves nothing
        assertThat( proxy ).isNotInstanceOf( ExpressionExperiment.class );

        assertThat( analysis( proxy ).getSecurityOwner() ).isSameAs( ee );
    }

    @Test
    public void initializedSubset_isOwnedByItsSourceExperiment() {
        ExpressionExperiment source = ee( 1L );
        assertThat( analysis( subset( 7L, source ) ).getSecurityOwner() ).isSameAs( source );
    }

    @Test
    public void lazyProxiedSubset_isOwnedByItsSourceExperiment() {
        ExpressionExperiment source = ee( 1L );
        BioAssaySet proxy = new LazyProxy( subset( 7L, source ) );
        assertThat( proxy ).isNotInstanceOf( ExpressionExperimentSubSet.class );

        assertThat( analysis( proxy ).getSecurityOwner() ).isSameAs( source );
    }

    @Test
    public void noExperimentAnalyzed_hasNoSecurityOwner() {
        assertThat( analysis( null ).getSecurityOwner() ).isNull();
    }
}
