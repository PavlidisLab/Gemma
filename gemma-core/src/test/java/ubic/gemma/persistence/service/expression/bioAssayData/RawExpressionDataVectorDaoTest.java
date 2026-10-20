package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class RawExpressionDataVectorDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class RawExpressionDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public RawExpressionDataVectorDao rawExpressionDataVectorDao( SessionFactory sessionFactory ) {
            return new RawExpressionDataVectorDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Test
    public void testFind() {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 1L );
        QuantitationType qt = new QuantitationType();
        qt.setId( 1L );
        assertThat( rawExpressionDataVectorDao.find( ad, qt ) ).isEmpty();
    }

    @Test
    public void testGetRandomRawVectors() {
        Session session = sessionFactory.getCurrentSession();
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        session.persist( taxon );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "test", taxon );
        ad.setPrimaryTaxon( taxon );
        session.persist( ad );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        session.persist( bad );
        QuantitationType qtA = persistQt( session, "qtA" );
        QuantitationType qtB = persistQt( session, "qtB" );

        // 5 vectors on qtA, 3 on qtB, sharing one EE / platform / dimension.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        for ( int i = 0; i < 5; i++ ) {
            vectors.add( newRawVector( session, ee, ad, bad, qtA, "csA" + i ) );
        }
        for ( int i = 0; i < 3; i++ ) {
            vectors.add( newRawVector( session, ee, ad, bad, qtB, "csB" + i ) );
        }
        ee.setRawExpressionDataVectors( vectors );
        ee.setNumberOfDataVectors( vectors.size() );
        session.persist( ee );
        session.flush();

        // limit < available -> exactly `limit` vectors, all belonging to the requested QT.
        Collection<RawExpressionDataVector> sample = rawExpressionDataVectorDao.getRandomRawVectors( qtA, 3 );
        assertThat( sample ).hasSize( 3 )
                .allSatisfy( v -> assertThat( v.getQuantitationType() ).isEqualTo( qtA ) );

        // limit >= available -> all of the QT's vectors, and none from the other QT.
        assertThat( rawExpressionDataVectorDao.getRandomRawVectors( qtA, 100 ) ).hasSize( 5 )
                .allSatisfy( v -> assertThat( v.getQuantitationType() ).isEqualTo( qtA ) );
        assertThat( rawExpressionDataVectorDao.getRandomRawVectors( qtB, 100 ) ).hasSize( 3 )
                .allSatisfy( v -> assertThat( v.getQuantitationType() ).isEqualTo( qtB ) );

        // limit <= 0 -> empty, even though the QT has vectors.
        assertThat( rawExpressionDataVectorDao.getRandomRawVectors( qtA, 0 ) ).isEmpty();
    }

    @Test
    public void testGetRandomRawVectorsEmptyWhenNoVectors() {
        Session session = sessionFactory.getCurrentSession();
        QuantitationType qt = persistQt( session, "empty" );
        session.flush();
        assertThat( rawExpressionDataVectorDao.getRandomRawVectors( qt, 10 ) ).isEmpty();
    }

    private RawExpressionDataVector newRawVector( Session session, ExpressionExperiment ee, ArrayDesign ad,
            BioAssayDimension bad, QuantitationType qt, String csName ) {
        CompositeSequence cs = CompositeSequence.Factory.newInstance( csName, ad );
        session.persist( cs );
        RawExpressionDataVector ev = new RawExpressionDataVector();
        ev.setDesignElement( cs );
        ev.setBioAssayDimension( bad );
        ev.setQuantitationType( qt );
        ev.setExpressionExperiment( ee );
        ev.setData( new byte[0] );
        return ev;
    }

    private QuantitationType persistQt( Session session, String name ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( name );
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
        session.persist( qt );
        return qt;
    }
}
