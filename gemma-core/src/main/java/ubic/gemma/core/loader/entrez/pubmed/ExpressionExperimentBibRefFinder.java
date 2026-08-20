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
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * @author pavlidis
 */
public class ExpressionExperimentBibRefFinder {

    private static final Log log = LogFactory.getLog( ExpressionExperimentBibRefFinder.class.getName() );

    private static final String GEO_SERIES_URL_BASE = "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=";

    /**
     * SOFT "self" text view of a series. It exposes the machine-readable {@code !Series_pubmed_id}
     * field, which tracks the live GEO record — unlike the {@code esummary db=gds} index, which lags
     * behind re-pointed publication links. This is the same field {@code GeoFamilyParser} reads on
     * import, so the refresh path stays consistent with (and as current as) first import.
     */
    private static final String GEO_SERIES_SOFT_SUFFIX = "&targ=self&form=text";

    private static final String SERIES_PUBMED_ID_TAG = "!Series_pubmed_id";

    private final String ncbiApiKey;

    public ExpressionExperimentBibRefFinder( String ncbiApiKey ) {
        this.ncbiApiKey = ncbiApiKey;
    }

    public BibliographicReference locatePrimaryReference( ExpressionExperiment ee ) throws IOException {
        return locatePrimaryReference( ee, false );
    }

    /**
     * Locate the primary reference for an experiment from GEO.
     *
     * @param forceFromGeo when {@code false} (the default), an experiment that already has a primary
     *                     publication is left untouched — this only fills in missing links. When
     *                     {@code true}, GEO's current {@code !Series_pubmed_id} is re-resolved even if
     *                     a primary is already set, so a link that GEO has since re-pointed can be
     *                     refreshed. Use with care: it will override a curator's non-GEO primary.
     */
    public BibliographicReference locatePrimaryReference( ExpressionExperiment ee, boolean forceFromGeo ) throws IOException {

        if ( !forceFromGeo && ee.getPrimaryPublication() != null )
            return ee.getPrimaryPublication();

        DatabaseEntry accession = ee.getAccession();

        if ( accession == null ) {
            ExpressionExperimentBibRefFinder.log.warn( String.format( "%s has no accession, will return null as primary reference", ee ) );
            return null;
        }

        ExternalDatabase ed = accession.getExternalDatabase();

        if ( !ed.getName().equals( ExternalDatabases.GEO ) ) {
            ExpressionExperimentBibRefFinder.log.warn( "Don't know how to get references for non-GEO data sets" );
            return null;
        }

        String geoId = accession.getAccession();

        int pubMedId = this.locatePubMedId( geoId );

        if ( pubMedId < 0 )
            return null;

        PubMedSearch fetcher = new PubMedSearch( ncbiApiKey );
        return fetcher.retrieve( String.valueOf( pubMedId ) );
    }

