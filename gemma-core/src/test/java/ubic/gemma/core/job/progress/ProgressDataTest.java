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
package ubic.gemma.core.job.progress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.job.progress.ProgressData;

import static org.junit.Assert.assertEquals;

/**
 * @author klc
 *
 */
public class ProgressDataTest {

    private ProgressData pd;

    @Before
    public void setUp() {

        pd = new ProgressData( "12344", 1, "test", false );

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.ProgressData(int, String, boolean)'
     */
    @Test
    public void testProgressData() {
        assertEquals( pd.getPercent(), 1 );
        assertEquals( pd.getDescription(), "test" );
        assertEquals( pd.isDone(), false );

    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDescription(String)'
     */
    @Test
    public void testSetDescription() {
        pd.setDescription( "testDescription" );
        assertEquals( "testDescription", pd.getDescription() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setDone(boolean)'
     */
    @Test
    public void testSetDone() {
        pd.setDone( true );
        assert ( pd.isDone() );
    }

    /*
     * Test method for 'ubic.gemma.web.util.progress.ProgressData.setPercent(int)'
     */
    @Test
    public void testSetPercent() {
        pd.setPercent( 88 );
        assertEquals( pd.getPercent(), 88 );

    }

    @After
    public void tearDown() {
        pd = null;
    }

}
