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
package ubic.gemma.core.loader.expression.geo.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import ubic.gemma.core.loader.entrez.EutilFetch;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.persistence.util.Slice;

import javax.xml.xpath.XPathExpression;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * This is marked as {@link Lazy} since we don't use it outside Gemma Web, so it won't be loaded unless it's needed.
 * @author pavlidis
 */
@Lazy
@Component
public class GeoBrowserServiceImpl implements GeoBrowserService, InitializingBean, DisposableBean {
    private static final int MIN_SAMPLES = 5;
    private static final String GEO_DATA_STORE_FILE_NAME = "GEODataStore";
    private static final Log log = LogFactory.getLog( GeoBrowserServiceImpl.class.getName() );

    // private static final XPathExpression xgds = XMLUtils.compile( "/eSummaryResult/DocSum/Item[@Name=\"GDS\"][1]/text()" );
    private static final XPathExpression xgse = XMLUtils.compile( "/eSummaryResult/DocSum/Item[@Name=\"GSE\"][1]/text()" );
    private static final XPathExpression xtitle = XMLUtils.compile( "/eSummaryResult/DocSum/Item[@Name=\"title\"][1]/text()" );
    private static final XPathExpression xgpls = XMLUtils.compile( "/eSummaryResult/DocSum/Item[@Name=\"GPL\"]/text()" );
    private static final XPathExpression xsummary = XMLUtils.compile( "/eSummaryResult/DocSum/Item[@Name=\"summary\"][1]/text()" );

    @Autowired
    protected ExpressionExperimentService expressionExperimentService;

    @Autowired
    protected ArrayDesignService arrayDesignService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private EntityUrlBuilder entityUrlBuilder;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    @Value("${gemma.download.path}")
    private String downloadPath;

    private GeoBrowser browser;

    private final Map<String, GeoRecord> localInfo = new ConcurrentHashMap<>();

    private final ExecutorService es = Executors.newSingleThreadExecutor();

