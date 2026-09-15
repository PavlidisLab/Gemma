package ubic.gemma.persistence.service.expression.bioAssayData;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HB6 cascade audit #10 regression guard: {@code BioAssayDimension} HBM (Pattern C
 * — {@code mutable="false"} class + child list + cache directive).
 * <p>
 * {@code BioAssayDimension} is mapped {@code mutable="false"} with
 * {@code <cache usage="read-only"/>} and a {@code <cache usage="read-write"/>}
 * on the {@code bioAssays} list — the same AuditTrail / AuditEvent bug shape
 * (commit {@code ab8b4c443c}). The full L2 cache-staleness symptom requires
 * second-level caching, which {@code BaseDatabaseTest5} disables; this test
 * guards the in-session persist + flush + clear + reload round-trip which is the
 * basic Pattern C smoke check. The 1 BAD-cited failure from the EE-DAO 18-failure
 * set (commit {@code c99be75b47}) was a cascade-walk issue, not a L2 staleness
 * issue, and is incidentally already covered by the now-green
 * {@code ExpressionExperimentDaoTest.testReplaceRawDataVectorsWithNewDimension}.
 *
 * @see <a href="file:../../../../../../../HIBERNATE6_CASCADE_AUDIT.md">HB6 audit doc, finding #10</a>
 */
@ContextConfiguration
public class BioAssayDimensionCacheStalenessTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class BioAssayDimensionCacheStalenessTestContextConfiguration extends BaseDatabaseTestContextConfiguration {
    }

    /**
     * A {@code mutable="false"} {@link BioAssayDimension} with an empty bioAssays list
     * persists and survives a flush + clear + reload.
     */
    @Test
    public void testEmptyBioAssayDimensionRoundTrip() {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setMerged( false );
        sessionFactory.getCurrentSession().persist( bad );
        Long id = bad.getId();
        assertThat( id ).isNotNull();

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        BioAssayDimension reloaded = sessionFactory.getCurrentSession().get( BioAssayDimension.class, id );
        assertThat( reloaded ).isNotNull();
        assertThat( reloaded.getId() ).isEqualTo( id );
        assertThat( reloaded.getMerged() ).isFalse();
        // bioAssays is fetch="select" lazy="false" — must be materialized as an empty list on reload.
        assertThat( reloaded.getBioAssays() ).isEmpty();
    }

    /**
     * Reloading the same {@code mutable="false"} {@link BioAssayDimension} twice
     * after flush + clear must give equivalent state both times. This is the minimum
     * guard that the in-session cache invalidation path is not serving a stale
     * snapshot of a {@code mutable="false"} entity post-clear.
     */
    @Test
    public void testRepeatedReloadOfBioAssayDimension() {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setMerged( true );
        sessionFactory.getCurrentSession().persist( bad );
        Long id = bad.getId();

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        BioAssayDimension first = sessionFactory.getCurrentSession().get( BioAssayDimension.class, id );
        assertThat( first.getMerged() ).isTrue();
        assertThat( first.getBioAssays() ).isEmpty();

        sessionFactory.getCurrentSession().clear();
        BioAssayDimension second = sessionFactory.getCurrentSession().get( BioAssayDimension.class, id );
        assertThat( second.getMerged() ).isTrue();
        assertThat( second.getBioAssays() ).isEmpty();
        assertThat( second ).isNotSameAs( first );
    }
}
