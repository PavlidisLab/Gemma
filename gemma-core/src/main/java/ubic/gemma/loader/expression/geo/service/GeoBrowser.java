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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.model.GeoRecord;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoBrowser {

    private static Log log = LogFactory.getLog( GeoBrowser.class.getName() );
    private String GEO_BROWSE_URL = "http://www.ncbi.nlm.nih.gov/projects/geo/query/browse.cgi?mode=series&private=0&sorton=pub_date&sortdir=1&start=";
    private String GEO_BROWSE_SUFFIX = "&pgsize=";

    private String GEO_TABLE_CELL_REGEXP = ".+?(DEEBDC|EEEEEE)\".*?>(.+?)</td>";

    /**
     * For an example of the HTML used, see
     * {@link http://www.ncbi.nlm.nih.gov/projects/geo/query/browse.cgi?mode=series&sorton=pub_date&sortdir=1&start=1&pgsize=10}
     * 
     * @param startPoint how many records in to start from. For example, 100 means start from record 100.
     * @param numberToFetch how many, from the most recent, to fetch.
     * @return List of GeoRecords with data on the experiments, most recent first.
     */
    public List<GeoRecord> getRecentGeoRecords( int startPoint, int numberToFetch ) {
        Pattern pat = Pattern.compile( GEO_TABLE_CELL_REGEXP );
        Pattern simpleUrlPat = Pattern.compile( "<.+?>(.+?)<.+?>" );
        URL url = null;
        List<GeoRecord> records = new ArrayList<GeoRecord>();
        try {
            url = new URL( GEO_BROWSE_URL + startPoint + GEO_BROWSE_SUFFIX + numberToFetch );

            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;
            int fieldnum = 0;
            GeoRecord geoRecord = null;

            while ( ( line = br.readLine() ) != null ) {
                Matcher mat = pat.matcher( line );
                log.debug( line );
                if ( mat.find() ) {
                    String captured = mat.group( 2 );
                    log.debug( "Field " + fieldnum + " Got: " + captured );
                    switch ( fieldnum ) {
                        case 0:
                            if ( geoRecord != null ) {
                                records.add( geoRecord );
                                log.info( "Record: " + geoRecord );
                            }
                            geoRecord = new GeoRecord();
                            Pattern accath = Pattern.compile( ".+?acc=(GSE\\d+).+" );
                            Matcher accmat = accath.matcher( captured );
                            accmat.find();
                            geoRecord.setGeoAccession( accmat.group( 1 ) );
                            break;
                        case 1:
                            geoRecord.setTitle( captured );
                            break;
                        case 2:
                            int numSamples = Integer.parseInt( captured );
                            geoRecord.setNumSamples( numSamples );
                            break;
                        case 3:
                            String[] fields = captured.split( "; " );

                            for ( String string : fields ) {
                                Matcher specCap = simpleUrlPat.matcher( string );
                                specCap.find();
                                String organism = specCap.group( 1 );
                                log.debug( "Organism: " + organism );
                                geoRecord.getOrganisms().add( organism );
                            }
                            break;
                        case 4:
                            // supplementary file, skip it.
                            break;
                        case 5:
                            // contact
                            log.debug( captured );
                            Matcher specCap = simpleUrlPat.matcher( captured );
                            specCap.find();
                            String contact = specCap.group( 1 );
                            geoRecord.setContactName( contact );
                            specCap.find();
                            break;
                        case 6:
                            DateFormat df = DateFormat.getDateInstance();
                            Date d = df.parse( captured );
                            log.debug( d );
                            geoRecord.setReleaseDate( d );
                            fieldnum = -1; // back to start.
                            break;
                        default:
                            break;
                    }

                    fieldnum++;
                }

            }
            // last one dangles
            if ( geoRecord != null ) {
                records.add( geoRecord );
            }
            is.close();
            return records;
        } catch ( MalformedURLException e ) {
            log.error( e, e );
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not get data from remote server", e );
        } catch ( NumberFormatException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not parse sample count" );
        } catch ( ParseException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not parse date" );
        }

    }
}
