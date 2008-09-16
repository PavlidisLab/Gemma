package ubic.gemma.model.util;


import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BeanPropertyCompleterTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testComplete() throws Exception {

//        OntologyEntry a = OntologyEntry.Factory.newInstance();
//        OntologyEntry b = OntologyEntry.Factory.newInstance();
//
//        a.setCategory( "foo" );
//        a.setValue( "bar" );
//
//        ExternalDatabase d = ExternalDatabase.Factory.newInstance();
//        d.setName( "dbfoo" );
//
//        b.setCategory( "foo" );
//        b.setValue( "bar" );
//        b.setExternalDatabase( d );
//
//        BeanPropertyCompleter.complete( a, b, false );
//
//        assertTrue( a.getExternalDatabase() == d );

    }

    public final void testCompleteUpdate() throws Exception {

//        OntologyEntry a = OntologyEntry.Factory.newInstance();
//        OntologyEntry b = OntologyEntry.Factory.newInstance();
//
//        a.setCategory( "foo" );
//        a.setValue( "bar" );
//
//        ExternalDatabase d = ExternalDatabase.Factory.newInstance();
//        d.setName( "dbfoo" );
//
//        b.setCategory( "foo" );
//        b.setValue( "barbie" );
//        b.setExternalDatabase( d );
//
//        BeanPropertyCompleter.complete( a, b, true );
//
//        assertTrue( a.getValue().equals( "barbie" ) );

    }

    public final void testCompleteNullVals() throws Exception {

//        OntologyEntry a = OntologyEntry.Factory.newInstance();
//        OntologyEntry b = OntologyEntry.Factory.newInstance();
//
//        a.setCategory( "foo" );
//        a.setValue( "bar" );
//
//        ExternalDatabase d = ExternalDatabase.Factory.newInstance();
//        d.setName( "dbfoo" );
//
//        b.setCategory( null );
//        b.setValue( "barbie" );
//        b.setExternalDatabase( d );
//
//        BeanPropertyCompleter.complete( a, b, true );
//
//        assertTrue( a.getCategory().equals( "foo" ) );

    }

    public final void testCompleteUnsameType() throws Exception {
        // try {
        // BeanPropertyCompleter.complete( new String(), new Double( 1.0 ), false );
        // fail( "Should have thrown an exception" );
        // } catch ( IllegalArgumentException e ) {
        //
        //        }
    }

}
