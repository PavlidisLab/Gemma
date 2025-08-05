package ubic.gemma.persistence.service.genome;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
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
    @WithMockUser // needed for in-query ACL checks
    public void testGetCompositeSequences() {
        Taxon taxon = Taxon.Factory.newInstance();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "test", taxon );
        sessionFactory.getCurrentSession().persist( ad );
        Gene g = geneDao.create( Gene.Factory.newInstance() );
        assertThat( geneDao.getCompositeSequences( g, true ) ).isEmpty();
        assertThat( geneDao.getCompositeSequences( g, false ) ).isEmpty();
        assertThat( geneDao.getCompositeSequences( g, ad, true ) ).isEmpty();
        assertThat( geneDao.getCompositeSequences( g, ad, false ) ).isEmpty();
        assertThat( geneDao.getCompositeSequencesById( g.getId(), true ) ).isEmpty();
        assertThat( geneDao.getCompositeSequencesById( g.getId(), false ) ).isEmpty();
        assertThat( geneDao.getCompositeSequenceCount( g, true ) ).isZero();
        assertThat( geneDao.getCompositeSequenceCount( g, false ) ).isZero();
        assertThat( geneDao.getCompositeSequenceCountById( g.getId(), true ) ).isZero();
        assertThat( geneDao.getCompositeSequenceCountById( g.getId(), false ) ).isZero();
        assertThat( geneDao.getCompositeSequenceCountById( g.getId(), true ) ).isZero();
        assertThat( geneDao.getCompositeSequenceCountById( g.getId(), false ) ).isZero();
    }

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