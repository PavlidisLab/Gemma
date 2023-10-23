package ubic.gemma.persistence.service.genome;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class GeneDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class GeneDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public GeneDao geneDao( SessionFactory sessionFactory ) {
            return new GeneDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private GeneDao geneDao;

    @Test
    public void testRemove() {
        Gene g = geneDao.create( Gene.Factory.newInstance() );
        geneDao.remove( g );
    }

    @Test
    public void testRemoveWithDummyGeneProducts() {
        Gene g = geneDao.create( Gene.Factory.newInstance() );
        GeneProduct gp = new GeneProduct();
        gp.setDummy( true );
        gp.setGene( g );
        sessionFactory.getCurrentSession().persist( gp );
        g = reload( g );
        assertThat( g.getProducts() ).doesNotContain( gp );
        geneDao.remove( g );
        sessionFactory.getCurrentSession().flush();
    }

    private Gene reload( Gene g ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( g );
        return ( Gene ) sessionFactory.getCurrentSession().get( Gene.class, g.getId() );
    }
}