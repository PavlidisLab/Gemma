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
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExternalDatabaseServiceImplTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    private ExternalDatabaseServiceImpl svc = null;
    private ExternalDatabaseDao dao = null;

    protected void setUp() throws Exception {
        super.setUp();
        svc = new ExternalDatabaseServiceImpl();
        dao = createMock( ExternalDatabaseDao.class );
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
        expectLastCall().andReturn( m );

        replay( dao ); // switch from record mode to replay
        svc.find( "PubMed" );
        verify( dao );
    }

}
