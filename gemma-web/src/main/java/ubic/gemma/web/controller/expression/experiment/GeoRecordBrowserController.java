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

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserService;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

/**
 * @author pavlidis
 */
@Controller
@CommonsLog
public class GeoRecordBrowserController {

    @Autowired
    private GeoBrowserService geoBrowserService;

    @Autowired
    private ServletContext servletContext;

    /**
     * AJAX
     *
     * @param start starting record number
     * @param count how many records to retrieve "per page"
     * @return GEO series records fetch from GEO.
     * @throws IOException    IO problems
     * @throws ParseException parse problems
     */
    public Collection<GeoRecord> browse( int start, int count, String searchString )
            throws IOException, ParseException {
        Collection<GeoRecord> geoRecords;

        if ( count == 0 ) {
            count = 20; // sorry.
        }
        if ( start < 0 ) {
            start = 0;
        }

        int startPage = start / count + 1;

        if ( searchString.isEmpty() ) {
            // No search term entered
            geoRecords = geoBrowserService.getRecentGeoRecords( startPage, count );
            // TEST log
            log.info( "getRecentGeoRecords fired from Controller.browse" );
        } else {
            // Search term entered; FIX ME
            geoRecords = geoBrowserService.searchGeoRecords( searchString, start, count, false );
            // TEST log
            log.info( "searchGeoRecords fired from Controller.browse" );
        }

        log./* debug */info(
                "Returning " + geoRecords.size() + " records on page=" + startPage + ", search term=" + searchString );
        return geoRecords;
    }

    public String getDetails( String accession ) {
        try {
            return geoBrowserService.getDetails( accession, servletContext.getContextPath() );
        } catch ( IOException e ) {
            return "No result";
        }
    }

    public boolean toggleUsability( String accession ) {
        return geoBrowserService.toggleUsability( accession );
    }

}
