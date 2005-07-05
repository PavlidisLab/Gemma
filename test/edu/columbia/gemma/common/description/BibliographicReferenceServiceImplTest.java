package edu.columbia.gemma.common.description;

import org.easymock.MockControl;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.BibliographicReference;
/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceServiceImplTest extends BaseServiceTestCase {

    private BibliographicReferenceServiceImpl svc = null;
    private BibliographicReferenceDao brdao = null;
    private ExternalDatabaseDao eddao = null;
    private DatabaseEntry de = null;
    private ExternalDatabase extDB = null;
    private MockControl controlBR;
    private MockControl controlED;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        svc = new BibliographicReferenceServiceImpl();
        controlBR = MockControl.createControl( BibliographicReferenceDao.class );
        controlED = MockControl.createControl( ExternalDatabaseDao.class );
        
        brdao = ( BibliographicReferenceDao ) controlBR.getMock();
        eddao = ( ExternalDatabaseDao ) controlED.getMock();
        
        svc.setBibliographicReferenceDao(brdao);
        svc.setExternalDatabaseDao(eddao);
        extDB = ExternalDatabase.Factory.newInstance();
        extDB.setName("PUBMED");
        
        de = DatabaseEntry.Factory.newInstance();
        de.setAccession("12345");
        de.setExternalDatabase(extDB);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
    	super.tearDown();
    }

    public final void testFindByExternalId() {
       
        
        BibliographicReference mockBR = BibliographicReference.Factory.newInstance();
        mockBR.setPubAccession(de);
        mockBR.setTitle("My Title");
        brdao.findByExternalId("12345", "PUBMED");
		controlBR.setReturnValue( mockBR );
		
        controlBR.replay();
        BibliographicReference brRet = svc.findByExternalId("12345", "PUBMED");
        controlBR.verify();
    }
    

}