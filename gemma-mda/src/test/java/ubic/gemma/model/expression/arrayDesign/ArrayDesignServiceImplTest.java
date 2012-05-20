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
package ubic.gemma.model.expression.arrayDesign;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * Unit testing of the ArrayDesignService.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignServiceImplTest extends TestCase {

    private ArrayDesignServiceImpl arrayDesignService = new ArrayDesignServiceImpl();
    private ArrayDesignDao arrayDesignDaoMock = null;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        arrayDesignDaoMock = createMock( ArrayDesignDao.class );
        arrayDesignService.setArrayDesignDao( arrayDesignDaoMock );
    }

    /**
     * Uses mock objects to unit test service layer method getAllArrayDesigns().
     * 
     * @throws Exception
     */
    public void testGetAllArrayDesigns() throws Exception {
        // to implement this test, the mock dao has to save several objects.
        Collection<ArrayDesign> m = new HashSet<ArrayDesign>();

        for ( int i = 0; i < 5; i++ ) {
            ArrayDesign tad = ArrayDesign.Factory.newInstance();

            tad.setName( "Foo" + i );
            if ( !m.add( tad ) ) throw new IllegalStateException( "Couldn't add to the collection - check equals" );
        }

        /*
         * Record mode. The expected behaviour is that the arrayDesignService will call
         * arrayDesignDao.getAllArrayDesigns().
         */
        arrayDesignDaoMock.loadAll();
        expectLastCall().andReturn( m );

        /*
         * Playback mode.
         */
        replay( arrayDesignDaoMock );
        arrayDesignService.loadAll();

        /*
         * Verification.
         */
        verify( arrayDesignDaoMock );
    }

    /**
     * @throws Exception
     */
    public void testSaveArrayDesign() throws Exception {
        ArrayDesign tad = ArrayDesign.Factory.newInstance();

        tad.setName( "Foo" );

        /*
         * Record mode. The expected behavior is that the arrayDesignService will call the
         * arrayDesignDao.findOrCreate(arrayDesign) method, so this is what is recorded.
         */
        arrayDesignDaoMock.findOrCreate( tad );
        expectLastCall().andReturn( tad );

        /*
         * Playback mode.
         */
        replay( arrayDesignDaoMock );
        arrayDesignService.findOrCreate( tad );

        /*
         * Verification.
         */
        verify( arrayDesignDaoMock );
    }

}
