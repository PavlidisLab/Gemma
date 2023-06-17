package ubic.gemma.persistence.service.expression.designElement;

import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.TestComponent;

import static org.junit.Assert.*;

@ContextConfiguration
public class CompositeSequenceDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CompositeSequenceDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CompositeSequenceDao compositeSequenceDao( SessionFactory sessionFactory ) {
            return new CompositeSequenceDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private CompositeSequenceDao compositeSequenceDao;

    private ArrayDesign platform;
    private Gene gene;
    private CompositeSequence cs;

    @Before
    public void setUp() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        platform = new ArrayDesign();
        platform.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( platform );
        cs = new CompositeSequence();
        cs.setArrayDesign( platform );
        sessionFactory.getCurrentSession().persist( cs );
        gene = new Gene();
        sessionFactory.getCurrentSession().persist( gene );
    }

    @Test
    public void testFindByGene() {
        compositeSequenceDao.findByGene( gene );
    }

    @Test
    public void testFindByGeneSlice() {
        Slice<CompositeSequence> slice = compositeSequenceDao.findByGene( gene, 0, 10 );
        assertNull( slice.getSort() );
        assertNotNull( slice.getOffset() );
        assertEquals( 0, ( int ) slice.getOffset() );
        assertNotNull( slice.getLimit() );
        assertEquals( 10, ( int ) slice.getLimit() );
        assertNotNull( slice.getTotalElements() );
    }

    @Test
    public void testFindByGeneAndPlatform() {
        compositeSequenceDao.findByGene( gene, platform );
    }

    @Test
    public void testGetGenes() {
        Slice<Gene> slice = compositeSequenceDao.getGenes( cs, 0, 10 );
        assertNull( slice.getSort() );
        assertNotNull( slice.getOffset() );
        assertEquals( 0, ( int ) slice.getOffset() );
        assertNotNull( slice.getLimit() );
        assertEquals( 10, ( int ) slice.getLimit() );
        assertNotNull( slice.getTotalElements() );
    }
}