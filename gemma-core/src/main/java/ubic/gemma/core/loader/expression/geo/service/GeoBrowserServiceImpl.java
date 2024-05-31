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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ubic.gemma.core.loader.entrez.EutilFetch;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.core.config.Settings;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Autowired
    protected ExpressionExperimentService expressionExperimentService;

    @Autowired
    protected ArrayDesignService arrayDesignService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    private final Map<String, GeoRecord> localInfo = new ConcurrentHashMap<>();
    private XPathExpression xgse;
    private XPathExpression xtitle;
    private XPathExpression xgpls;
    private XPathExpression xsummary;

    private final ExecutorService es = Executors.newSingleThreadExecutor();

    @Override
    public void afterPropertiesSet() throws XPathExpressionException {
        XPathFactory xf = XPathFactory.newInstance();
        XPath xpath = xf.newXPath();
        // xgds = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GDS\"][1]/text()" );
        xgse = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GSE\"][1]/text()" );
        xtitle = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"title\"][1]/text()" );
        xgpls = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GPL\"]/text()" );
        xsummary = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"summary\"][1]/text()" );
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
        String details = EutilFetch.fetch( "gds", accession, 25 );

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
    public List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException, ParseException {
        GeoBrowser browser = new GeoBrowser();
        List<GeoRecord> records = browser.getRecentGeoRecords( start, count );

        if ( records.isEmpty() )
            return records;

        return this.filterGeoRecords( records );
    }

    @Override
    public List<GeoRecord> searchGeoRecords( String searchString, int start, int count, boolean detailed ) throws IOException {
        GeoBrowser browser = new GeoBrowser();
        List<GeoRecord> records = browser.getGeoRecordsBySearchTerm( searchString, start, count, detailed, null, null );

        return this.filterGeoRecords( records );
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
    String formatDetails( String details, String contextPath ) throws IOException {

        /*
         * Bug 2690. There must be a better way.
         */
        details = details.replaceAll( "encoding=\"UTF-8\"", "" );

        try {
            Document document = EutilFetch.parseStringInputStream( details );

            String gse = "GSE" + xgse.evaluate( document, XPathConstants.STRING );
            String title = ( String ) xtitle.evaluate( document, XPathConstants.STRING );
            NodeList gpls = ( NodeList ) xgpls.evaluate( document, XPathConstants.NODESET );
            String summary = ( String ) xsummary.evaluate( document, XPathConstants.STRING );

            StringBuilder buf = new StringBuilder();
            buf.append( "<div class=\"small\">" );

            ExpressionExperiment ee = this.expressionExperimentService.findByShortName( gse );

            if ( ee != null ) {
                buf.append( "\n<p><strong><a target=\"_blank\" href=\"" ).append( contextPath )
                        .append( "/expressionExperiment/showExpressionExperiment.html?id=" ).append( ee.getId() )
                        .append( "\">" ).append( gse ).append( "</a></strong>" );
            } else {
                buf.append( "\n<p><strong>" ).append( gse ).append( " [new to Gemma]</strong>" );
            }

            buf.append( "<p>" ).append( title ).append( "</p>\n" );
            buf.append( "<p>" ).append( summary ).append( "</p>\n" );

            this.formatArrayDetails( gpls, buf, contextPath );

            buf.append( "</div>" );
            details = buf.toString();

            // }
        } catch ( ParserConfigurationException | SAXException | XPathExpressionException e ) {
            throw new RuntimeException( e );
        }

        return details;
    }

    private List<GeoRecord> filterGeoRecords( List<GeoRecord> records ) {
        ExternalDatabase geo = externalDatabaseService.findByName( "GEO" );
        Collection<GeoRecord> toRemove = new HashSet<>();
        assert geo != null;
        rec:
        for ( GeoRecord record : records ) {

            if ( record.getNumSamples() < GeoBrowserServiceImpl.MIN_SAMPLES ) {
                toRemove.add( record );
            }

            Collection<String> organisms = record.getOrganisms();
            if ( organisms == null || organisms.size() == 0 ) {
                continue;
            }
            int i = 0;
            for ( String string : organisms ) {
                Taxon t = taxonService.findByCommonName( string );
                if ( t == null ) {
                    t = taxonService.findByScientificName( string );
                    if ( t == null ) {
                        toRemove.add( record );
                        continue rec;
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
        }

        records.removeAll( toRemove );

        return records;

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
                        trouble = "&nbsp;<img src='" + contextPath
                                + "/images/icons/warning.png' height='16' width='16' alt=\"troubled\" title=\""
                                + lastTroubleEvent.getNote() + "\"/>";
                    }
                }
                buf.append( "<p><strong>Platform in Gemma:&nbsp;<a target=\"_blank\" href=\"" )
                        .append( contextPath ).append( "/arrays/showArrayDesign.html?id=" )
                        .append( arrayDesign.getId() ).append( "\">" ).append( gpl ).append( "</a></strong>" )
                        .append( trouble );
            } else {
                buf.append( "<p><strong>" ).append( gpl ).append( " [New to Gemma]</strong>" );
            }
        }
    }

    private File getInfoStoreFile() {
        String path = Settings.getDownloadPath();
        return new File( path + File.separatorChar + GeoBrowserServiceImpl.GEO_DATA_STORE_FILE_NAME );
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
