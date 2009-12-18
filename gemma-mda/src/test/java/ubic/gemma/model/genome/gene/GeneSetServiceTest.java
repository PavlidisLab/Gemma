package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.testing.BaseSpringContextTest;

public class GeneSetServiceTest extends BaseSpringContextTest {

    private Gene g = null;
    private Gene g2 = null;
    private Gene g3 = null;

    @Autowired
    GeneSetService geneSetService;

    @Before
    public void setUp() throws Exception {

        g = this.getTestPeristentGene();
        g2 = this.getTestPeristentGene();
        g3 = this.getTestPeristentGene();

    }

    @Test
    public void testCreate() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( g );
        gmember.setScore( 0.22 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "CreateTest" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );
        
        geneSetService.remove( gset );
    }

    @Test
    public void testRemove() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "DeleteTest" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        geneSetService.remove( gset );

        assert ( geneSetService.load( gset.getId() ) == null );

    }

    @Test
    public void testUpdate() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "Update Test" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g3 );
        gmember.setScore( 0.66 );
        gset.getGeneSetMembers().add( gmember );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 1 );

        geneSetService.update( gset );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 2 );
        
        geneSetService.remove( gset );

    }

    // FIXME I thought this test would fail but it passes. Our API lets us add the same geneMember to the same set twice.
    // Thought the HashSet would reject this.
    
    @Test
    public void testUpdateAddingSameGeneMemberTwice() {

        Collection<GeneSetMember> gsMembers = new HashSet<GeneSetMember>();
        GeneSetMember gmember = new GeneSetMemberImpl();
        gmember.setGene( this.g2 );
        gmember.setScore( 0.33 );

        gsMembers.add( gmember );

        GeneSet gset = new GeneSetImpl();
        gset.setName( "testUpdateAddingSameGeneMemberTwice" );
        gset.setGeneSetMembers( gsMembers );

        gset = geneSetService.create( gset );

        assert ( gset.equals( geneSetService.load( gset.getId() ).getId() ) );

        gset.getGeneSetMembers().add( gset.getGeneSetMembers().iterator().next() );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 1 );

        geneSetService.update( gset );

        assert ( geneSetService.load( gset.getId() ).getGeneSetMembers().size() == 2 );
        
        geneSetService.remove( gset );

    }

}
