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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.loader.expression.geo.service.GeoBrowserService;

/**
 * @version $Id$
 * @author pavlidis
 */
@Controller
public class GeoRecordBrowserController {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_START_PAGE = 1;

    @Autowired
    private GeoBrowserService geoBrowserService;

    @RequestMapping("/admin/geoBrowser/showBatch.html")
    @SuppressWarnings("unused")
    public ModelAndView handleRequest( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        boolean next = request.getParameter( "next" ) != null;
        boolean prev = request.getParameter( "prev" ) != null;

        
        int start = 1;
        String startSt = request.getParameter( "start" );
        if ( StringUtils.isNotBlank( startSt ) ) {
            try {
                start = Integer.parseInt( startSt );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        int count = DEFAULT_BATCH_SIZE;
        int startPage = DEFAULT_START_PAGE;

        String batchSize = request.getParameter( "count" );
        if ( StringUtils.isNotBlank( batchSize ) ) {
            try {
                count = Integer.parseInt( batchSize );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        int skip = 0;

        String skipSize = request.getParameter( "skip" );
        if ( StringUtils.isNotBlank( skipSize ) ) {
            try {
                skip = Integer.parseInt( skipSize );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        String minSamplesS = request.getParameter( "minsam" );
        if ( StringUtils.isNotBlank( skipSize ) ) {
            try {
                skip = Integer.parseInt( skipSize );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        String taxonS = request.getParameter( "taxon" );

        if ( next ) {
            start += count;
        } else if ( prev ) {
            start = Math.max( 0, start -= count );
        }
        start = start + skip;

        startPage = (start/count) + 1;
        Collection<GeoRecord> geoRecords = geoBrowserService.getRecentGeoRecords( startPage, count );

        ModelAndView mav = new ModelAndView( "/admin/geoRecordBrowser" );

        mav.addObject( "start", start );
        if ( geoRecords != null ) {
            mav.addObject( "geoRecords", geoRecords );
            mav.addObject( "numGeoRecords", geoRecords.size() );
        } else {
            mav.addObject( "numGeoRecords", 0 );
        }
        return mav;
    }

    public void setGeoBrowserService( GeoBrowserService geoBrowserService ) {
        this.geoBrowserService = geoBrowserService;
    }

}
