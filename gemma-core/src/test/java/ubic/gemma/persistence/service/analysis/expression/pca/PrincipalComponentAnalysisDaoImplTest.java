package ubic.gemma.persistence.service.analysis.expression.pca;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.pca.Eigenvalue;
import ubic.gemma.model.analysis.expression.pca.Eigenvector;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.List;

@ContextConfiguration
public class PrincipalComponentAnalysisDaoImplTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class PCACC extends BaseDatabaseTestContextConfiguration {
        @Bean
        public PrincipalComponentAnalysisDao principalComponentAnalysisDao( SessionFactory sessionFactory ) {
            return new PrincipalComponentAnalysisDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Test
    public void testBulkDelete() {
        Taxon t = new Taxon();
        sessionFactory.getCurrentSession().persist( t );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( t );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setArrayDesign( ad );
            cs.setName( "cs" + i );
            ad.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        List<CompositeSequence> cs = new ArrayList<>( ad.getCompositeSequences() );
        PrincipalComponentAnalysis pca = new PrincipalComponentAnalysis();
        for ( int i = 0; i < 100; i++ ) {
            Eigenvalue ev = new Eigenvalue();
            ev.setComponentNumber( i );
            ev.setValue( 1.0 );
            pca.getEigenValues().add( ev );
            Eigenvector evc = new Eigenvector();
            evc.setComponentNumber( i );
            evc.setVector( new byte[0] );
            pca.getEigenVectors().add( evc );
            ProbeLoading pl = new ProbeLoading();
            pl.setComponentNumber( i );
            pl.setLoading( 1.0 );
            pl.setLoadingRank( 1 );
            pl.setProbe( cs.get( i ) );
            pca.getProbeLoadings().add( pl );
        }
        pca = principalComponentAnalysisDao.create( pca );
        sessionFactory.getCurrentSession().flush();
        principalComponentAnalysisDao.remove( pca );
        sessionFactory.getCurrentSession().flush();
    }
}