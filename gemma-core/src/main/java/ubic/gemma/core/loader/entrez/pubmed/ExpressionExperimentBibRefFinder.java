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
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserImpl;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * @author pavlidis
 */
public class ExpressionExperimentBibRefFinder {

    private static final Log log = LogFactory.getLog( ExpressionExperimentBibRefFinder.class.getName() );

    private final GeoBrowser geoBrowser;

    private final String ncbiApiKey;

    public ExpressionExperimentBibRefFinder( String ncbiApiKey ) {
        this( new GeoBrowserImpl( ncbiApiKey ), ncbiApiKey );
    }

    /**
     * Inject the GEO client, so that resolution can be tested without reaching Entrez.
     */
    public ExpressionExperimentBibRefFinder( GeoBrowser geoBrowser, String ncbiApiKey ) {
        this.geoBrowser = geoBrowser;
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
     * Every PubMed id GEO lists for a series, in the order GEO lists them.
     *
     * <h4>Why Entrez and not {@code acc.cgi}</h4>
     *
     * <p>This used to scrape the per-accession {@code acc.cgi} SOFT record
     * ({@code &targ=self&form=text}) for {@code !Series_pubmed_id}, on the argument that the
     * {@code esummary db=gds} index lags the live GEO record: a re-pointed publication link shows up
     * on {@code acc.cgi} at once but can be missing from {@code esummary} for a while (observed on
     * GSE270880). A refresh whose job is to catch drifted links wants the lag-free source, so
     * {@code acc.cgi} was chosen despite eutils being the better-behaved API.</p>
     *
     * <p>That trade no longer exists, because the lag-free source stopped answering. NCBI serves
     * {@code www.ncbi.nlm.nih.gov/geo/query/acc.cgi} behind a Google reCAPTCHA challenge, returned as
     * <em>HTTP 200 with an HTML body</em> — no error status, no exception, just a page with no
     * {@code !Series_pubmed_id} in it. Every series therefore parsed as "GEO states no publication".
     * Measured 2026-09-02; the same challenge is served for {@code form=xml} and for GEO's own
     * documentation pages, so it is the host and not one view.</p>
     *
     * <p>Entrez is the supported programmatic interface, on a separate host
     * ({@code eutils.ncbi.nlm.nih.gov}) that is not challenged, takes the API key, and publishes rate
     * limits. {@link GeoBrowser} already does the two documented steps — {@code esearch db=gds} to
     * turn the accession into a UID, then {@code esummary} — so nothing here constructs a UID by
     * hand. {@code GeoRetrieveConfig.DEFAULT} leaves every detail flag off, so this costs a search
     * and a summary and never the MINiML family download.</p>
     *
     * <p>The freshness caveat is real and now unavoidable: a very recently re-pointed link may take
     * time to reach {@code esummary}. A stale answer beats the alternative, which is a confident
     * wrong answer for the entire corpus.</p>
     *
     * <h4>Use esummary, not elink</h4>
     *
     * <p>{@code elink dbfrom=gds&db=pubmed} looks like an equivalent way to ask the same question and
     * is not: it returns the same ids in a different order. GSE934 lists 15802019 then 15867358 in
     * {@code esummary}, matching what {@code acc.cgi} listed; {@code elink} answers 15867358 then
     * 15802019 (checked 2026-09-02).</p>
     *
     * <p>Order is not cosmetic here. Callers take the first id as the primary publication and write
     * it: {@code VerifyPublicationEvidenceCli} stores {@code get(0)} as primary with the rest
     * other-relevant, and promotes held evidence to TAS only when Gemma's paper sits at position 0 --
     * a paper anywhere later is reported for a curator instead. Swapping in {@code elink} would
     * therefore flip which paper Gemma calls primary on every multi-paper series, and turn
     * "a curator should look at this" into "verified", silently. Note also that first-is-primary is
     * {@code GeoConverterImpl}'s convention rather than something GEO asserts, so the order carries
     * more weight than GEO itself puts on it.</p>
     *
     * <h4>Empty is not the same as unknown</h4>
     *
     * <p>An empty list means GEO states no publication for the series. It never means "GEO could not
     * be read" — that throws {@link IOException}. Keeping those apart is the whole point: the
     * CAPTCHA above was harmful precisely because it collapsed into the empty case, and callers that
     * write a finding on an empty list went on to record "GEO lists no publication" for datasets
     * nobody had successfully asked about.</p>
     *
     * @return GEO's PubMed ids in order, first being the primary; empty when the accession is not a
     * GEO series or GEO states no publication for it
     * @throws IOException if GEO could not be reached, or has no record at all for this accession —
     *                     i.e. whenever the answer is unknown rather than "none"
     */
    public List<Integer> locatePubMedIds( String geoSeries ) throws IOException {
        if ( !geoSeries.matches( "GSE\\d+" ) ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " is not a GEO Series Accession" );
            return Collections.emptyList();
        }
        GeoRecord record = geoBrowser.getGeoRecord( GeoRecordType.SERIES, geoSeries );
        if ( record == null ) {
            // not "GEO says no publication" -- GEO has no series under this accession at all, so we
            // have not learned anything about its publications. Callers must not record a finding.
            throw new IOException( "GEO returned no series record for " + geoSeries + "." );
        }
        List<String> pubMedIds = record.getPubMedIds();
        if ( pubMedIds == null ) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>( pubMedIds.size() );
        for ( String pubMedId : pubMedIds ) {
            String value = StringUtils.strip( pubMedId );
            if ( StringUtils.isBlank( value ) ) {
                continue;
            }
            if ( !value.matches( "\\d+" ) ) {
                ExpressionExperimentBibRefFinder.log.warn( geoSeries
                        + ": ignoring non-numeric PubMed id '" + value + "'" );
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

    /**
     * As {@link #locatePubMedIds(String)}, but only the first id — the primary, matching
     * {@code GeoConverterImpl}'s first-id-is-primary convention.
     *
     * <p>Public because verification needs this and nothing else. {@link #locatePrimaryReference(ExpressionExperiment, boolean)} spends
     * a second PubMed round trip building a {@link BibliographicReference}, which a caller that only
     * wants to know whether GEO still says the same id has no use for.</p>
     *
     * @return GEO's primary PubMed id, or -1 when the accession is not a series or GEO states no
     * publication for it
     * @throws IOException as {@link #locatePubMedIds(String)} — an unknown answer is never -1
     */
    public int locatePubMedId( String geoSeries ) throws IOException {
        List<Integer> ids = locatePubMedIds( geoSeries );
        if ( ids.size() > 1 ) {
            ExpressionExperimentBibRefFinder.log.warn( geoSeries + " lists " + ids.size()
                    + " PubMed ids in GEO; using the first (" + ids.get( 0 )
                    + ") as the primary reference. A curator should confirm which is primary." );
        }
        return ids.isEmpty() ? -1 : ids.get( 0 );
    }
}
