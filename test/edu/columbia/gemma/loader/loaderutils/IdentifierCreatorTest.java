package edu.columbia.gemma.loader.loaderutils;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.Identifiable;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class IdentifierCreatorTest extends BaseDAOTestCase {

    Identifiable a;
    Identifiable c;
    Identifiable d;
    Identifiable e;
    Identifiable f;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        a = ArrayDesign.Factory.newInstance();
        a.setName( "Foo" );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testCreate() {
        String actualReturn = IdentifierCreator.create( a, ctx );
        
        String expectedReturn = "ArrayDesign:Foo";
        assertEquals( expectedReturn, actualReturn );
    }

}
