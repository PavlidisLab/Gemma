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
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.loader.expression.geo.service.GeoBrowserService;
import ubic.gemma.web.controller.BaseMultiActionController;

/**
 * @spring.bean id="geoRecordBrowserController"
 * @spring.property name="geoBrowserService" ref="geoBrowserService"
 * @spring.property name="methodNameResolver" ref="geoRecordBrowserActions"
 * @version $Id$
 * @author pavlidis
 */
public class GeoRecordBrowserController extends BaseMultiActionController {

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_START = 0;
    GeoBrowserService geoBrowserService;

    public void setGeoBrowserService( GeoBrowserService geoBrowserService ) {
        this.geoBrowserService = geoBrowserService;
    }

    public ModelAndView showBatch( HttpServletRequest request, HttpServletResponse response ) {

        boolean next = request.getParameter( "next" ) != null;
        boolean prev = request.getParameter( "prev" ) != null;

        int start = DEFAULT_START;
        String startSt = request.getParameter( "start" );
        if ( StringUtils.isNotBlank( startSt ) ) {
            try {
                start = Integer.parseInt( startSt );
            } catch ( NumberFormatException e ) {
                //
            }
        }

        int count = DEFAULT_BATCH_SIZE;

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

        if ( next ) {
            start += count;
        } else if ( prev ) {
            start = Math.max( 0, start -= count );
        }
        start = start + skip;

        Collection<GeoRecord> geoRecords = geoBrowserService.getRecentGeoRecords( start, count );

        ModelAndView mav = new ModelAndView( "geoRecordBrowser" );

        mav.addObject( "start", start );
        if ( geoRecords != null ) {
            mav.addObject( "geoRecords", geoRecords );
            mav.addObject( "numGeoRecords", geoRecords.size() );
        } else {
            mav.addObject( "numGeoRecords", 0 );
        }
        return mav;
    }
}
