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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author pavlidis
 */
public class ExpressionExperimentBibRefFinder {

    private static final Log log = LogFactory.getLog( ExpressionExperimentBibRefFinder.class.getName() );

    private static final String GEO_SERIES_URL_BASE = "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=";

    private static final String PUBMEDREF_REGEX = "class=\"pubmed_id\" id=\"(\\d+)";

    private final String ncbiApiKey;

    public ExpressionExperimentBibRefFinder( String ncbiApiKey ) {
        this.ncbiApiKey = ncbiApiKey;
    }

    public BibliographicReference locatePrimaryReference( ExpressionExperiment ee ) throws IOException {

        if ( ee.getPrimaryPublication() != null )
            return ee.getPrimaryPublication();

        DatabaseEntry accession = ee.getAccession();

        if ( accession == null ) {
            ExpressionExperimentBibRefFinder.log.warn( String.format( "%s has no accession, will return null as primary reference", ee ) );
            return null;
        }

        ExternalDatabase ed = accession.getExternalDatabase();

        if ( !ed.getName().equals( "GEO" ) ) {
            ExpressionExperimentBibRefFinder.log.warn( "Don't know how to get references for non-GEO data sets" );
            return null;
        }

        String geoId = accession.getAccession();

        int pubMedId = this.locatePubMedId( geoId );

        if ( pubMedId < 0 )
            return null;

        PubMedSearch fetcher = new PubMedSearch( ncbiApiKey );
        return fetcher.fetchById( pubMedId );
    }

    private int locatePubMedId( String geoSeries ) throws IOException {
        if ( !geoSeries.matches( "GSE\\d+" ) ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " is not a GEO Series Accession" );
            return -1;
        }
        URL url;

        Pattern pat = Pattern.compile( ExpressionExperimentBibRefFinder.PUBMEDREF_REGEX );

        URLConnection conn;
        try {
            url = new URL( ExpressionExperimentBibRefFinder.GEO_SERIES_URL_BASE + geoSeries );
            conn = url.openConnection();
            conn.connect();
        } catch ( IOException e1 ) {
            ExpressionExperimentBibRefFinder.log.error( e1, e1 );
            throw new RuntimeException( "Could not get data from remote server", e1 );
        }

        try ( InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {

            String line;
            while ( ( line = br.readLine() ) != null ) {
                Matcher mat = pat.matcher( line );
                ExpressionExperimentBibRefFinder.log.debug( line );
                if ( mat.find() ) {
                    String capturedAccession = mat.group( 1 );
                    if ( StringUtils.isBlank( capturedAccession ) )
                        return -1;
                    return Integer.parseInt( capturedAccession );
                }
            }
        } catch ( NumberFormatException e ) {
            ExpressionExperimentBibRefFinder.log.error( e, e );
            throw new RuntimeException( "Could not determine valid pubmed id" );
        }

        return -1;

    }
}
