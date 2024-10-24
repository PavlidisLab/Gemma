package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ContextConfiguration
public class QuantitationTypeDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class QuantitationTypeDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public QuantitationTypeDao quantitationTypeDao( SessionFactory sessionFactory ) {
            return new QuantitationTypeDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Test
    public void testFind() {
        QuantitationType qt = createQuantitationType();
        assertThat( quantitationTypeDao.find( qt ) )
                .isEqualTo( qt );
    }

    @Test
    public void testFindByVectorType() {
        ArrayDesign ad = createPlatform();

        QuantitationType qt = createQuantitationType();
        ExpressionExperiment ee = new ExpressionExperiment();

        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );

        sessionFactory.getCurrentSession().persist( ee );
        assertThat( quantitationTypeDao.find( qt, RawExpressionDataVector.class ) )
                .isEqualTo( qt );
    }

    @Test
    public void testLoadValueObjects() {
        Filters filters = Filters.by( Filter.parse( null, "name", String.class, Filter.Operator.eq, "FPKM" ) );
        assertThat( quantitationTypeDao.loadValueObjects( filters, null ) ).isEmpty();
    }

    @Test
    public void testFindAllByExperiment() {
        ArrayDesign ad = createPlatform();

        QuantitationType qt = createQuantitationType();
        ExpressionExperiment ee = new ExpressionExperiment();

        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );

        sessionFactory.getCurrentSession().persist( ee );

        assertThat( quantitationTypeDao.findByExpressionExperiment( ee ) ).containsExactly( qt );
    }

    @Test
    public void testLoadByIdAndVectorType() {
        ArrayDesign ad = createPlatform();

        QuantitationType qt = createQuantitationType();
        ExpressionExperiment ee = new ExpressionExperiment();

        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );

        sessionFactory.getCurrentSession().persist( ee );
        assertThat( quantitationTypeDao.loadByIdAndVectorType( qt.getId(), ee, RawExpressionDataVector.class ) )
                .isEqualTo( qt );
    }

    @Test
    public void testFindByNameAndVectorType() {
        ArrayDesign ad = createPlatform();

        ExpressionExperiment ee = new ExpressionExperiment();

        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );

        QuantitationType qt = createQuantitationType();
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );

        sessionFactory.getCurrentSession().persist( ee );

        assertThat( quantitationTypeDao.findByNameAndVectorType( ee, "test", RawExpressionDataVector.class ) )
                .isEqualTo( qt );
    }

    @Test
    public void testFindByNameWhenNameIsNonUnique() {
        ArrayDesign ad = createPlatform();

        ExpressionExperiment ee = new ExpressionExperiment();

        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );

        QuantitationType qt = createQuantitationType();
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );

        QuantitationType qt2 = createQuantitationType();
        RawExpressionDataVector vector2 = new RawExpressionDataVector();
        vector2.setBioAssayDimension( bad );
        vector2.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector2.setQuantitationType( qt2 );
        vector2.setData( new byte[0] );
        vector2.setExpressionExperiment( ee );

        ee.getQuantitationTypes().add( qt2 );
        ee.getRawExpressionDataVectors().add( vector2 );

        sessionFactory.getCurrentSession().persist( ee );

        assertThatThrownBy( () -> quantitationTypeDao.findByNameAndVectorType( ee, "test", RawExpressionDataVector.class ) )
                .isInstanceOf( NonUniqueResultException.class );
    }

    @Test
    public void testGetVectorType() {
        ArrayDesign ad = createPlatform();
        ExpressionExperiment ee = new ExpressionExperiment();
        BioAssayDimension bad = new BioAssayDimension();
        sessionFactory.getCurrentSession().persist( bad );
        QuantitationType qt = createQuantitationType();
        RawExpressionDataVector vector = new RawExpressionDataVector();
        vector.setBioAssayDimension( bad );
        vector.setDesignElement( ad.getCompositeSequences().iterator().next() );
        vector.setQuantitationType( qt );
        vector.setData( new byte[0] );
        vector.setExpressionExperiment( ee );
        ee.getQuantitationTypes().add( qt );
        ee.getRawExpressionDataVectors().add( vector );
        sessionFactory.getCurrentSession().persist( ee );
        // attach some vectors to it
        assertThat( quantitationTypeDao.getVectorType( qt ) ).isEqualTo( RawExpressionDataVector.class );
    }

    private ArrayDesign createPlatform() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        CompositeSequence cs = new CompositeSequence();
        cs.setArrayDesign( ad );
        ad.getCompositeSequences().add( cs );
        sessionFactory.getCurrentSession().persist( ad );
        return ad;
    }

    private QuantitationType createQuantitationType() {
        QuantitationType newQt = new QuantitationType();
        newQt.setName( "test" );
        newQt.setGeneralType( GeneralType.QUANTITATIVE );
        newQt.setType( StandardQuantitationType.AMOUNT );
        newQt.setScale( ScaleType.LOG2 );
        newQt.setRepresentation( PrimitiveType.DOUBLE );
        sessionFactory.getCurrentSession().persist( newQt );
        return newQt;
    }
}