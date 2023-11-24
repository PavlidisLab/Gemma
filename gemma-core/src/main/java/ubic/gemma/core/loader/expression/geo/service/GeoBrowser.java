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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 * <p>
 * See <a href="https://www.ncbi.nlm.nih.gov/geo/info/geo_paccess.html">Programmatic access to GEO</a> for
 * some information.
 *
 * @author pavlidis
 */
public class GeoBrowser {

    /**
     * Maximum number of retries when retrieving a document.
     */
    private static final int MAX_RETRIES = 3;

    /**
     * Maximum size of a MINiML record in bytes.
     * @see #openUrlWithMaxSize(URL, long)
     */
    private static final long MAX_MINIML_RECORD_SIZE = 100_000_000;

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

    private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    private static final XPathExpression characteristics;
    private static final XPathExpression source;
    private static final XPathExpression xaccession;
    private static final XPathExpression xChannel;
    private static final XPathExpression xLibraryStrategy;
    private static final XPathExpression xgpl;
    private static final XPathExpression xnumSamples;
    private static final XPathExpression xorganisms;
    private static final XPathExpression xPlataccession;
    private static final XPathExpression xPlatformTech;
    private static final XPathExpression xpubmed;
    private static final XPathExpression xRelationType;
    private static final XPathExpression xOverallDesign;
    private static final XPathExpression xreleaseDate;
    private static final XPathExpression xsummary;
    private static final XPathExpression xtitle;
    private static final XPathExpression xtype;

    /* locale */
    private static final Locale GEO_LOCALE = Locale.ENGLISH;
    private static final String[] GEO_DATE_FORMATS = new String[] { "MMM dd, yyyy" };

    @SuppressWarnings("FieldCanBeLocal") // Constant is better
    private static final String GEO_BROWSE_SUFFIX = "&display=";

