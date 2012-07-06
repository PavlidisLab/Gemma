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
package ubic.gemma.web.controller.expression.experiment;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.loader.expression.geo.service.GeoBrowserService;

/**
 * @version $Id$
 * @author pavlidis
 */
@Component
public class GeoRecordBrowserController {

    private static Logger log = LoggerFactory.getLogger( GeoRecordBrowserController.class );
    @Autowired
    private GeoBrowserService geoBrowserService;

    /**
     * AJAX
     * 
     * @param start
     * @param count
     * @param skip
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public Collection<GeoRecord> browse( int start, int count, int skip ) throws IOException, ParseException {
        if ( count == 0 ) {
            count = 20; // sorry.
        }
        if ( start < 0 ) {
            start = 0;
        }
        if ( skip < 0 ) {
            skip = 0;
        }
        if ( skip > 10000 ) {
            skip = 10000;
        }
        int startPage = ( ( start + skip ) / count ) + 1;
        log.info( "Start page =" + startPage );

        Collection<GeoRecord> geoRecords = geoBrowserService.getRecentGeoRecords( startPage, count );
        return geoRecords;
    }

    /**
     * @param accession
     * @return
     */
    public String getDetails( String accession ) {
        return geoBrowserService.getDetails( accession );
    }

    /**
     * @param accession
     * @return
     */
    public boolean toggleUsability( String accession ) {
        return geoBrowserService.toggleUsability( accession );
    }

}
