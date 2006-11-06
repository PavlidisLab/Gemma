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
package ubic.gemma.util.progress;

import junit.framework.TestCase;

/**
 * @author klc
 * @version $Id$
 */
public class ProgressDataTest extends TestCase {

    protected ProgressData pd;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pd = new ProgressData( 1, "test", false );

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        pd = null;

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.ProgressData(int, String, boolean)'
     */
    public void testProgressData() {

        assertEquals( pd.getPercent(), 1 );
        assertEquals( pd.getDescription(), "test" );
        assertEquals( pd.isDone(), false );

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDone(boolean)'
     */
    public void testSetDone() {
        pd.setDone( true );
        assert ( pd.isDone() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDescription(String)'
     */
    public void testSetDescription() {
        pd.setDescription( "testDescription" );
        assertEquals( "testDescription", pd.getDescription() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setPercent(int)'
     */
    public void testSetPercent() {
        pd.setPercent( 88 );
        assertEquals( pd.getPercent(), 88 );

    }

}
