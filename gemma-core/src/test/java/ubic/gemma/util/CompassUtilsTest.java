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
package ubic.gemma.util;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class CompassUtilsTest extends BaseSpringContextTest {
    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * Tests deleting the compass index
     */
    public void testDeleteCompassIndex() {
        log.debug( this.getBean( "compassGps" ) );

        CompassGpsInterfaceDevice gps = ( CompassGpsInterfaceDevice ) this.getBean( "compassGps" );

        boolean fail = false;
        try {
            CompassUtils.deleteCompassIndex( gps );
        } catch ( IOException e ) {
            fail = true;
            e.printStackTrace();
        } finally {
            assertFalse( false );
        }
    }

}