    static {
        GeoBrowser.docFactory.setIgnoringComments( true );
        GeoBrowser.docFactory.setValidating( false );
        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();
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
            xLibraryStrategy = xpath.compile( "//MINiML/Sample/Library-Strategy" );
            source = xpath.compile( "//Source" );
            characteristics = xpath.compile( "//Characteristics" );
            xRelationType = xpath.compile( "//MINiML/Series/Relation" );
            xOverallDesign = xpath.compile( "//MINiML/Series/Overall-Design" );
            xPlataccession = xpath.compile( "//DocSum/Item[@Name='GPL']" );
            xPlatformTech = xpath.compile( "//DocSum/Item[@Name='ptechType']" );
            //   XPathExpression xsampleaccs = xpath.compile( "//Item[@Name='Sample']/Item[@Name='Accession']" );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( e );
        }
    }

    private final PubMedXMLFetcher pubmedFetcher = new PubMedXMLFetcher();

    /**
     * Retrieve records for experiments
     * @param accessions of experiments
     * @return collection of records
     */
    public Collection<GeoRecord> getGeoRecords( Collection<String> accessions ) throws IOException {
        List<GeoRecord> records = new ArrayList<>();
        //https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=GSE[ETYP]+AND+(GSE100[accn]+OR+GSE101[accn])&retmax=5000&usehistory=y

        for ( List<String> chunk : ListUtils.partition( new ArrayList<>( accessions ), 10 ) ) {
            String searchUrlString = GeoBrowser.ESEARCH + "(" + chunk.stream().map( this::urlEncode ).collect( Collectors.joining( "[accn]+OR+" ) ) + "[accn])&usehistory=y";
            if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
                searchUrlString = searchUrlString + "&api_key=" + urlEncode( NCBI_API_KEY );
            }
            getGeoBasicRecords( records, searchUrlString );
        }

        return records;
    }

    private void getGeoBasicRecords( List<GeoRecord> records, String searchUrlString ) throws IOException {
        URL searchUrl = new URL( searchUrlString );
        Document searchDocument = parseMiniMLDocument( searchUrl );

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        if ( count == 0 )
            throw new RuntimeException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text&query_key=" + urlEncode( queryId ) + "&WebEnv=" + urlEncode( cookie )
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + urlEncode( NCBI_API_KEY ) : "" ) );

        StopWatch t = new StopWatch();
        t.start();

        NodeList accNodes, titleNodes, sampleNodes, dateNodes, orgnNodes, platformNodes, summaryNodes, typeNodes, pubmedNodes;
        Document summaryDocument = parseMiniMLDocument( fetchUrl );
        try {
            accNodes = ( NodeList ) xaccession.evaluate( summaryDocument, XPathConstants.NODESET );
            titleNodes = ( NodeList ) xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            sampleNodes = ( NodeList ) xnumSamples.evaluate( summaryDocument, XPathConstants.NODESET );
            dateNodes = ( NodeList ) xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
            orgnNodes = ( NodeList ) xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            platformNodes = ( NodeList ) xgpl.evaluate( summaryDocument, XPathConstants.NODESET );
            summaryNodes = ( NodeList ) xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            typeNodes = ( NodeList ) xtype.evaluate( summaryDocument, XPathConstants.NODESET );
            pubmedNodes = ( NodeList ) xpubmed.evaluate( summaryDocument, XPathConstants.NODESET );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( String.format( "Failed to parse XML for %s", fetchUrl ), e );
        }

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

            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse date for %s", record.getGeoAccession() ), e );
            }

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
     */
    public Collection<GeoRecord> getAllGEOPlatforms() throws IOException {

        String searchUrlString;

        searchUrlString = GeoBrowser.EPLATRETRIEVE + "&retmax=" + 10000 + "&usehistory=y"; //10k is the limit.

        if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
            searchUrlString = searchUrlString + "&api_key=" + urlEncode( NCBI_API_KEY );
        }

        URL searchUrl = new URL( searchUrlString );
        Document searchDocument = parseMiniMLDocument( searchUrl );

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        if ( count == 0 )
            throw new RuntimeException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text" + "&query_key=" + urlEncode( queryId ) + "&retmax="
                        + 10000 + "&WebEnv=" + urlEncode( cookie )
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + urlEncode( NCBI_API_KEY ) : "" ) );

        StopWatch t = new StopWatch();
        t.start();

        Document summaryDocument = parseMiniMLDocument( fetchUrl );
        NodeList accNodes, titleNodes, dateNodes, orgnNodes, summaryNodes, techNodes;
        try {
            accNodes = ( NodeList ) xPlataccession.evaluate( summaryDocument, XPathConstants.NODESET );
            titleNodes = ( NodeList ) xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            summaryNodes = ( NodeList ) xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            techNodes = ( NodeList ) xPlatformTech.evaluate( summaryDocument, XPathConstants.NODESET );
            orgnNodes = ( NodeList ) xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            dateNodes = ( NodeList ) xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
        } catch ( XPathExpressionException e ) {
            log.error( "Could not parse data: " + searchUrl, e );
            return Collections.emptyList();
        }

        // consider n_samples (number of elements) and the number of GSEs, but not every record has them, so it would be trickier.

        log.debug( "Got " + accNodes.getLength() + " XML records" );

        List<GeoRecord> records = new ArrayList<>( accNodes.getLength() );
        for ( int i = 0; i < accNodes.getLength(); i++ ) {
            GeoRecord record = new GeoRecord();
            record.setGeoAccession( "GPL" + accNodes.item( i ).getTextContent() );
            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse release date for %s", record.getGeoAccession() ), e );
            }
            record.setTitle( titleNodes.item( i ).getTextContent() );
            record.setOrganisms( null );
            record.setSummary( summaryNodes.item( i ).getTextContent() );
            record.setSeriesType( techNodes.item( i ).getTextContent() ); // slight abuse
            Collection<String> taxa = this.getTaxonCollection( orgnNodes.item( i ).getTextContent() );
            record.setOrganisms( taxa );
            records.add( record );
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
            platformLimitClause = " AND (" + limitPlatforms.stream().map( s -> s + "[ACCN]" ).collect( Collectors.joining( " OR " ) );
        }

        String searchUrlString;
        if ( StringUtils.isBlank( searchTerms ) ) {
            searchUrlString = GeoBrowser.ERETRIEVE + urlEncode( platformLimitClause ) + "&retstart=" + start + "&retmax=" + pageSize + "&usehistory=y";
        } else {
            // FIXME: could allow merging in of platformLimitClause. Should really rewrite the way we form these urls to be more modular.
            searchUrlString = GeoBrowser.ESEARCH + urlEncode( searchTerms ) + "&retstart=" + start + "&retmax=" + pageSize
                    + "&usehistory=y";
        }

        if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
            searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
        }

        URL searchUrl = new URL( searchUrlString );
        Document searchDocument = parseMiniMLDocument( searchUrl );

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        if ( count == 0 )
            throw new RuntimeException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL(
                GeoBrowser.EFETCH + "&mode=mode.text" + "&query_key=" + urlEncode( queryId ) + "&retstart=" + start + "&retmax="
                        + pageSize + "&WebEnv=" + urlEncode( cookie )
                        + ( StringUtils.isNotBlank( NCBI_API_KEY ) ? "&api_key=" + urlEncode( NCBI_API_KEY ) : "" ) );

        StopWatch t = new StopWatch();
        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH ); // for logging

        t.start();
        int rawRecords = 0;

        Document summaryDocument = parseMiniMLDocument( fetchUrl );
        NodeList accNodes, titleNodes, sampleNodes, dateNodes, orgnNodes, platformNodes, summaryNodes, typeNodes, pubmedNodes;
        try {
            accNodes = ( NodeList ) xaccession.evaluate( summaryDocument, XPathConstants.NODESET );
            titleNodes = ( NodeList ) xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
            sampleNodes = ( NodeList ) xnumSamples.evaluate( summaryDocument, XPathConstants.NODESET );
            dateNodes = ( NodeList ) xreleaseDate.evaluate( summaryDocument, XPathConstants.NODESET );
            orgnNodes = ( NodeList ) xorganisms.evaluate( summaryDocument, XPathConstants.NODESET );
            platformNodes = ( NodeList ) xgpl.evaluate( summaryDocument, XPathConstants.NODESET );
            summaryNodes = ( NodeList ) xsummary.evaluate( summaryDocument, XPathConstants.NODESET );
            typeNodes = ( NodeList ) xtype.evaluate( summaryDocument, XPathConstants.NODESET );
            pubmedNodes = ( NodeList ) xpubmed.evaluate( summaryDocument, XPathConstants.NODESET );
            // NodeList sampleLists = ( NodeList ) xsamples.evaluate( summaryDocument, XPathConstants.NODESET );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( String.format( "Failed to parse XML for %s", searchUrl ), e );
        }

        // Create GeoRecords using information parsed from XML file
        log.debug( String.format( "Got %d XML records in %d ms", accNodes.getLength(), t.getTime( TimeUnit.MILLISECONDS ) ) );

        for ( int i = 0; i < accNodes.getLength(); i++ ) {
            t.reset();
            t.start();

            GeoRecord record = new GeoRecord();

            rawRecords++; // prior to any filtering.

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
                        break;
                    }
                }
                if ( !useableTaxa ) {
                    continue;
                }
            }
            record.setOrganisms( taxa );

            record.setTitle( titleNodes.item( i ).getTextContent() );

            record.setNumSamples( Integer.parseInt( sampleNodes.item( i ).getTextContent() ) );

            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse date for %s", record.getGeoAccession() ) );
            }

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
                // without details this goes a lot quicker so feedback isn't very important
                log.debug( "Obtaining details for " + record.getGeoAccession() + " " + record.getNumSamples() + " samples..." );
                getDetails( record );
            }

            records.add( record );

            log.debug( "Processed: " + record.getGeoAccession() + " (" + dateFormat.format( record.getReleaseDate() ) + "), " + record.getNumSamples() + " samples, " + t.getTime() / 1000 + "s " );
        }


        if ( records.isEmpty() && rawRecords != 0 ) {
            /*
               When there are raw records, all that happened is we filtered them all out.
             */
            GeoBrowser.log.warn( "No records retained from query - all filtered out; number of raw records was " + rawRecords );
        } else if ( rawRecords == 0 ) {
            log.warn( "No records received at all" ); // could be the very beginning ...
            log.warn( "Query was " + searchUrl );
            log.warn( "Fetch was " + fetchUrl );
        } else {
            log.debug( "Parsed " + rawRecords + " records, " + records.size() + " retained at this stage" );
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
     */
    public List<GeoRecord> getRecentGeoRecords( int startPage, int pageSize ) throws IOException {

        if ( startPage < 0 || pageSize < 0 )
            throw new IllegalArgumentException( "Values must be greater than zero " );

        List<GeoRecord> records = new ArrayList<>();
        URL url = new URL( GEO_BROWSE_URL + startPage + GEO_BROWSE_SUFFIX + pageSize );

        try ( BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) ) ) {

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

                try {
                    Date date = DateUtils.parseDate( fields[columnNameToIndex.get( "Release Date" )]
                            .replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" ), GEO_LOCALE, GEO_DATE_FORMATS );
                    geoRecord.setReleaseDate( date );
                } catch ( ParseException e ) {
                    log.error( String.format( "Failed to parse date for %s", geoRecord.getGeoAccession() ) );
                }

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
     */
    void parseMINiML( GeoRecord record, Document detailsDocument ) throws IOException {
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE180363&targ=gse&form=xml&view=full
        NodeList relTypeNodes;
        String overallDesign;
        try {
            relTypeNodes = ( NodeList ) xRelationType.evaluate( detailsDocument, XPathConstants.NODESET );
            overallDesign = ( String ) xOverallDesign.evaluate( detailsDocument, XPathConstants.STRING );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( "Failed to parse MINiML", e );
        }

        for ( int i = 0; i < relTypeNodes.getLength(); i++ ) {

            String relType = relTypeNodes.item( i ).getAttributes().getNamedItem( "type" ).getTextContent();
            String relTo = relTypeNodes.item( i ).getAttributes().getNamedItem( "target" ).getTextContent();

            if ( relType.startsWith( "SubSeries" ) ) {
                record.setSubSeries( true );
                record.setSubSeriesOf( relTo );
            }
        }

        if ( overallDesign != null )
            record.setOverallDesign( StringUtils.remove( overallDesign, '\n' ) );
    }

    /**
     * exposed for testing
     *
     */
    void parseSampleMiNIML( GeoRecord record, Document detailsDocument ) throws XPathExpressionException {
        // Source, Characteristics
        NodeList channelNodes = ( NodeList ) xChannel.evaluate( detailsDocument, XPathConstants.NODESET );
        Set<String> props = new HashSet<>(); // expect duplicate terms

        for ( int i = 0; i < channelNodes.getLength(); i++ ) {
            Node item = channelNodes.item( i );
            NodeList sources = ( NodeList ) source.evaluate( item, XPathConstants.NODESET );
            for ( int k = 0; k < sources.getLength(); k++ ) {
                String s = sources.item( k ).getTextContent();
                String v = StringUtils.strip( s );
                try {
                    Double.parseDouble( v );
                    // skip unadorned numbers
                } catch ( NumberFormatException e ) {
                    props.add( v );
                }
            }
            NodeList chars = ( NodeList ) characteristics.evaluate( item, XPathConstants.NODESET );
            for ( int k = 0; k < chars.getLength(); k++ ) {
                String s = chars.item( k ).getTextContent();
                String v = StringUtils.strip( s );
                try {
                    Double.parseDouble( v );
                } catch ( NumberFormatException e ) {
                    props.add( v );
                }
            }
        }

        NodeList ls = ( NodeList ) xLibraryStrategy.evaluate( detailsDocument, XPathConstants.NODESET );
        Set<String> libraryStrategies = new HashSet<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            libraryStrategies.add( ls.item( i ).getTextContent() );
        }

        if ( !libraryStrategies.isEmpty() ) {
            record.setLibraryStrategy( StringUtils.join( libraryStrategies, ";" ) );
        }
        record.setSampleDetails( StringUtils.join( props, ";" ) );
    }

    /**
     * Do another query to NCBI to fetch additional information not present in the eutils. Specifically: whether this is
     * a subseries, and MeSH headings associated with any publication.
     * <p>
     * This method is resilient to various errors, ensuring that the GEO record can still be returned even if all the
     * details might not be filled.
     */
    private void getDetails( GeoRecord record ) {
        if ( !record.isSuperSeries() ) {
            URL miniMLURL;
            try {
                miniMLURL = new URL( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?targ=gse&form=xml&view=full&acc=" + urlEncode( record.getGeoAccession() ) );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException( e );
            }

            /*
             * I can't find a better way to get subseries info: the eutils doesn't provide this
             * information other than mentioning (in the summary text) when a series is a superseries. Determining
             * subseries
             * status is impossible without another query. I don't think batch queries for MiniML is possible.
             */
            try {
                parseMINiML( record, parseMiniMLDocument( miniMLURL ) );
            } catch ( IOException e ) {
                log.error( e.getMessage() + " while processing MINiML for " + record.getGeoAccession()
                        + ", subseries status will not be determined." );
            }
        }

        try {
            getSampleDetails( record );
        } catch ( EmptyMinimlDocumentException | IOException e ) {
            log.error( e.getMessage() + " while processing MINiML for " + record.getGeoAccession()
                    + ", sample details will not be obtained" );
        }

        // another query. Note that new Pubmed IDs generally don't have mesh headings yet, so this might not be that useful.
        if ( StringUtils.isNotBlank( record.getPubMedIds() ) ) {
            try {
                getMeshHeadings( record );
            } catch ( IOException e ) {
                log.error( "Could not get MeSH headings for " + record.getGeoAccession() + ": " + e.getMessage() );
            }
        }

        // get sample information
        // https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSM5230452&targ=self&form=xml&view=full
    }

    /**
     * @param  record      to process
     */
    private void getMeshHeadings( GeoRecord record ) throws IOException {
        Collection<String> meshheadings = new ArrayList<>();
        String[] idlist = record.getPubMedIds().split( "[,\\s]+" );
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
    }

    /**
     * Fetch and parse MINiML for samples.
     *
     */
    private void getSampleDetails( GeoRecord record ) throws IOException {
        // Fetch miniML for the samples.
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE171682&targ=gsm&form=xml&view=full
        URL sampleMINIMLURL = new URL( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?targ=gsm&form=xml&view=full&acc=" + urlEncode( record.getGeoAccession() ) );
        log.debug( String.format( "Obtaining sample details for %s from %s...", record.getGeoAccession(), sampleMINIMLURL ) );
        try {
            parseSampleMiNIML( record, parseMiniMLDocument( sampleMINIMLURL ) );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( String.format( "Failed to parse MINiML from URL %s", sampleMINIMLURL ), e );
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

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     *
     * @param url an URL from which the document should be parsed
     * @return a parsed MINiML document
     * @throws IOException if there is a problem while manipulating the file or if the number of records in the document
     * exceeds {@link #MAX_MINIML_RECORD_SIZE}
     */
    Document parseMiniMLDocument( URL url ) throws IOException {
        return parseMiniMLDocument( url, MAX_RETRIES, null );
    }

    private Document parseMiniMLDocument( URL url, int maxRetries, @Nullable IOExceptionWithRetry errorFromPreviousAttempt ) throws IOException {
        try ( InputStream is = openUrlWithMaxSize( url, MAX_MINIML_RECORD_SIZE ) ) {
            return GeoBrowser.docFactory.newDocumentBuilder().parse( is );
        } catch ( ParserConfigurationException | SAXException e ) {
            if ( isCausedByAnEmptyMinimlDocument( e ) ) {
                throw new EmptyMinimlDocumentException( e );
            } else if ( isLikelyCausedByAPrivateGeoRecord( e ) ) {
                throw new LikelyNonPublicGeoRecordException( e );
            } else {
                throw new RuntimeException( String.format( "Failed to parse MINiML from URL %s", url ), e );
            }
        } catch ( IOException ioe ) {
            if ( maxRetries > 0 && isEligibleForRetry( ioe ) ) {
                // exponential backoff?
                log.warn( String.format( "Failed to retrieve MINiML from URL %s, there are %d attempts left.", url, maxRetries - 1 ), ioe );
                try {
                    Thread.sleep( 1000 );
                } catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    throw ioe; // give up and just raise the exception
                }
                return parseMiniMLDocument( url, maxRetries - 1, new IOExceptionWithRetry( ioe, errorFromPreviousAttempt ) );
            } else if ( errorFromPreviousAttempt != null ) {
                throw new IOExceptionWithRetry( ioe, errorFromPreviousAttempt );
            } else {
                throw ioe;
            }
        }
    }

    private boolean isEligibleForRetry( IOException e ) {
        return !ExceptionUtils.hasCause( e, MinimlDocumentTooLargeException.class );
    }

    private boolean isCausedByAnEmptyMinimlDocument( Exception e ) {
        return e instanceof SAXParseException && e.getMessage().contains( "Premature end of file." );
    }

    /**
     * GEO delivers an HTML document for non-public datasets
     * it's possible for this specific case because we're not querying a dataset in particular
     */
    private boolean isLikelyCausedByAPrivateGeoRecord( Exception e ) {
        return e instanceof SAXParseException && e.getMessage().contains( "White spaces are required between publicId and systemId" );
    }

    /**
     * Open an URL with a maximum size, raising an {@link IOException} if exceeded.
     * <p>
     * This is trick we use to read from GEO MINiML because the HTTP payload does not advertise its size via a
     * {@code Content-Length} header.
     */
    private InputStream openUrlWithMaxSize( URL url, long maxSize ) throws IOException {
        InputStream inputStream = url.openStream();
        return new LimitedInputStream( inputStream, maxSize ) {
            @Override
            protected void raiseError( long pSizeMax, long pCount ) throws IOException {
                throw new MinimlDocumentTooLargeException( String.format( "Document exceeds %d B.", maxSize ) );
            }
        };
    }
}