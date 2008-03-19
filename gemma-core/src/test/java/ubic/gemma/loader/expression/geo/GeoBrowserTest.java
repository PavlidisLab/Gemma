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
package ubic.gemma.loader.expression.geo;

import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.loader.expression.geo.service.GeoBrowser;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeoBrowserTest extends TestCase {

    public void testGetRecentGeoRecords() {
        GeoBrowser b = new GeoBrowser();
        Collection<GeoRecord> res = b.getRecentGeoRecords( 10, 10 );
        assertEquals( 10, res.size() );
    }

}
