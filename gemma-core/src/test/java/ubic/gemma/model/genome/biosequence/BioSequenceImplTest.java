/**
 * 
 */
package ubic.gemma.model.genome.biosequence;

import junit.framework.TestCase;
import ubic.gemma.model.genome.Taxon;

/**
 * @author Paul
 * @version $Id$
 */
public class BioSequenceImplTest extends TestCase {

    public void testEqualsID() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setId( 10L );
        b.setId( 10L );

        a.setName( "foo" );
        b.setName( "goo" );

        assertTrue( a.equals( b ) );
    }

    public void testEqualsName() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "goo" );

        assertTrue( !a.equals( b ) );
    }

    public void testEqualsSeq() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "foo" );

        a.setSequence( "AAAAAAAAAAAAAAAA" );
        b.setSequence( "BBBBBBBBBBBBB" );

        assertTrue( !a.equals( b ) );
    }

    public void testEqualsTaxon() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setName( "foo" );
        b.setName( "foo" );

        a.setSequence( "AAAAAAAAAAAAAAAA" );
        b.setSequence( "AAAAAAAAAAAAAAAA" );

        Taxon m = Taxon.Factory.newInstance();
        m.setIsSpecies( true );
        m.setIsGenesUsable( true );
        m.setScientificName( "Mus musculus" );

        Taxon h = Taxon.Factory.newInstance();
        h.setIsSpecies( true );
        h.setIsGenesUsable( true );
        h.setScientificName( "Homo sapiens" );

        a.setTaxon( m );
        b.setTaxon( h );

        assertTrue( !a.equals( b ) );
    }

    public void testNotEqualsID() {
        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();

        a.setId( 10L );
        b.setId( 111L );

        a.setName( "foo" );
        b.setName( "foo" );

        assertTrue( !a.equals( b ) );
    }
}
