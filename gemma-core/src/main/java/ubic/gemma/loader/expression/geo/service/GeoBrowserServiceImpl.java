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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.lib.StringInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.entrez.EutilFetch;
import ubic.gemma.loader.expression.geo.model.GeoRecord;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.Settings;

/**
 * @author pavlidis
 * @version $Id$
 */
@Component
public class GeoBrowserServiceImpl implements GeoBrowserService {
    private static final int MIN_SAMPLES = 5;
    private static final String GEO_DATA_STORE_FILE_NAME = "GEODataStore";

    @Autowired
    protected ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    protected ArrayDesignService arrayDesignService;

    @Autowired
    private AuditTrailService auditTrailService;

    private Map<String, GeoRecord> localInfo;

    private XPathExpression xgds;
    private XPathExpression xgse;
    private XPathExpression xtitle;
    private XPathExpression xgpls;
    private XPathExpression xsummary;
    private XPathExpression xsamples;

    private static Log log = LogFactory.getLog( GeoBrowserServiceImpl.class.getName() );

    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        initializeLocalInfo();

        XPathFactory xf = XPathFactory.newInstance();
        XPath xpath = xf.newXPath();
        xgds = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GDS\"][1]/text()" );
        xgse = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GSE\"][1]/text()" );
        xtitle = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"title\"][1]/text()" );
        xgpls = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"GPL\"]/text()" );
        xsummary = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"summary\"][1]/text()" );
        xsamples = xpath.compile( "/eSummaryResult/DocSum/Item[@Name=\"Samples\"]/text()" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoBrowserService#getDetails(java.lang.String)
     */
    @Override
    public String getDetails( String accession ) throws IOException {
        /*
         * The maxrecords is > 1 because it return platforms as well (and there are series with as many as 13 platforms
         * ... leaving some headroom)
         */
        String details = EutilFetch.fetch( "gds", accession, 25 );

        initLocalRecord( accession );

        /*
         * increment click counts.
         */
        localInfo.get( accession ).setPreviousClicks( localInfo.get( accession ).getPreviousClicks() + 1 );

        saveLocalInfo();

        return formatDetails( details );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoBrowserService#getRecentGeoRecords(int, int)
     */
    @Override
    public List<GeoRecord> getRecentGeoRecords( int start, int count ) throws IOException, ParseException {
        GeoBrowser browser = new GeoBrowser();
        List<GeoRecord> records = browser.getRecentGeoRecords( start, count );

        if ( records.isEmpty() ) return records;

        return filterGeoRecords( records );
    }

    @Override
    public List<GeoRecord> searchGeoRecords( String searchString, int start, int count ) throws IOException,
            ParseException {
        GeoBrowser browser = new GeoBrowser();
        // Change this method to browser.getGeoRecordsBySearchTerm when implemented in GeoBrowser.java
        List<GeoRecord> records = browser.getGeoRecordsBySearchTerm( searchString, start, count );

        return filterGeoRecords( records );
    }

    /*
     * 
     * @param records
     * 
     * @return
     */
    private List<GeoRecord> filterGeoRecords( List<GeoRecord> records ) {
        ExternalDatabase geo = externalDatabaseService.find( "GEO" );
        Collection<GeoRecord> toRemove = new HashSet<GeoRecord>();
        assert geo != null;
        rec: for ( GeoRecord record : records ) {

            if ( record.getNumSamples() < MIN_SAMPLES ) {
                toRemove.add( record );
            }

            Collection<String> organisms = record.getOrganisms();
            if ( organisms == null || organisms.size() == 0 ) {
                continue rec;
            }
            int i = 1;
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

                record.setUsable( localInfo.containsKey( acc ) ? localInfo.get( acc ).isUsable() : true );

            }
            i++;
        }

        for ( GeoRecord record : toRemove ) {
            records.remove( record );
        }

        return records;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoBrowserService#toggleUsability(java.lang.String)
     */
    @Override
    public boolean toggleUsability( String accession ) {

        initLocalRecord( accession );

        localInfo.get( accession ).setUsable( !localInfo.get( accession ).isUsable() );

        saveLocalInfo();

        return localInfo.get( accession ).isUsable();
    }

    /**
     * @param gpls
     * @param buf
     */
    private void formatArrayDetails( NodeList gpls, StringBuilder buf ) {
        Set<String> seenGpl = new HashSet<String>();
        for ( int i = 0; i < gpls.getLength(); i++ ) {
            String gpl = "GPL" + gpls.item( i ).getNodeValue();
            if ( gpl.contains( ";" ) ) continue;
            if ( seenGpl.contains( gpl ) ) continue;
            seenGpl.add( gpl );
            ArrayDesign arrayDesign = arrayDesignService.findByShortName( gpl );

            if ( arrayDesign != null ) {
                arrayDesign = arrayDesignService.thawLite( arrayDesign );
                String trouble = "";
                if ( arrayDesign.getStatus().getTroubled() ) {
                    AuditEvent lastTroubleEvent = auditTrailService.getLastTroubleEvent( arrayDesign );
                    if ( lastTroubleEvent != null ) {
                        trouble = "&nbsp;<img src='/Gemma/images/icons/warning.png' height='16' width='16' alt=\"troubled\" title=\""
                                + lastTroubleEvent.getNote() + "\"/>";
                    }
                }
                buf.append( "<p><strong>Platform in Gemma:&nbsp;<a target=\"_blank\" href=\"/Gemma/arrays/showArrayDesign.html?id="
                        + arrayDesign.getId() + "\">" + gpl + "</a></strong>" + trouble );
            } else {
                buf.append( "<p><strong>" + gpl + " [New to Gemma]</strong>" );
            }
        }
    }

    /**
     * @return
     */
    private File getInfoStoreFile() {
        String path = Settings.getDownloadPath();
        File f = new File( path + File.separatorChar + GEO_DATA_STORE_FILE_NAME );
        return f;
    }

    /**
     * 
     */
    private void initializeLocalInfo() {
        File f = getInfoStoreFile();
        try {
            if ( f.exists() ) {
                FileInputStream fis = new FileInputStream( f );
                ObjectInputStream ois = new ObjectInputStream( fis );
                this.localInfo = ( Map<String, GeoRecord> ) ois.readObject();
                ois.close();
                fis.close();
            } else {
                this.localInfo = new HashMap<String, GeoRecord>();
            }
        } catch ( Exception e ) {
            log.error( "Failed to load local GEO info, reinitializing..." );
            this.localInfo = new HashMap<String, GeoRecord>();
        }
        assert this.localInfo != null;
    }

    /**
     * @param accession
     */
    private void initLocalRecord( String accession ) {
        assert localInfo != null;
        if ( !localInfo.containsKey( accession ) ) {
            localInfo.put( accession, new GeoRecord() );
            localInfo.get( accession ).setGeoAccession( accession );
        }
    }

    /**
     * 
     */
    private void saveLocalInfo() {
        if ( this.localInfo == null ) return;
        try {
            FileOutputStream fos = new FileOutputStream( getInfoStoreFile() );
            ObjectOutputStream oos = new ObjectOutputStream( fos );
            oos.writeObject( this.localInfo );
            oos.flush();
            oos.close();
        } catch ( Exception e ) {
            log.error( "Failed to save local GEO info", e );
        }
    }

    /**
     * Take the details string from GEO and make it nice. Add links to series and platforms that are already in gemma.
     * 
     * @param details XML from eSummary
     * @return HTML-formatted
     * @throws IOException
     */
    protected String formatDetails( String details ) throws IOException {
        try {

            /*
             * Bug 2690. There must be a better way.
             */
            details = details.replaceAll( "encoding=\"UTF-8\"", "" );

            DocumentBuilder builder = factory.newDocumentBuilder();
            StringInputStream is = new StringInputStream( details );

            Document document = builder.parse( is );

            NodeList samples = ( NodeList ) xsamples.evaluate( document, XPathConstants.NODESET );

            String gds = ( String ) xgds.evaluate( document, XPathConstants.STRING ); // FIXME, use this.
            String gse = "GSE" + ( String ) xgse.evaluate( document, XPathConstants.STRING );
            String title = ( String ) xtitle.evaluate( document, XPathConstants.STRING );
            NodeList gpls = ( NodeList ) xgpls.evaluate( document, XPathConstants.NODESET ); // FIXME get description.
            String summary = ( String ) xsummary.evaluate( document, XPathConstants.STRING );

            StringBuilder buf = new StringBuilder();
            buf.append( "<div class=\"small\">" );

            ExpressionExperiment ee = this.expressionExperimentService.findByShortName( gse );

            if ( ee != null ) {
                buf.append( "\n<p><strong><a target=\"_blank\" href=\"/Gemma/expressionExperiment/showExpressionExperiment.html?id="
                        + ee.getId() + "\">" + gse + "</a></strong>" );
            } else {
                buf.append( "\n<p><strong>" + gse + " [new to Gemma]</strong>" );
            }

            buf.append( "<p>" + title + "</p>\n" );
            buf.append( "<p>" + summary + "</p>\n" );

            formatArrayDetails( gpls, buf );

            for ( int i = 0; i < samples.getLength(); i++ ) {
                // samples.item( i )
                // FIXME use this.
            }

            buf.append( "</div>" );
            details = buf.toString();

            // }
        } catch ( ParserConfigurationException e ) {
            throw new RuntimeException( e );
        } catch ( SAXException e ) {
            throw new RuntimeException( e );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( e );
        }

        return details;
    }

}
