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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.persistence.util.Settings;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 *
 * See {@link <a href="http://www.ncbi.nlm.nih.gov/geo/info/geo_paccess.html">here</a>} for some information
 *
 * @author pavlidis
 */
public class GeoBrowser {

    private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    private static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gds&";
    private static final String EPLATRETRIEVE = "https://eutls.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gpl[ETYP]+AND+(mouse[orgn]+OR+human[orgn]+OR+rat[orgn])";
    private static final String ERETRIEVE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gse[ETYP]"; //no extra search term
    // Used by getGeoRecordsBySearchTerm (will look for GSE entries only)
    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gse[ETYP]+AND+";
    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";
    // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv
    // because of garbled tabbed lines returned
    // from GEO.
    private static final String GEO_BROWSE_URL = "https://www.ncbi.nlm.nih.gov/geo/browse/?view=series&zsort=date&mode=csv&page=";
    private static final Log log = LogFactory.getLog( GeoBrowser.class.getName() );
    private static final String NCBI_API_KEY = Settings.getString( "entrez.efetch.apikey" );
    XPathExpression characteristics;
    XPathExpression source;

    XPathExpression xaccession;
    XPathExpression xChannel;

    XPathFactory xFactory = XPathFactory.newInstance();
    XPathExpression xgpl;
    XPathExpression xnumSamples;
    XPathExpression xorganisms;
    XPath xpath = xFactory.newXPath();
    XPathExpression xPlataccession;
    XPathExpression xPlatformTech;
    XPathExpression xpubmed;
    XPathExpression xRelationType;
    XPathExpression xreleaseDate;
    XPathExpression xsummary;
    XPathExpression xtitle;

    XPathExpression xtype;
    private final String[] DATE_FORMATS = new String[] { "MMM dd, yyyy" };
    @SuppressWarnings("FieldCanBeLocal") // Constant is better
    private final String GEO_BROWSE_SUFFIX = "&display=";

    private PubMedXMLFetcher pubmedFetcher = new PubMedXMLFetcher();

    public GeoBrowser() {
        try {
            // Get relevant data from the XML file
            xaccession = xpath.compile( "//DocSum/Item[@Name='GSE']" );
            xtitle = xpath.compile( "//DocSum/Item[@Name='title']" );
            xnumSamples = xpath.compile( "//DocSum/Item[@Name='n_samples']" );
            xreleaseDate = xpath.compile( "//DocSum/Item[@Name='PDAT']" );
            xorganisms = xpath.compile( "//DocSum/Item[@Name='taxon']" );
            xgpl = xpath.compile( "//DocSum/Item[@Name='GPL']" );
            xsummary = xpath.compile( "//DocSum/Item[@Name='summary']" );
            xtype = xpath.compile( "//DocSum/Item[@Name='gdsType']" );
            xpubmed = xpath.compile( "//DocSum/Item[@Name='PubMedIds']" ); // list; also in miniml
            xChannel = xpath.compile( "//MINiML/Sample/Channel" );
            source = xpath.compile( "//Source" );
            characteristics = xpath.compile( "//Characteristics" );
            xRelationType = xpath.compile( "//MINiML/Series/Relation" );
            xPlataccession = xpath.compile( "//DocSum/Item[@Name='GPL']" );
            xPlatformTech = xpath.compile( "//DocSum/Item[@Name='ptechType']" );
            //   XPathExpression xsampleaccs = xpath.compile( "//Item[@Name='Sample']/Item[@Name='Accession']" );
        } catch ( Exception e ) {
            throw new RuntimeException( "Error setting up GEOBrowser xpaths: " + e.getMessage() );
        }

    }


