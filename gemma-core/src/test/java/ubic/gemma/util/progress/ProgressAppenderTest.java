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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * For this test to work you must have the appender configured!
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressAppenderTest extends BaseSpringContextTest {

    // important for this test!
    private static Log log = LogFactory.getLog( ProgressAppenderTest.class.getName() );

    public void testProgressLogging() throws Exception {

        ProgressJob job = ProgressManager.createProgressJob( "foo", "testing" );

        String expectedValue = "la de da";
        log.info( expectedValue );
        assertEquals( expectedValue, job.getProgressData().getDescription() );

        log.debug( "pay no attention" ); // should not update the description.
        assertEquals( expectedValue, job.getProgressData().getDescription() );

        log.warn( "listenToMe" );
        assertEquals( "listenToMe", job.getProgressData().getDescription() );
    }

}
