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
package ubic.gemma.loader.entrez.pubmed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentBibRefFinder {

    private static Log log = LogFactory.getLog( ExpressionExperimentBibRefFinder.class.getName() );

    private static String GEO_SERIES_URL_BASE = "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=";

    private static String PUBMEDREF_REGEX = "class=\"pubmed_id\" id=\"(\\d+)";

    /**
     * @param ee
     * @return
     */
    public BibliographicReference locatePrimaryReference( ExpressionExperiment ee ) {

        if ( ee.getPrimaryPublication() != null ) return ee.getPrimaryPublication();

        DatabaseEntry accession = ee.getAccession();

        ExternalDatabase ed = accession.getExternalDatabase();

        if ( !ed.getName().equals( "GEO" ) ) {
            log.warn( "Don't know how to get references for non-GEO data sets" );
            return null;
        }

        String geoId = accession.getAccession();

        int pubMedId = this.locatePubMedId( geoId );

        if ( pubMedId < 0 ) return null;

        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        return fetcher.retrieveByHTTP( pubMedId );
    }

    /**
     * @param geoSeries
     * @return
     */
    private int locatePubMedId( String geoSeries ) {
        if ( !geoSeries.matches( "GSE\\d+" ) ) {
            log.warn( geoSeries + " is not a GEO Series Accession" );
            return -1;
        }
        URL url = null;

        Pattern pat = Pattern.compile( PUBMEDREF_REGEX );

        try {
            url = new URL( GEO_SERIES_URL_BASE + geoSeries );

            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;
            while ( ( line = br.readLine() ) != null ) {
                Matcher mat = pat.matcher( line );
                log.debug( line );
                if ( mat.find() ) {
                    String capturedAccession = mat.group( 1 );
                    if ( StringUtils.isBlank( capturedAccession ) ) return -1;
                    return Integer.parseInt( capturedAccession );
                }
            }
            is.close();
        } catch ( MalformedURLException e ) {
            log.error( e, e );
            throw new RuntimeException( "Invalid URL " + url, e );
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not get data from remote server", e );
        } catch ( NumberFormatException e ) {
            log.error( e, e );
            throw new RuntimeException( "Could not determine valid pubmed id" );
        }

        return -1;

    }
}
