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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.job.progress.LocalProgressAppender;
import ubic.gemma.testing.BaseSpringContextTest;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * For this test to work you should have the appender configured in log4j.properties. If not it will be set up
 * programatically.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressAppenderTest extends BaseSpringContextTest {

    LocalProgressAppender progressAppender;

    // Used to put things back as they were after the test.
    Level oldLevel;

    Logger log4jLogger;

    Deque<String> updates = new LinkedBlockingDeque<String>();
    /*
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() {

        String loggerName = "ubic.gemma";
        log4jLogger = LogManager.exists( loggerName );

        progressAppender = new LocalProgressAppender( "randomtaskidF", updates );
        log4jLogger.addAppender( progressAppender );

        oldLevel = log4jLogger.getLevel();

        log4jLogger.setLevel( Level.INFO );
    }

    /*
     * @see ubic.gemma.testing.BaseSpringContextTest#onTearDownInTransaction()
     */
    @After
    public void teardown() {
        log4jLogger.setLevel( oldLevel );
    }

    @Test
    public void testProgressLogging() {
        progressAppender.initialize();
        assertTrue( "MDC should be set.", MDC.get( "taskId" ) != null );

        String expectedValue = "la de da";
        log.info( expectedValue );

        assertEquals( expectedValue, updates.peekLast() );

        log.debug( "pay no attention" ); // should not update the description.
        assertEquals( expectedValue, updates.peekLast() );

        log.warn( "listenToMe" );
        assertEquals( "listenToMe", updates.peekLast() );

        progressAppender.close();
        assertTrue( "MDC should be cleaned up.", MDC.get( "taskId" ) == null );
    }

}
