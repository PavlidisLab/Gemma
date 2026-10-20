package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration
public class ExpressionExperimentSubSetDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ExpressionExperimentSubSetDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentSubSetDao expressionExperimentSubSetDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentSubSetDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentSubSetDao dao;

    private Taxon taxon;
    private ArrayDesign ad;

    @BeforeEach
    public void setUp() {
        taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
    }

    @Test
    public void testRemove() {
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.getBioAssays().add( ba );
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertTrue( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
    }

    @Test
    public void testRemoveWhenBioAssayIsOwnedByTheSubset() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertFalse( sessionFactory.getCurrentSession().contains( ba ) );
        assertFalse( sessionFactory.getCurrentSession().contains( bm ) );
    }

    @Test
    public void testRemoveWhenBioMaterialIsUsedByAnotherBioAssay() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        BioAssay ba2 = new BioAssay();
        ba2.setArrayDesignUsed( ad );
        ba2.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba2 );
        sessionFactory.getCurrentSession().persist( ba2 );
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertFalse( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( ba2 ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
    }

    /**
     * An assay can sit in a cell-type subset and in a further subset of it. The subset join table restricts on
     * delete, so deleting the assay out from under the other subset aborts the whole removal.
     */
    @Test
    public void testRemoveWhenBioAssayIsUsedByAnotherSubSet() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        ExpressionExperimentSubSet other = new ExpressionExperimentSubSet();
        other.setSourceExperiment( ee );
        other.getBioAssays().add( ba );
        other = dao.create( other );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        sessionFactory.getCurrentSession().flush();

        dao.remove( subset );
        sessionFactory.getCurrentSession().flush();

        assertTrue( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
        assertTrue( sessionFactory.getCurrentSession().contains( other ) );
        assertTrue( other.getBioAssays().contains( ba ) );
    }

    /**
     * A dimension that indexes no data is garbage once the subset that owns its assays is gone; leaving it behind
     * strands the assays forever, since the guard below would then refuse to ever delete them.
     */
    @Test
    public void testRemoveWhenBioAssayIsInADimensionThatIndexesNoData() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.getBioAssays().add( ba );
        sessionFactory.getCurrentSession().persist( bad );
        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertFalse( sessionFactory.getCurrentSession().contains( ba ) );
        assertFalse( sessionFactory.getCurrentSession().contains( bm ) );
        assertFalse( sessionFactory.getCurrentSession().contains( bad ) );
    }

    /**
     * The reverse: vectors index their values by the assay's position in the dimension, so neither the assay nor the
     * dimension may be touched.
     */
    @Test
    public void testRemoveWhenBioAssayIsInADimensionThatIndexesData() {
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );
        BioMaterial bm = new BioMaterial();
        bm.setSourceTaxon( taxon );
        sessionFactory.getCurrentSession().persist( bm );
        BioAssay ba = new BioAssay();
        ba.setArrayDesignUsed( ad );
        ba.setSampleUsed( bm );
        bm.getBioAssaysUsedIn().add( ba );
        sessionFactory.getCurrentSession().persist( ba );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.getBioAssays().add( ba );
        sessionFactory.getCurrentSession().persist( bad );

        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "qt" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsPreferred( false );
        qt.setIsRatio( false );
        qt.setIsMaskedPreferred( false );
        sessionFactory.getCurrentSession().persist( qt );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs", ad );
        sessionFactory.getCurrentSession().persist( cs );
        RawExpressionDataVector ev = new RawExpressionDataVector();
        ev.setDesignElement( cs );
        ev.setBioAssayDimension( bad );
        ev.setQuantitationType( qt );
        ev.setExpressionExperiment( ee );
        ev.setData( new byte[0] );
        sessionFactory.getCurrentSession().persist( ev );
        sessionFactory.getCurrentSession().flush();

        ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
        subset.setSourceExperiment( ee );
        subset.getBioAssays().add( ba );
        subset = dao.create( subset );
        dao.remove( subset );
        assertTrue( sessionFactory.getCurrentSession().contains( ba ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bm ) );
        assertTrue( sessionFactory.getCurrentSession().contains( bad ) );
    }
}