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

import org.apache.log4j.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ubic.gemma.job.progress.JobProgress;
import ubic.gemma.job.progress.LocalProgressAppender;
import ubic.gemma.testing.BaseSpringContextTest;

import java.util.Enumeration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static junit.framework.Assert.assertEquals;

/**
 * For this test to work you should have the appender configured in log4j.properties. If not it will be set up
 * programatically.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProgressAppenderTest extends BaseSpringContextTest {

    JobProgress job;

    // used to put things back as they were after the test.
    Level oldLevel;

    Logger log4jLogger;

    Queue<String> updates = new ConcurrentLinkedQueue<String>();
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() {

        String loggerName = "ubic.gemma";
        log4jLogger = LogManager.exists( loggerName );

        Enumeration<Appender> appenders = log4jLogger.getAllAppenders();

        Appender progressAppender = null;
        for ( ; appenders.hasMoreElements(); ) {
            Appender appender = appenders.nextElement();
            if ( appender instanceof LocalProgressAppender) {
                progressAppender = appender;
            }
        }

        if ( progressAppender == null ) {
            log.warn( "There is no progress appender configured; adding one for test" );

            MDC.put("taskId", "randomtaskidF");
            log4jLogger.addAppender( new LocalProgressAppender( "randomtaskidF", updates ) );
        }

        oldLevel = log4jLogger.getLevel();

        log4jLogger.setLevel( Level.INFO );

        //job = ProgressManager.createProgressJob( new TaskCommand() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onTearDownInTransaction()
     */
    @After
    public void teardown() {
        //ProgressManager.destroyProgressJob( job );
        log4jLogger.setLevel( oldLevel );
    }

    @Test
    public void testProgressLogging() {

        String expectedValue = "la de da";
        log.info( expectedValue );

        assertEquals( expectedValue, updates.peek() );

        log.debug( "pay no attention" ); // should not update the description.
        assertEquals( expectedValue, updates.peek() );

        updates.clear();
        log.warn( "listenToMe" );
        assertEquals( "listenToMe", updates.peek() );
    }

}
