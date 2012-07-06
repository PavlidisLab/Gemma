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
package ubic.gemma.loader.expression.geo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.expression.geo.model.GeoRecord;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoBrowser {

    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";

    private static Log log = LogFactory.getLog( GeoBrowser.class.getName() );

    // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv because of garbled tabbed lines returned
    // from GEO.
    private String GEO_BROWSE_URL = "http://www.ncbi.nlm.nih.gov/geo/browse/?view=series&zsort=date&mode=csv&page=";
    private String GEO_BROWSE_SUFFIX = "&display=";

    private String[] DATE_FORMATS = new String[] { "MMM dd, yyyy" };

    /**
     * Retrieves and parses tab delimited file from GEO. File contains pageSize GEO records starting from startPage.
     * 
     * @param startPage
     * @param pageSize
     * @return list of GeoRecords
     * @throws IOException
     * @throws ParseException
     */
    public List<GeoRecord> getRecentGeoRecords( int startPage, int pageSize ) throws IOException, ParseException {

        if ( startPage < 0 || pageSize < 0 ) throw new IllegalArgumentException( "Values must be greater than zero " );

        List<GeoRecord> records = new ArrayList<GeoRecord>();
        URL url = null;
        try {
            url = new URL( GEO_BROWSE_URL + startPage + GEO_BROWSE_SUFFIX + pageSize );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Invalid URL " + url, e );
        }

        InputStream is = null;

        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            is = conn.getInputStream();
        } catch ( IOException e ) {
            log.error( e, e );
            throw e;
        }

        // We are getting a tab delimited file.
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        // Read columns headers.
        String headerLine = br.readLine();
        String[] headers = StringUtil.csvSplit( headerLine );

        // Map column names to their indices (handy later).
        Map<String, Integer> columnNameToIndex = new HashMap<String, Integer>();
        for ( int i = 0; i < headers.length; i++ ) {
            columnNameToIndex.put( headers[i], i );
        }

        // Read the rest of the file.
        String line = null;
        while ( ( line = br.readLine() ) != null ) {
            String[] fields = StringUtil.csvSplit( line );

            GeoRecord geoRecord = new GeoRecord();
            geoRecord.setGeoAccession( fields[columnNameToIndex.get( "Accession" )] );
            geoRecord.setTitle( StringUtils.strip( fields[columnNameToIndex.get( "Title" )].replaceAll(
                    FLANKING_QUOTES_REGEX, "" ) ) );

            String sampleCountS = fields[columnNameToIndex.get( "Sample Count" )];
            if ( StringUtils.isNotBlank( sampleCountS ) ) {
                try {
                    geoRecord.setNumSamples( Integer.parseInt( sampleCountS ) );
                } catch ( NumberFormatException e ) {
                    throw new RuntimeException( "Could not parse sample count: " + sampleCountS );
                }
            } else {
                log.warn( "No sample count for " + geoRecord.getGeoAccession() );
            }
            geoRecord
                    .setContactName( fields[columnNameToIndex.get( "Contact" )].replaceAll( FLANKING_QUOTES_REGEX, "" ) );

            String[] taxons = fields[columnNameToIndex.get( "Taxonomy" )].replaceAll( FLANKING_QUOTES_REGEX, "" )
                    .split( ";" );
            geoRecord.getOrganisms().addAll( Arrays.asList( taxons ) );

            Date date = DateUtils.parseDate(
                    fields[columnNameToIndex.get( "Release Date" )].replaceAll( FLANKING_QUOTES_REGEX, "" ),
                    DATE_FORMATS );
            geoRecord.setReleaseDate( date );

            geoRecord.setSeriesType( fields[columnNameToIndex.get( "Series Type" )] );

            records.add( geoRecord );
        }

        is.close();

        if ( records.isEmpty() ) {
            log.warn( "No records obatined" );
        }
        return records;

    }
}
