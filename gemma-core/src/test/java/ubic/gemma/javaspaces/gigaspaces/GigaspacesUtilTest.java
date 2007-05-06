/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.javaspaces.gigaspaces;

import org.springframework.context.ApplicationContext;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaspacesUtilTest extends BaseSpringContextTest {

    private static final String DEFAULT_REMOTING_SPACE = "rmi://localhost:10098/./remotingSpace";

    /**
     * Tests if space is running.
     */
    public void testIsSpaceRunning() {

        boolean isRunning = GigaspacesUtil.isSpaceRunning( DEFAULT_REMOTING_SPACE );

        if ( isRunning )
            assertTrue( isRunning );

        else
            assertFalse( isRunning );

    }

    /**
     * Tests starting the space at the specified url.
     */
    public void testStartSpace() {

        ApplicationContext ctx = this.getContext();

        GigaspacesUtil.startSpace( ctx, DEFAULT_REMOTING_SPACE );
    }

    /**
     * Tests stopping the space at the specified url.
     */
    public void testStopSpace() {

        ApplicationContext ctx = this.getContext();
        GigaspacesUtil.stopSpace( ctx, DEFAULT_REMOTING_SPACE );
    }

    /**
     * Tests checking for workers listening for space at specified url.
     */
    public void testAreWorkersListening() {

        GigaspacesUtil.areWorkersListening( DEFAULT_REMOTING_SPACE );
    }
}