    @Override
    public void afterPropertiesSet() {
        browser = new GeoBrowserImpl( ncbiApiKey );
        try {
            es.submit( this::initializeLocalInfo );
        } finally {
            es.shutdown();
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( !es.isTerminated() ) {
            log.warn( "GeoBrowser is still loading local info, shutting it down now..." );
            es.shutdownNow();
        }
        saveLocalInfo();
    }

    @Override
    public String getDetails( String accession, String contextPath ) throws IOException {
        /*
         * The maxrecords is > 1 because it return platforms as well (and there are series with as many as 13 platforms
         * ... leaving some headroom)
         */
        Document details = EutilFetch.summary( "gds", accession, 25, ncbiApiKey );

        if ( details == null ) {
            throw new IOException( "No results from GEO" );
        }

        this.initLocalRecord( accession );

        /*
         * increment click counts.
         */
        localInfo.get( accession ).setPreviousClicks( localInfo.get( accession ).getPreviousClicks() + 1 );

        return this.formatDetails( details, contextPath );

    }

    @Override
    public List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException {
        return this.filterGeoRecords( browser.getRecentGeoRecords( GeoRecordType.SERIES, start, count ) );
    }

    @Override
    public List<GeoRecord> searchGeoRecords( String searchString, int start, int count, boolean detailed ) throws IOException {
        return this.filterGeoRecords( browser.searchAndRetrieveGeoRecords( GeoRecordType.SERIES, searchString, null, null, null, null, start, count, detailed ) );
    }

    @Override
    public boolean toggleUsability( String accession ) {

        this.initLocalRecord( accession );

        localInfo.get( accession ).setUsable( !localInfo.get( accession ).isUsable() );

        return localInfo.get( accession ).isUsable();
    }

    /**
     * Take the details string from GEO and make it nice. Add links to series and platforms that are already in gemma.
     *
     * @param  details XML from eSummary
     * @return HTML-formatted
     */
    String formatDetails( Document details, String contextPath ) throws IOException {
        String gse = "GSE" + XMLUtils.evaluateToString( xgse, details );
        String title = XMLUtils.evaluateToString( xtitle, details );
        NodeList gpls = XMLUtils.evaluate( xgpls, details );
        String summary = XMLUtils.evaluateToString( xsummary, details );

        StringBuilder buf = new StringBuilder();
        buf.append( "<div class=\"small\">" );

        ExpressionExperiment ee = this.expressionExperimentService.findByShortName( gse );

        if ( ee != null ) {
            buf.append( "\n<p><strong><a target=\"_blank\" href=\"" )
                    .append( entityUrlBuilder.fromBaseUrl( contextPath ).entity( ee ).web().toUriString() )
                    .append( "\">" ).append( escapeHtml4( gse ) ).append( "</a></strong>" );
        } else {
            buf.append( "\n<p><strong>" ).append( gse ).append( " [new to Gemma]</strong>" );
        }

        buf.append( "<p>" ).append( title ).append( "</p>\n" );
        buf.append( "<p>" ).append( summary ).append( "</p>\n" );

        this.formatArrayDetails( gpls, buf, contextPath );

        buf.append( "</div>" );
        return buf.toString();
    }

    private List<GeoRecord> filterGeoRecords( Slice<GeoRecord> records ) {
        ExternalDatabase geo = externalDatabaseService.findByName( ExternalDatabases.GEO );
        return records.stream()
                .filter( record -> {
                    if ( record.getNumSamples() < GeoBrowserServiceImpl.MIN_SAMPLES ) {
                        return false;
                    }

                    Collection<String> organisms = record.getOrganisms();
                    if ( organisms == null || organisms.isEmpty() ) {
                        return true;
                    }

                    int i = 0;
                    for ( String string : organisms ) {
                        Taxon t = taxonService.findByCommonName( string );
                        if ( t == null ) {
                            t = taxonService.findByScientificName( string );
                            if ( t == null ) {
                                return false;
                            }
                        }
                        String acc = record.getGeoAccession();
                        if ( organisms.size() > 1 ) {
                            acc = acc + "." + i;
                        }
                        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                        de.setExternalDatabase( geo );
                        de.setAccession( acc );

                        Collection<ExpressionExperiment> ee = expressionExperimentService.findByAccession( de );
                        if ( !ee.isEmpty() ) {
                            for ( ExpressionExperiment expressionExperiment : ee ) {
                                record.getCorrespondingExperiments().add( expressionExperiment.getId() );
                            }
                        }

                        record.setPreviousClicks( localInfo.containsKey( acc ) ? localInfo.get( acc ).getPreviousClicks() : 0 );

                        record.setUsable( !localInfo.containsKey( acc ) || localInfo.get( acc ).isUsable() );
                        i++;
                    }

                    return true;
                } )
                .collect( Collectors.toList() );
    }

    private void formatArrayDetails( NodeList gpls, StringBuilder buf, String contextPath ) {
        Set<String> seenGpl = new HashSet<>();
        for ( int i = 0; i < gpls.getLength(); i++ ) {
            String gpl = "GPL" + gpls.item( i ).getNodeValue();
            if ( gpl.contains( ";" ) )
                continue;
            if ( seenGpl.contains( gpl ) )
                continue;
            seenGpl.add( gpl );
            ArrayDesign arrayDesign = arrayDesignService.findByShortName( gpl );

            if ( arrayDesign != null ) {
                arrayDesign = arrayDesignService.thawLite( arrayDesign );
                String trouble = "";
                if ( arrayDesign.getCurationDetails().getTroubled() ) {
                    AuditEvent lastTroubleEvent = arrayDesign.getCurationDetails().getLastTroubledEvent();
                    if ( lastTroubleEvent != null ) {
                        trouble = "&nbsp;<img src=\"" + contextPath + "/images/icons/warning.png\" height=\"16\" width=\"16\" alt=\"troubled\""
                                + ( lastTroubleEvent.getNote() != null ? " title=\"" + escapeHtml4( lastTroubleEvent.getNote() ) + "\"" : "" )
                                + "/>";
                    }
                }
                buf.append( "<p><strong>Platform in Gemma:&nbsp;<a target=\"_blank\" href=\"" )
                        .append( entityUrlBuilder.fromBaseUrl( contextPath ).entity( arrayDesign ).web().toUriString() )
                        .append( "\">" ).append( escapeHtml4( gpl ) ).append( "</a></strong>" )
                        .append( trouble );
            } else {
                buf.append( "<p><strong>" ).append( escapeHtml4( gpl ) ).append( " [New to Gemma]</strong>" );
            }
        }
    }

    private File getInfoStoreFile() {
        return new File( downloadPath + File.separatorChar + GeoBrowserServiceImpl.GEO_DATA_STORE_FILE_NAME );
    }

    /**
     * Initialize local info from disk. Only fill in entries that are not already present.
     */
    private void initializeLocalInfo() {
        File f = this.getInfoStoreFile();
        if ( f.exists() ) {
            GeoBrowserServiceImpl.log.info( String.format( "Loading GEO browser info from %s...", f.getAbsolutePath() ) );
            StopWatch timer = StopWatch.createStarted();
            try ( FileInputStream fis = new FileInputStream( f ); ObjectInputStream ois = new ObjectInputStream( fis ) ) {
                //noinspection unchecked
                Map<String, GeoRecord> records = ( ( Map<String, GeoRecord> ) ois.readObject() );
                for ( Map.Entry<String, GeoRecord> e : records.entrySet() ) {
                    this.localInfo.putIfAbsent( e.getKey(), e.getValue() );
                }
                GeoBrowserServiceImpl.log.info( String.format( "Done GEO browser info. Loaded %d on-disk GEO records in %d ms.", records.size(), timer.getTime() ) );
            } catch ( Exception e ) {
                GeoBrowserServiceImpl.log.error( "Failed to load local GEO info from " + f.getAbsolutePath(), e );
            }
        }
    }

    private void initLocalRecord( String accession ) {
        if ( !localInfo.containsKey( accession ) ) {
            localInfo.put( accession, new GeoRecord() );
            localInfo.get( accession ).setGeoAccession( accession );
        }
    }

    /**
     * Save the cached GEO information for next time
     */
    private void saveLocalInfo() throws IOException {
        try ( FileOutputStream fos = new FileOutputStream( this.getInfoStoreFile() );
                ObjectOutputStream oos = new ObjectOutputStream( fos ) ) {
            oos.writeObject( this.localInfo );
        }
    }
}
