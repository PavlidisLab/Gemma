/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.util;

import junit.framework.TestCase;
//import ubic.gemma.model.common.description.ExternalDatabase;
//import ubic.gemma.model.common.description.OntologyEntry;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BeanPropertyCompleterTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
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
