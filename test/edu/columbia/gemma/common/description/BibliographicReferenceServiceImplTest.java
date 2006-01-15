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
package edu.columbia.gemma.common.description;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * 
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BibliographicReferenceServiceImplTest extends TestCase {

    private BibliographicReferenceServiceImpl svc = null;
    private BibliographicReferenceDao brdao = null;
    private ExternalDatabaseDao eddao = null;
    private DatabaseEntry de = null;
    private ExternalDatabase extDB = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        svc = new BibliographicReferenceServiceImpl();

        brdao = createMock( BibliographicReferenceDao.class );
        eddao = createMock( ExternalDatabaseDao.class );

        svc.setBibliographicReferenceDao( brdao );
        svc.setExternalDatabaseDao( eddao );
        extDB = ExternalDatabase.Factory.newInstance();
        extDB.setName( "PUBMED" );

        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "12345" );
        de.setExternalDatabase( extDB );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testFindByExternalId() {

        BibliographicReference mockBR = BibliographicReference.Factory.newInstance();
        mockBR.setPubAccession( de );
        mockBR.setTitle( "My Title" );
        brdao.findByExternalId( "12345", "PUBMED" );
        expectLastCall().andReturn( mockBR );

        replay( brdao );
        svc.findByExternalId( "12345", "PUBMED" );
        verify( brdao );
    }

}