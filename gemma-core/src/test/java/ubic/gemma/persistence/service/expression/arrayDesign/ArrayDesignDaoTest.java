package ubic.gemma.persistence.service.expression.arrayDesign;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.util.TestComponent;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ArrayDesignDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ArrayDesignDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }

        @Bean
        public CurationDetailsDao curationDetailsDao() {
            return mock( CurationDetailsDao.class );
        }
    }

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Test
    public void testThaw() {
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad.setAuditTrail( new AuditTrail() );
        ad = arrayDesignDao.create( ad );

        Set<CompositeSequence> probes = new HashSet<>();
        for ( int i = 0; i < 20000; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + i, ad );
            BioSequence bs = BioSequence.Factory.newInstance( "s" + i, taxon );
            sessionFactory.getCurrentSession().persist( bs );
            cs.setBiologicalCharacteristic( bs );
            probes.add( cs );
        }
        ad.setCompositeSequences( probes );
        arrayDesignDao.update( ad );

        ArrayDesign thawedAd = arrayDesignDao.thaw( ad );
        assertSame( ad, thawedAd );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        ad = arrayDesignDao.load( ad.getId() );
        assertNotNull( ad );
        assertFalse( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        ad = arrayDesignDao.thaw( ad );
        assertNotNull( ad );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences() ) );
        assertTrue( Hibernate.isInitialized( ad.getCompositeSequences().iterator().next().getBiologicalCharacteristic() ) );
        assertEquals( 20000, ad.getCompositeSequences().size() );

        sessionFactory.getCurrentSession().update( ad );
        sessionFactory.getCurrentSession().flush();
    }
}