    /**
     * Retrieve records for experiments
     * @param accessions of experiments
     * @return collection of records
     */
    public Collection<GeoRecord> getGeoRecords( Collection<String> accessions ) throws IOException {
        List<GeoRecord> records = new ArrayList<>();
        //https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=GSE[ETYP]+AND+(GSE100[accn]+OR+GSE101[accn])&retmax=5000&usehistory=y

        int chunkSize = 10;
        Collection<String> chunk = new ArrayList<>();
        for ( String acc : accessions ) {
            chunk.add( acc );

            if ( chunk.size() == chunkSize ) {
                String searchUrlString = GeoBrowser.ESEARCH + "(" + StringUtils.join( chunk, "[accn]+OR+" ) + "[accn])&usehistory=y";

                if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
                    searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
                }

                getGeoBasicRecords( records, searchUrlString );
                chunk.clear();
            }

        }

        if (!chunk.isEmpty()) {
            String searchUrlString = GeoBrowser.ESEARCH + "(" + StringUtils.join( chunk, "[accn]+OR+" ) + "[accn])&usehistory=y";

            if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
                searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
            }
            getGeoBasicRecords( records, searchUrlString );
        }

        return records;
    }

    private void getGeoBasicRecords( List<GeoRecord> records, String searchUrlString ) throws IOException {
        Document searchDocument;
        URL searchUrl = new URL( searchUrlString );
        URLConnection conn = searchUrl.openConnection();
        conn.connect();

        try ( InputStream is = conn.getInputStream() ) {

            GeoBrowser.docFactory.setIgnoringComments( true );
            GeoBrowser.docFactory.setValidating( false );

            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            searchDocument = builder.parse( is );
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count;
        try {
            count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        } catch ( NumberFormatException e ) {
            throw new IOException( "Could not parse count from: " + searchUrl );
        }

        if ( count == 0 )
            throw new IOException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text&query_key=" + queryId + "&WebEnv=" + cookie
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + NCBI_API_KEY : "" ) );

        StopWatch t = new StopWatch();
        t.start();
        conn = fetchUrl.openConnection();
        conn.connect();

        Document summaryDocument;
        try ( InputStream is = conn.getInputStream() ) {
            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            summaryDocument = builder.parse( is );

            Object accs = xaccession.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList accNodes = ( NodeList ) accs;

            Object titles = xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList titleNodes = ( NodeList ) titles;

            Object sampleCounts = xnumSamples.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList sampleNodes = ( NodeList ) sampleCounts;

            Object dates = xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList dateNodes = ( NodeList ) dates;

            Object organisms = xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList orgnNodes = ( NodeList ) organisms;

            Object platforms = xgpl.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList platformNodes = ( NodeList ) platforms;

            Object summary = xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList summaryNodes = ( NodeList ) summary;

            Object type = xtype.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList typeNodes = ( NodeList ) type;

            Object pubmed = xpubmed.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList pubmedNodes = ( NodeList ) pubmed;

            // Create GeoRecords using information parsed from XML file
            log.debug( "Got " + accNodes.getLength() + " XML records" );

            for ( int i = 0; i < accNodes.getLength(); i++ ) {

                GeoRecord record = new GeoRecord();
                record.setGeoAccession( "GSE" + accNodes.item( i ).getTextContent() );

                record.setSeriesType( typeNodes.item( i ).getTextContent() );
                if ( !record.getSeriesType().contains( "Expression profiling" ) ) {
                    continue;
                }
                Collection<String> taxa = this.getTaxonCollection( orgnNodes.item( i ).getTextContent() );

                record.setOrganisms( taxa );

                record.setTitle( titleNodes.item( i ).getTextContent() );

                record.setNumSamples( Integer.parseInt( sampleNodes.item( i ).getTextContent() ) );

                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );

                // there can be more than one, delimited by ';'
                String[] platformlist = StringUtils.split( platformNodes.item( i ).getTextContent(), ";" );
                List<String> finalPlatformIds = new ArrayList<>();
                for ( String p : platformlist ) {
                    finalPlatformIds.add( "GPL" + p );
                }

                String platformS = StringUtils.join( finalPlatformIds, ";" );
                record.setPlatform( platformS );
                record.setSummary( summaryNodes.item( i ).getTextContent() );
                record.setPubMedIds( StringUtils.strip( pubmedNodes.item( i ).getTextContent() ).replaceAll( "\\n", "," ).replaceAll( "\\s*", "" ) );
                record.setSuperSeries( record.getTitle().contains( "SuperSeries" ) || record.getSummary().contains( "SuperSeries" ) );
                records.add( record );

            }

        } catch ( IOException | ParserConfigurationException | ParseException | XPathExpressionException | SAXException e ) {
            log.error( "Could not parse data: " + searchUrl, e );
        }

        if ( records.isEmpty() ) {
            GeoBrowser.log.warn( "No records obtained" );
        } else {
            log.debug( "Parsed " + records.size() + " records" );
        }
    }

    /**
     * A bit hacky, can be improved. Limited to human, mouse, rat, is not guaranteed to get everything, though as of
     * 7/2021, this is sufficient (~8000 platforms)
     *
     * @return all relevant platforms up to single-query limit of NCBI
     * @throws IOException
     */
    public Collection<GeoRecord> getAllGEOPlatforms() throws IOException {

        List<GeoRecord> records = new ArrayList<>();

        String searchUrlString;

        searchUrlString = GeoBrowser.EPLATRETRIEVE + "&retmax=" + 10000 + "&usehistory=y"; //10k is the limit.

        if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
            searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
        }

        URL searchUrl = new URL( searchUrlString );

        Document searchDocument;
        URLConnection conn = searchUrl.openConnection();
        conn.connect();
        try ( InputStream is = conn.getInputStream() ) {

            GeoBrowser.docFactory.setIgnoringComments( true );
            GeoBrowser.docFactory.setValidating( false );

            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            searchDocument = builder.parse( is );
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count;
        try {
            count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        } catch ( NumberFormatException e ) {
            throw new IOException( "Could not parse count from: " + searchUrl );
        }

        if ( count == 0 )
            throw new IOException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text" + "&query_key=" + queryId + "&retmax="
                        + 10000 + "&WebEnv=" + cookie
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + NCBI_API_KEY : "" ) );

        StopWatch t = new StopWatch();
        t.start();
        conn = fetchUrl.openConnection();
        conn.connect();

        Document summaryDocument;
        try ( InputStream is = conn.getInputStream() ) {
            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            summaryDocument = builder.parse( is );

            Object accessions = xPlataccession.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList accNodes = ( NodeList ) accessions;

            Object titles = xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList titleNodes = ( NodeList ) titles;

            Object summary = xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList summaryNodes = ( NodeList ) summary;

            Object tech = xPlatformTech.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList techNodes = ( NodeList ) tech;

            Object organisms = xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList orgnNodes = ( NodeList ) organisms;

            Object dates = xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList dateNodes = ( NodeList ) dates;

            // consider n_samples (number of elements) and the number of GSEs, but not every record has them, so it would be trickier.

            log.debug( "Got " + accNodes.getLength() + " XML records" );

            for ( int i = 0; i < accNodes.getLength(); i++ ) {

                GeoRecord record = new GeoRecord();

                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );
                record.setGeoAccession( "GPL" + accNodes.item( i ).getTextContent() );
                record.setTitle( titleNodes.item( i ).getTextContent() );
                record.setOrganisms( null );
                record.setSummary( summaryNodes.item( i ).getTextContent() );
                record.setSeriesType( techNodes.item( i ).getTextContent() ); // slight abuse
                Collection<String> taxa = this.getTaxonCollection( orgnNodes.item( i ).getTextContent() );
                record.setOrganisms( taxa );
                records.add( record );
            }

        } catch ( ParserConfigurationException | XPathExpressionException | SAXException | DOMException | ParseException e ) {
            log.error( e.getMessage() );

        }

        return records;

    }

    /**
     * Provides more details than getRecentGeoRecords. Performs an E-utilities query of the GEO database with the given
     * searchTerms (search terms can be ommitted). Returns at most pageSize records. Does some screening of results for
     * expression studies, and (optionally) taxa. This is used for identifying data sets for loading
     *
     * @param  start          start an offset to retrieve batches
     * @param  pageSize       page size how many to retrive
     * @param  searchTerms    search terms in NCBI Entrez query format
     * @param  detailed       if true, additional information is fetched (slower)
     * @param  allowedTaxa    if not null, data sets not containing any of these taxa will be skipped
     * @param  limitPlatforms not null or empty, platforms to limit the query to (combining with searchTerms not
     *                        supported yet)
     * @return list of GeoRecords
     * @throws IOException    if there is a problem obtaining or manipulating the file (some exceptions are not thrown
     *                        and
     *                        just logged)
     */
    public List<GeoRecord> getGeoRecordsBySearchTerm( String searchTerms, int start, int pageSize, boolean detailed, Collection<String> allowedTaxa,
            Collection<String> limitPlatforms )
            throws IOException {

        List<GeoRecord> records = new ArrayList<>();

        String platformLimitClause = "";
        if ( limitPlatforms != null && !limitPlatforms.isEmpty() ) {
            platformLimitClause = "%20AND%20(" + StringUtils.join( limitPlatforms, "[ACCN]%20OR%20" ) + "[ACCN])";
        }

        String searchUrlString;
        if ( StringUtils.isBlank( searchTerms ) ) {
            searchUrlString = GeoBrowser.ERETRIEVE + platformLimitClause + "&retstart=" + start + "&retmax=" + pageSize + "&usehistory=y";
        } else {
            // FIXME: could allow merging in of platformLimitClause. Should really rewrite the way we form these urls to be more modular.
            searchUrlString = GeoBrowser.ESEARCH + searchTerms + "&retstart=" + start + "&retmax=" + pageSize
                    + "&usehistory=y";
        }

        if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
            searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
        }

        URL searchUrl = new URL( searchUrlString );

        Document searchDocument;
        URLConnection conn = searchUrl.openConnection();
        conn.connect();
        try ( InputStream is = conn.getInputStream() ) {

            GeoBrowser.docFactory.setIgnoringComments( true );
            GeoBrowser.docFactory.setValidating( false );

            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            searchDocument = builder.parse( is );
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count;
        try {
            count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        } catch ( NumberFormatException e ) {
            throw new IOException( "Could not parse count from: " + searchUrl );
        }

        if ( count == 0 )
            throw new IOException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text" + "&query_key=" + queryId + "&retstart=" + start + "&retmax="
                        + pageSize + "&WebEnv=" + cookie
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + NCBI_API_KEY : "" ) );

        StopWatch t = new StopWatch();
        t.start();
        conn = fetchUrl.openConnection();
        conn.connect();

        Document summaryDocument;
        try ( InputStream is = conn.getInputStream() ) {
            DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
            summaryDocument = builder.parse( is );

            Object accessions = xaccession.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList accNodes = ( NodeList ) accessions;

            Object titles = xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList titleNodes = ( NodeList ) titles;

            Object sampleCounts = xnumSamples.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList sampleNodes = ( NodeList ) sampleCounts;

            Object dates = xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList dateNodes = ( NodeList ) dates;

            Object organisms = xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList orgnNodes = ( NodeList ) organisms;

            Object platforms = xgpl.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList platformNodes = ( NodeList ) platforms;

            Object summary = xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList summaryNodes = ( NodeList ) summary;

            Object type = xtype.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList typeNodes = ( NodeList ) type;

            Object pubmed = xpubmed.evaluate( summaryDocument, XPathConstants.NODESET );
            NodeList pubmedNodes = ( NodeList ) pubmed;

            //                Object samples = xsamples.evaluate( summaryDocument, XPathConstants.NODESET );
            //                NodeList sampleLists = ( NodeList ) samples;

            // Create GeoRecords using information parsed from XML file
            log.debug( "Got " + accNodes.getLength() + " XML records" );

            for ( int i = 0; i < accNodes.getLength(); i++ ) {

                GeoRecord record = new GeoRecord();

                record.setGeoAccession( "GSE" + accNodes.item( i ).getTextContent() );

                record.setSeriesType( typeNodes.item( i ).getTextContent() );
                if ( !record.getSeriesType().contains( "Expression profiling" ) ) {
                    continue;
                }

                Collection<String> taxa = this.getTaxonCollection( orgnNodes.item( i ).getTextContent() );
                if ( allowedTaxa != null && !allowedTaxa.isEmpty() ) {
                    boolean useableTaxa = false;
                    for ( String ta : taxa ) {
                        if ( allowedTaxa.contains( ta ) ) {
                            useableTaxa = true;
                        }
                    }
                    if ( !useableTaxa ) {
                        continue;
                    }
                }
                record.setOrganisms( taxa );

                record.setTitle( titleNodes.item( i ).getTextContent() );

                record.setNumSamples( Integer.parseInt( sampleNodes.item( i ).getTextContent() ) );

                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );

                // there can be more than one, delimited by ';'
                String[] platformlist = StringUtils.split( platformNodes.item( i ).getTextContent(), ";" );
                List<String> finalPlatformIds = new ArrayList<>();
                for ( String p : platformlist ) {
                    finalPlatformIds.add( "GPL" + p );
                }

                String platformS = StringUtils.join( finalPlatformIds, ";" );

                record.setPlatform( platformS );

                record.setSummary( summaryNodes.item( i ).getTextContent() );
                record.setPubMedIds( StringUtils.strip( pubmedNodes.item( i ).getTextContent() ).replaceAll( "\\n", "," ).replaceAll( "\\s*", "" ) );
                record.setSuperSeries( record.getTitle().contains( "SuperSeries" ) || record.getSummary().contains( "SuperSeries" ) );

                if ( detailed ) {
                    getDetails( record );
                }

                records.add( record );

                if ( detailed ) {
                    // without details this goes a lot quicker so feedback isn't very important
                    System.err.println(
                            "Processed: " + record.getGeoAccession() + ", " + record.getNumSamples() + " samples, " + t.getTime() / 1000 + "s " );
                }
                t.reset();
                t.start();
            }

        } catch ( IOException | ParserConfigurationException | ParseException | XPathExpressionException | SAXException e ) {
            log.error( "Could not parse data: " + searchUrl, e );
        }

        if ( records.isEmpty() ) {
            GeoBrowser.log.warn( "No records obtained" );
        } else {
            log.debug( "Parsed " + records.size() + " records" );
        }

        return records;

    }

    /**
     * Retrieves and parses tab delimited file from GEO. File contains pageSize GEO records starting from startPage. The
     * retrieved information is pretty minimal.
     *
     * @param  startPage      start page
     * @param  pageSize       page size
     * @return list of GeoRecords
     * @throws IOException    if there is a problem while manipulating the file
     * @throws ParseException if there is a parsing problem
     */
    public List<GeoRecord> getRecentGeoRecords( int startPage, int pageSize ) throws IOException, ParseException {

        if ( startPage < 0 || pageSize < 0 )
            throw new IllegalArgumentException( "Values must be greater than zero " );

        List<GeoRecord> records = new ArrayList<>();
        URL url;
        try {
            url = new URL( GEO_BROWSE_URL + startPage + GEO_BROWSE_SUFFIX + pageSize );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Invalid URL: " + GEO_BROWSE_URL + startPage + GEO_BROWSE_SUFFIX + pageSize,
                    e );
        }

        URLConnection conn = url.openConnection();
        conn.connect();
        try ( InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {

            // We are getting a tab delimited file.

            // Read columns headers.
            String headerLine = br.readLine();
            String[] headers = StringUtil.csvSplit( headerLine );

            // Map column names to their indices (handy later).
            Map<String, Integer> columnNameToIndex = new HashMap<>();
            for ( int i = 0; i < headers.length; i++ ) {
                columnNameToIndex.put( headers[i], i );
            }

            // Read the rest of the file.
            String line;
            while ( ( line = br.readLine() ) != null ) {
                String[] fields = StringUtil.csvSplit( line );

                GeoRecord geoRecord = new GeoRecord();
                geoRecord.setGeoAccession( fields[columnNameToIndex.get( "Accession" )] );
                geoRecord.setTitle( StringUtils.strip( fields[columnNameToIndex.get( "Title" )]
                        .replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" ) ) );

                String sampleCountS = fields[columnNameToIndex.get( "Sample Count" )];
                if ( StringUtils.isNotBlank( sampleCountS ) ) {
                    try {
                        geoRecord.setNumSamples( Integer.parseInt( sampleCountS ) );
                    } catch ( NumberFormatException e ) {
                        throw new RuntimeException( "Could not parse sample count: " + sampleCountS );
                    }
                } else {
                    GeoBrowser.log.warn( "No sample count for " + geoRecord.getGeoAccession() );
                }
                geoRecord.setContactName(
                        fields[columnNameToIndex.get( "Contact" )].replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" ) );

                String[] taxons = fields[columnNameToIndex.get( "Taxonomy" )]
                        .replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" ).split( ";" );
                geoRecord.getOrganisms().addAll( Arrays.asList( taxons ) );

                Date date = DateUtils.parseDate( fields[columnNameToIndex.get( "Release Date" )]
                        .replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" ), DATE_FORMATS );
                geoRecord.setReleaseDate( date );

                geoRecord.setSeriesType( fields[columnNameToIndex.get( "Series Type" )] );

                records.add( geoRecord );
            }

        }

        if ( records.isEmpty() ) {
            GeoBrowser.log.warn( "No records obtained" );
        }
        return records;

    }

    /**
     * exposed for testing
     *
     * @param  record
     * @param  is
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException
     */
    void parseMINiML( GeoRecord record, InputStream is ) {

        try {

            // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE180363&targ=gse&form=xml&view=full
            DocumentBuilder builderD = GeoBrowser.docFactory.newDocumentBuilder(); // can move out
            Document detailsDocument = builderD.parse( is );
            Object relationTypes = xRelationType.evaluate( detailsDocument, XPathConstants.NODESET );
            NodeList relTypeNodes = ( NodeList ) relationTypes;

            for ( int i = 0; i < relTypeNodes.getLength(); i++ ) {

                String relType = relTypeNodes.item( i ).getAttributes().getNamedItem( "type" ).getTextContent();
                String relTo = relTypeNodes.item( i ).getAttributes().getNamedItem( "target" ).getTextContent();

                if ( relType.startsWith( "SubSeries" ) ) {
                    record.setSubSeries( true );
                    record.setSubSeriesOf( relTo );
                }

            }
        } catch ( IOException | ParserConfigurationException | SAXException | XPathExpressionException e ) {
            log.error( e.getMessage() + " while processing MINiML for " + record.getGeoAccession()
                    + ", subseries status will not be determined." );
        }
    }

    /**
     * exposed for testing
     *
     * @param  record
     * @param  isd
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException
     */
    void parseSampleMiNIML( GeoRecord record, InputStream isd )
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        // Source, Characteristics
        DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder(); // can move out
        Document detailsDocument = builder.parse( isd );
        Object channels = xChannel.evaluate( detailsDocument, XPathConstants.NODESET );
        NodeList channelNodes = ( NodeList ) channels;
        Set<String> props = new HashSet<>(); // expect duplicate terms

        for ( int i = 0; i < channelNodes.getLength(); i++ ) {
            Node item = channelNodes.item( i );
            NodeList sources = ( NodeList ) source.evaluate( item, XPathConstants.NODESET );
            for ( int k = 0; k < sources.getLength(); k++ ) {
                String s = sources.item( k ).getTextContent();
                String v = StringUtils.strip( s );
                if ( v.matches( "[0-9]+" ) ) continue; // skip unadorned numbers

                props.add( v );
            }
            NodeList chars = ( NodeList ) characteristics.evaluate( item, XPathConstants.NODESET );
            for ( int k = 0; k < chars.getLength(); k++ ) {
                String s = chars.item( k ).getTextContent();
                String v = StringUtils.strip( s );
                if ( v.matches( "[0-9]+" ) ) continue;
                props.add( v );
            }
        }
        record.setSampleDetails( StringUtils.join( props, ";" ) );
    }

    /**
     * Do another query to NCBI to fetch additional information not present in the eutils. Specifically: whether this is
     * a subseries, and MeSH headings associated with any publication.
     *
     * @param  record
     * @throws MalformedURLException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void getDetails( GeoRecord record ) {
        URL miniMLURL = null;
        try {

            if ( !record.isSuperSeries() ) {
                miniMLURL = new URL(
                        "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?targ=gse&form=xml&view=full&acc=" + record.getGeoAccession() );

                /*
                 * I can't find a better way to get subseries info: the eutils doesn't provide this
                 * information other than mentioning (in the summary text) when a series is a superseries. Determining
                 * subseries
                 * status is impossible without another query. I don't think batch queries for MiniML is possible.
                 */

                URLConnection dconn = miniMLURL.openConnection();
                dconn.connect();

                try ( InputStream isd = dconn.getInputStream() ) {
                    parseMINiML( record, isd );
                }
            }

            getSampleDetails( record );

            // another query. Note that new Pubmed IDs generally don't have mesh headings yet, so this might not be that useful.
            if ( StringUtils.isNotBlank( record.getPubMedIds() ) ) {
                getMeshHeadings( record );
            }

            // get sample information
            // https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSM5230452&targ=self&form=xml&view=full

        } catch ( IOException e ) {
            // This is particularly common (and reproducible) for data sets with >300 samples, but can happen other times
            log.error( "Error while getting details for " + record.getGeoAccession() + ": " + e.getMessage() );
        }

    }

    /**
     * @param  record      to process
     * @throws IOException
     */
    private void getMeshHeadings( GeoRecord record ) throws IOException {
        try {
            Collection<String> meshheadings = new ArrayList<>();
            List<String> idlist = Arrays.asList( record.getPubMedIds().split( "\\s+" ) );
            List<Integer> idints = new ArrayList<>();
            for ( String s : idlist ) {
                try {
                    idints.add( Integer.parseInt( s ) );
                } catch ( NumberFormatException e ) {
                    log.warn( "Invalid PubMed ID '" + s + "' for " + record.getGeoAccession() );
                }
            }
            Collection<BibliographicReference> refs = pubmedFetcher.retrieveByHTTP( idints );
            for ( BibliographicReference ref : refs ) {
                for ( MedicalSubjectHeading mesh : ref.getMeshTerms() ) {
                    meshheadings.add( mesh.getTerm() );
                }
            }

            if ( !meshheadings.isEmpty() )
                record.setMeshHeadings( StringUtils.join( meshheadings, ";" ) );
        } catch ( IOException e ) {
            log.error( "Could not get MeSH headings for " + record.getGeoAccession() + ": " + e.getMessage() );
        }
    }

    /**
     * Fetch and parse MINiML for samples.
     *
     * @param record
     */
    private void getSampleDetails( GeoRecord record ) {

        try {

            // Fetch miniML for the samples.
            // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE171682&targ=gsm&form=xml&view=full
            URL sampleMINIMLURL = new URL(
                    "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?targ=gsm&form=xml&view=full&acc=" + record.getGeoAccession() );
            URLConnection sconn = sampleMINIMLURL.openConnection();
            sconn.connect();

            try ( InputStream isd = sconn.getInputStream() ) {

                parseSampleMiNIML( record, isd );

            }

        } catch ( IOException | ParserConfigurationException | SAXException | XPathExpressionException e ) {

            log.error( e.getMessage() + " while processing MINiML for " + record.getGeoAccession()
                    + ", sample details will not be obtained" );
        }
    }

    /**
     * Extracts taxon names from input string; returns a collection of taxon names
     *
     * @param  input input
     * @return taxon names
     */
    private Collection<String> getTaxonCollection( String input ) {
        Collection<String> taxa = new ArrayList<>();

        input = input.replace( "; ", ";" );
        String[] taxonArray = input.split( ";" );

        for ( int i = 0; i < Array.getLength( taxonArray ); i++ ) {
            taxa.add( taxonArray[i].trim() );
        }
        return taxa;
    }

}
