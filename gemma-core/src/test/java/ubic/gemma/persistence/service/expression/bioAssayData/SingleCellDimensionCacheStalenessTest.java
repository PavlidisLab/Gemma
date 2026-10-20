package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.GenericCellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HB6 cascade audit #9 regression guard: SingleCellDimension family (Pattern C —
 * {@code mutable="false"} class + child collection + cache directive).
 * <p>
 * Both {@code SingleCellDimension} and {@code GenericCellLevelCharacteristics}
 * are mapped {@code mutable="false"} with {@code <cache usage="read-only"/>},
 * the same shape as the {@code AuditTrail} / {@code AuditEvent} bug fixed at
 * commit {@code ab8b4c443c}. The full L2 cache-staleness symptom requires the
 * second-level cache enabled, which {@code BaseDatabaseTest5} disables; these
 * tests therefore guard the in-session round-trip (persist → flush → clear →
 * reload) which is the necessary but not sufficient leg of the shape. If the
 * audit ever needs an L2-enabled repro it can extend these by overriding the
 * session factory bean.
 *
 * @see ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDaoTest
 * @see <a href="file:../../../../../../../HIBERNATE6_CASCADE_AUDIT.md">HB6 audit doc, finding #9</a>
 */
@ContextConfiguration
public class SingleCellDimensionCacheStalenessTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class SingleCellDimensionCacheStalenessTestContextConfiguration extends BaseDatabaseTestContextConfiguration {
    }

    /**
     * Pattern C smoke test: a {@code mutable="false"} parent ({@link SingleCellDimension})
     * persisted with empty child sets survives a flush + clear + reload.
     */
    @Test
    public void testEmptySingleCellDimensionRoundTrip() {
        SingleCellDimension scd = newSingleCellDimension();
        sessionFactory.getCurrentSession().persist( scd );
        Long id = scd.getId();
        assertThat( id ).isNotNull();

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        SingleCellDimension reloaded = sessionFactory.getCurrentSession().get( SingleCellDimension.class, id );
        assertThat( reloaded ).isNotNull();
        assertThat( reloaded.getNumberOfCellIds() ).isEqualTo( scd.getNumberOfCellIds() );
        assertThat( reloaded.getCellIds() ).isEqualTo( scd.getCellIds() );
        assertThat( reloaded.getCellLevelCharacteristics() ).isEmpty();
        assertThat( reloaded.getCellTypeAssignments() ).isEmpty();
    }

    /**
     * Pattern C primary repro: persist an SCD with a non-empty {@code cellLevelCharacteristics}
     * set (cascade=all-delete-orphan) where each {@link GenericCellLevelCharacteristics}
     * itself is {@code mutable="false"} with a {@code mutable="false"} {@code characteristics}
     * list. After flush + clear + reload the children must be visible. This is the
     * AuditTrail/AuditEvent shape one level deeper.
     */
    @Test
    public void testSingleCellDimensionWithCellLevelCharacteristicsRoundTrip() {
        SingleCellDimension scd = newSingleCellDimension();

        Characteristic c1 = Characteristic.Factory.newInstance( "category", "categoryUri", "value", "valueUri" );
        Characteristic c2 = Characteristic.Factory.newInstance( "category", "categoryUri", "other", "otherUri" );
        List<Characteristic> chars = new ArrayList<>( Arrays.asList( c1, c2 ) );
        GenericCellLevelCharacteristics clc = ( GenericCellLevelCharacteristics ) CellLevelCharacteristics.Factory.newInstance(
                "my-clc", "test clc", chars, new int[] { 0, 1, 0 } );
        scd.getCellLevelCharacteristics().add( clc );

        sessionFactory.getCurrentSession().persist( scd );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        SingleCellDimension reloaded = sessionFactory.getCurrentSession().get( SingleCellDimension.class, scd.getId() );
        assertThat( reloaded ).isNotNull();
        // cellLevelCharacteristics is lazy in the HBM, so it should not be initialized on the bare get()
        assertThat( Hibernate.isInitialized( reloaded.getCellLevelCharacteristics() ) ).isFalse();
        // Triggering it must return the previously-persisted CLC. If the L2 / collection cache
        // ever served a stale empty bag (the AuditTrail bug shape) this would assert against an empty set.
        assertThat( reloaded.getCellLevelCharacteristics() )
                .hasSize( 1 )
                .allSatisfy( reloadedClc -> {
                    assertThat( reloadedClc.getName() ).isEqualTo( "my-clc" );
                    assertThat( reloadedClc.getNumberOfCharacteristics() ).isEqualTo( 2 );
                    // characteristics is mutable="false" + lazy=false, must materialize the same two rows
                    assertThat( reloadedClc.getCharacteristics() )
                            .hasSize( 2 )
                            .extracting( Characteristic::getValue )
                            .containsExactlyInAnyOrder( "value", "other" );
                } );
    }

    /**
     * Standalone {@link GenericCellLevelCharacteristics} create + reload, exercising the
     * {@code mutable="false"} + cache shape on the child class directly. The
     * {@code characteristics} list is itself {@code mutable="false"}, so we set it at
     * construction time (the only legal write).
     */
    @Test
    public void testGenericCellLevelCharacteristicsRoundTrip() {
        Characteristic c = Characteristic.Factory.newInstance( "category", "categoryUri", "v", "vUri" );
        GenericCellLevelCharacteristics clc = ( GenericCellLevelCharacteristics ) CellLevelCharacteristics.Factory.newInstance(
                "solo-clc", null, new ArrayList<>( Collections.singletonList( c ) ), new int[] { 0 } );
        sessionFactory.getCurrentSession().persist( clc );
        Long id = clc.getId();
        assertThat( id ).isNotNull();

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        GenericCellLevelCharacteristics reloaded = sessionFactory.getCurrentSession()
                .get( GenericCellLevelCharacteristics.class, id );
        assertThat( reloaded ).isNotNull();
        assertThat( reloaded.getName() ).isEqualTo( "solo-clc" );
        assertThat( reloaded.getNumberOfCharacteristics() ).isEqualTo( 1 );
        assertThat( reloaded.getCharacteristics() )
                .hasSize( 1 )
                .first()
                .extracting( Characteristic::getValue )
                .isEqualTo( "v" );
    }

    /**
     * Minimal SCD with empty {@code bioAssays} list. {@code bioAssaysOffset} and
     * {@code cellIds} are NOT-NULL, so seed them with single-element placeholders.
     */
    private static SingleCellDimension newSingleCellDimension() {
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( new ArrayList<>( Collections.singletonList( "cell1" ) ) );
        scd.setNumberOfCellIds( 1 );
        scd.setBioAssaysOffset( new int[] { 0 } );
        return scd;
    }
}
