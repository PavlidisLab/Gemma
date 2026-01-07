package ubic.gemma.model.expression.bioAssayData;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;
import ubic.gemma.persistence.service.expression.experiment.RandomExpressionExperimentUtils;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class RawExpressionDataVectorTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class RawExpressionDataVectorTestContextConfiguration extends BaseDatabaseTest.BaseDatabaseTestContextConfiguration {

    }

    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        ee = RandomExpressionExperimentUtils.randomExpressionExperiment( taxon, 1, ad );
        for ( BioAssay ba : ee.getBioAssays() ) {
            sessionFactory.getCurrentSession().persist( ba.getSampleUsed() );
        }
        sessionFactory.getCurrentSession().persist( ee );
        QuantitationType qt = new QuantitationType();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        sessionFactory.getCurrentSession().persist( qt );
        BioAssayDimension bad = new BioAssayDimension();
        bad.getBioAssays().addAll( ee.getBioAssays() );
        sessionFactory.getCurrentSession().persist( bad );
        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, bad, ad, qt, RawExpressionDataVector.class );
        ee.getRawExpressionDataVectors().addAll( vectors );
        // this flush is necessary, or else Hibernate will interleave vectors and NOC inserts (see #1578)
        sessionFactory.getCurrentSession().flush();
        vectors.forEach( v -> {
            v.setNumberOfCells( new int[] { 12 } );
        } );
        sessionFactory.getCurrentSession().flush();
    }

    /**
     * This test ensures that the {@link RawExpressionDataVectorNumberOfCells} is properly deleted when orphaned (e.g.
     * when the {@link RawExpressionDataVector#setNumberOfCells(int[])} is called with {@code null}).
     */
    @Test
    public void testNumberOfCellsIsDeletedWhenOrphaned() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ee = ( ExpressionExperiment ) sessionFactory.getCurrentSession().get( ExpressionExperiment.class, ee.getId() );
        RawExpressionDataVector vector = ee.getRawExpressionDataVectors().iterator().next();
        assertThat( vector.getId() ).isNotNull();
        assertThat( vector.getNumberOfCells() );
        RawExpressionDataVectorNumberOfCells noco = vector.getNumberOfCellsObject();
        assertThat( noco ).isNotNull();
        assertThat( noco.getId() ).isEqualTo( vector.getId() );

        vector = ee.getRawExpressionDataVectors().iterator().next();
        assertThat( vector.getNumberOfCells() )
                .containsExactly( 12 );
        noco = vector.getNumberOfCellsObject();

        // should delete the entity via delete-orphan cascade
        vector.setNumberOfCells( null );
        assertThat( sessionFactory.getCurrentSession().contains( noco ) ).isTrue();
        sessionFactory.getCurrentSession().flush();
        assertThat( sessionFactory.getCurrentSession().contains( noco ) ).isFalse();
    }

    @Test
    public void testNumberOfCellsObjectIsReusedWhenAssigned() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ee = ( ExpressionExperiment ) sessionFactory.getCurrentSession().get( ExpressionExperiment.class, ee.getId() );
        RawExpressionDataVector vector = ee.getRawExpressionDataVectors().iterator().next();
        assertThat( vector.getId() ).isNotNull();
        assertThat( vector.getNumberOfCells() );
        RawExpressionDataVectorNumberOfCells noco = vector.getNumberOfCellsObject();
        assertThat( noco ).isNotNull();
        assertThat( noco.getId() ).isEqualTo( vector.getId() );

        vector = ee.getRawExpressionDataVectors().iterator().next();
        assertThat( vector.getNumberOfCells() )
                .containsExactly( 12 );
        noco = vector.getNumberOfCellsObject();

        vector.setNumberOfCells( new int[] { 13 } );
        assertThat( vector.getNumberOfCellsObject() ).isSameAs( noco );
        assertThat( sessionFactory.getCurrentSession().contains( noco ) ).isTrue();
        sessionFactory.getCurrentSession().flush();
        assertThat( sessionFactory.getCurrentSession().contains( noco ) ).isTrue();
    }

    @Test
    public void testDeleteRawVectorsWithNumberOfCells() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        ee = ( ExpressionExperiment ) sessionFactory.getCurrentSession().get( ExpressionExperiment.class, ee.getId() );

        List<RawExpressionDataVectorNumberOfCells> nocs = new ArrayList<>();
        for ( RawExpressionDataVector rawExpressionDataVector : ee.getRawExpressionDataVectors() ) {
            nocs.add( rawExpressionDataVector.getNumberOfCellsObject() );
            sessionFactory.getCurrentSession().delete( rawExpressionDataVector );
        }
        ee.getRawExpressionDataVectors().clear();
        assertThat( nocs ).allSatisfy( sessionFactory.getCurrentSession()::contains );
        sessionFactory.getCurrentSession().flush();
        Long numNocs = ( Long ) sessionFactory.getCurrentSession()
                .createQuery( "select count(*) from RawExpressionDataVectorNumberOfCells v where v.id in :ids" )
                .setParameterList( "ids", IdentifiableUtils.getIds( nocs ) )
                .uniqueResult();
        assertThat( numNocs ).isZero();
        // FIXME: somehow, the NOCs remain in the session even if they've been deleted in cascade
        // assertThat( nocs ).noneSatisfy( sessionFactory.getCurrentSession()::contains );
    }
}