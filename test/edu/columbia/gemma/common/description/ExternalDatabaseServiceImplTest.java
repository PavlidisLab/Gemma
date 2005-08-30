package edu.columbia.gemma.common.description;

import org.easymock.MockControl;

import edu.columbia.gemma.BaseServiceTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExternalDatabaseServiceImplTest extends BaseServiceTestCase {

    /*
     * @see TestCase#setUp()
     */
    private ExternalDatabaseServiceImpl svc = null;
    private ExternalDatabaseDao dao = null;
    private MockControl control;

    protected void setUp() throws Exception {
        super.setUp();
        svc = new ExternalDatabaseServiceImpl();
        control = MockControl.createControl( ExternalDatabaseDao.class );
        dao = ( ExternalDatabaseDao ) control.getMock();
        svc.setExternalDatabaseDao( dao );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testFind() {
        ExternalDatabase m = ExternalDatabase.Factory.newInstance();
        m.setName( "PubMed" );
        dao.findByName( "PubMed" );
        control.setReturnValue( m );

        control.replay(); // switch from record mode to replay
        svc.find( "PubMed" );
        control.verify();
    }

}