    /**
     * Resolve the current primary PubMed id for a GEO series.
     * <p>
     * We intentionally scrape the per-accession {@code acc.cgi} SOFT record rather than call the
     * "modern" Entrez E-utilities ({@code esummary}/{@code elink} with {@code db=gds}). The gds
     * index <em>lags the live GEO record</em>: when a series is re-pointed to a newer paper, the new
     * {@code Series_pubmed_id} shows up on the {@code acc.cgi} record immediately but can be absent
     * from the {@code esummary db=gds} response for a long time (observed on GSE270880). A refresh
     * whose whole job is to catch drifted links must read the authoritative, lag-free source, so
     * eutils is the wrong tool here despite being the nicer API. The {@code &targ=self&form=text}
     * view gives the same {@code !Series_pubmed_id} field the importer ({@code GeoFamilyParser})
     * already trusts.
     *
     * <p>Public because verification needs this and nothing else. {@code locatePrimaryReference} spends
     * a second PubMed round trip building a {@link BibliographicReference}, which a caller that only
     * wants to know whether GEO still says the same id has no use for.</p>
     *
     * @return GEO's current {@code !Series_pubmed_id}, or -1 when the accession is not a series or GEO
     * states no publication for it
     */
    /**
     * As {@link #locatePubMedId(String)}, but every id the series lists rather than only the first.
     *
     * @return GEO's {@code !Series_pubmed_id} values in order, first being the primary; empty when the
     * accession is not a GEO series or GEO states no publication for it
     */
    public List<Integer> locatePubMedIds( String geoSeries ) throws IOException {
        if ( !geoSeries.matches( "GSE\\d+" ) ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " is not a GEO Series Accession" );
            return Collections.emptyList();
        }
        URL url;
        URLConnection conn;
        try {
            url = new URL( ExpressionExperimentBibRefFinder.GEO_SERIES_URL_BASE + geoSeries
                    + ExpressionExperimentBibRefFinder.GEO_SERIES_SOFT_SUFFIX );
            conn = url.openConnection();
            conn.connect();
        } catch ( IOException e1 ) {
            ExpressionExperimentBibRefFinder.log.error( e1, e1 );
            throw new RuntimeException( "Could not get data from remote server", e1 );
        }
        try ( InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            return parseSeriesPubMedIds( br, geoSeries );
        }
    }

    public int locatePubMedId( String geoSeries ) throws IOException {
        if ( !geoSeries.matches( "GSE\\d+" ) ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " is not a GEO Series Accession" );
            return -1;
        }
        URL url;
        URLConnection conn;
        try {
            url = new URL( ExpressionExperimentBibRefFinder.GEO_SERIES_URL_BASE + geoSeries
                    + ExpressionExperimentBibRefFinder.GEO_SERIES_SOFT_SUFFIX );
            conn = url.openConnection();
            conn.connect();
        } catch ( IOException e1 ) {
            ExpressionExperimentBibRefFinder.log.error( e1, e1 );
            throw new RuntimeException( "Could not get data from remote server", e1 );
        }

        try ( InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            return parseSeriesPubMedId( br, geoSeries );
        }
    }

    /**
     * Scan a GEO series SOFT record ({@code acc.cgi ...&targ=self&form=text}) for its primary PubMed
     * id, reading the {@code !Series_pubmed_id} field the same way {@link ubic.gemma.core.loader.expression.geo.GeoFamilyParser}
     * does on import. When a series lists several PubMed ids the first is returned (the primary,
     * matching {@code GeoConverterImpl}'s first-id-is-primary convention) and the rest are logged for
     * a curator. Returns {@code -1} when no numeric {@code !Series_pubmed_id} is present.
     */
    static int parseSeriesPubMedId( BufferedReader br, String geoSeries ) throws IOException {
        List<Integer> ids = parseSeriesPubMedIds( br, geoSeries );
        if ( ids.size() > 1 ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " lists " + ids.size()
                    + " PubMed ids in GEO; using the first (" + ids.get( 0 )
                    + ") as the primary reference. A curator should confirm which is primary." );
        }
        return ids.isEmpty() ? -1 : ids.get( 0 );
    }

    /**
     * Every {@code !Series_pubmed_id} the record carries, in the order GEO lists them.
     *
     * <p>The single-id form above always READ all of them — it counted them and warned that "a curator
     * should confirm which is primary" — and then discarded everything after the first. A series
     * listing two papers is a series with a primary and a follow-up, which is what the other-relevant
     * slot is for, so that warning described work the caller had no way to act on.</p>
     *
     * <p>First is primary, matching {@code GeoConverterImpl}'s convention; the rest are
     * other-relevant. Empty when GEO states no publication for the series.</p>
     */
    static List<Integer> parseSeriesPubMedIds( BufferedReader br, String geoSeries ) throws IOException {
        List<Integer> ids = new ArrayList<>();
        String line;
        while ( ( line = br.readLine() ) != null ) {
            if ( !StringUtils.startsWithIgnoreCase( StringUtils.stripStart( line, null ), SERIES_PUBMED_ID_TAG ) ) {
                continue;
            }
            int eqIndex = line.indexOf( '=' );
            if ( eqIndex < 0 ) {
                continue;
            }
            String value = StringUtils.strip( line.substring( eqIndex + 1 ) );
            if ( !value.matches( "\\d+" ) ) {
                ExpressionExperimentBibRefFinder.log.warn( geoSeries + ": ignoring non-numeric "
                        + SERIES_PUBMED_ID_TAG + " value '" + value + "'" );
                continue;
            }
            Integer id = Integer.valueOf( value );
            // the same id can appear more than once in a record; one paper listed twice is one paper
            if ( !ids.contains( id ) ) {
                ids.add( id );
            }
        }
        return ids;
    }
}
