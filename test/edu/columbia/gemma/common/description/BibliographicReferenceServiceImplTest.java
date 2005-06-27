package edu.columbia.gemma.common.description;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceServiceImplTest extends BaseDAOTestCase {

    private BibliographicReferenceService svc = null;
    private BibliographicReferenceDao brdao = null;

    protected void setUp() throws Exception {
        super.setUp();
        svc = ( BibliographicReferenceService ) ctx.getBean( "bibliographicReferenceService" );
        brdao = ( BibliographicReferenceDao ) ctx.getBean( "bibliographicReferenceDao" );
        

    }

    protected void tearDown() throws Exception {
 
    }

    public final void testFindByExternalId() {

		BibliographicReference br = svc.findByExternalId("12345", "PUBMED");
        if( br==null ){
        	br = svc.createBibliographicReferenceByLookup("12345", "PUBMED");
        }
       
        assertTrue(br.getPubAccession().getAccession().equals("12345"));
    	assertTrue(br.getPubAccession().getExternalDatabase().getName().equals("PUBMED"));
        
    }

}