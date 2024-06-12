package ubic.gemma.model.genome.biosequence;

import junit.framework.TestCase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author Paul
 */
public class BioSequenceTest extends TestCase {

    public void testEqualsID() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setId( 10L );
        b.setId( 10L );

        a.setName( "foo" );
        b.setName( "goo" );

        TestCase.assertTrue( a.equals( b ) );
    }

    public void testEqualsName() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "goo" );

        TestCase.assertTrue( !a.equals( b ) );
    }

    public void testEqualsSeq() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "foo" );

        a.setSequence( "AAAAAAAAAAAAAAAA" );
        b.setSequence( "BBBBBBBBBBBBB" );

        TestCase.assertTrue( !a.equals( b ) );
    }

    public void testEqualsTaxon() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "foo" );

        a.setSequence( "AAAAAAAAAAAAAAAA" );
        b.setSequence( "AAAAAAAAAAAAAAAA" );

        Taxon m = Taxon.Factory.newInstance();
        m.setIsGenesUsable( true );
        m.setScientificName( "Mus musculus" );

        Taxon h = Taxon.Factory.newInstance();
        h.setIsGenesUsable( true );
        h.setScientificName( "Homo sapiens" );

        a.setTaxon( m );
        b.setTaxon( h );

        TestCase.assertTrue( !a.equals( b ) );
    }

    public void testNotEqualsID() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setId( 10L );
        b.setId( 111L );

        a.setName( "foo" );
        b.setName( "foo" );

        TestCase.assertTrue( !a.equals( b ) );
    }
}
