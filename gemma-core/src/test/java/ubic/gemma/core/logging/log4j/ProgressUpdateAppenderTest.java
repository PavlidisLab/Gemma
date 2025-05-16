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
package ubic.gemma.core.logging.log4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import ubic.gemma.core.job.progress.ProgressUpdateCallback;
import ubic.gemma.core.job.progress.ProgressUpdateContext;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * For this test to work you should have the appender configured in log4j-test.properties. If not it will be set up
 * programatically.
 *
 * @author pavlidis
 */
public class ProgressUpdateAppenderTest {

    private static Log log = LogFactory.getLog( ProgressUpdateAppenderTest.class );

    private final Deque<String> updates = new LinkedBlockingDeque<>();

    @Test
    public void testProgressLogging() {
        // create a region where the task executes that the update callback is responsive to logs
        try ( ProgressUpdateContext progressUpdateContext = ProgressUpdateAppender.createContext( updates::add ) ) {
            String expectedValue = "la de da";
            log.info( expectedValue );

            assertEquals( expectedValue, updates.peekLast() );

            log.debug( "pay no attention" ); // should not update the description.
            assertEquals( expectedValue, updates.peekLast() );

            log.warn( "listenToMe" );
            assertEquals( "listenToMe", updates.peekLast() );
        }

        // this is outside the context, so it should not be picked up
        log.info( "da de di do du" );
        assertEquals( "listenToMe", updates.peekLast() );
    }

    @Test
    public void testLoggingInProgressUpdateCallbackDoesNotResultInLoggingRecursion() {
        AtomicBoolean reached = new AtomicBoolean( false );
        ProgressUpdateCallback progressUpdateCallback = ( message ) -> {
            // if this is set here, the ProcessUpdateAppender might recurse, so we must ensure that there is no
            // current context
            assertThat( ProgressUpdateAppender.currentContext() ).isEmpty();
            log.info( "This message should not be picked up." );
            reached.set( true );
        };
        try ( ProgressUpdateContext progressUpdateContext = ProgressUpdateAppender.createContext( progressUpdateCallback ) ) {
            assertThat( ProgressUpdateAppender.currentContext() ).hasValue( progressUpdateContext );
            log.info( "la da de" );
        }
        assertTrue( reached.get() );
        assertThat( ProgressUpdateAppender.currentContext() ).isEmpty();
    }
}
