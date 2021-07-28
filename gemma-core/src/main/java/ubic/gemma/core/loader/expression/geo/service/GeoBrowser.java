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
import java.io.UTFDataFormatException;
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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.XMLUtils;
import ubic.gemma.persistence.util.Settings;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 *
 * @author pavlidis
 */
public class GeoBrowser {

    private static final String NCBI_API_KEY = Settings.getString( "entrez.efetch.apikey" );

    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";
    private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    private static final Log log = LogFactory.getLog( GeoBrowser.class.getName() );
    // Used by getGeoRecordsBySearchTerm (will look for GSE entries only)
    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gse[ETYP]+AND+";
    private static final String ERETRIEVE = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gse[ETYP]"; //no extra search term
    private static final String EFETCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gds&";
    // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv
    // because of garbled tabbed lines returned
    // from GEO.
    @SuppressWarnings("FieldCanBeLocal") // Constant is better
    private final String GEO_BROWSE_URL = "https://www.ncbi.nlm.nih.gov/geo/browse/?view=series&zsort=date&mode=csv&page=";
    @SuppressWarnings("FieldCanBeLocal") // Constant is better
    private final String GEO_BROWSE_SUFFIX = "&display=";
    private final String[] DATE_FORMATS = new String[] { "MMM dd, yyyy" };

    /**
     * Provides more details than getRecentGeoRecords. Performs an E-utilities query of the GEO database with the given
     * searchTerms (search terms can be ommitted). Returns at most pageSize records
     * (if found) starting at record #start.
     *
     * @param  start       start
     * @param  pageSize    page size
     * @param  searchTerms search terms
     * @param  detailed    if true, additional information is fetched (slower)
     * @return             list of GeoRecords
     * @throws IOException if there is a problem while manipulating the file
     */
    public List<GeoRecord> getGeoRecordsBySearchTerm( String searchTerms, int start, int pageSize, boolean detailed )
            throws IOException, RuntimeException {

        List<GeoRecord> records = new ArrayList<>();

        String searchUrlString;
        if ( StringUtils.isBlank( searchTerms ) ) {
            searchUrlString = GeoBrowser.ERETRIEVE + "&retstart=" + start + "&retmax=" + pageSize + "&usehistory=y";
        } else {
            searchUrlString = GeoBrowser.ESEARCH + searchTerms + "&retstart=" + start + "&retmax=" + pageSize + "&usehistory=y";
        }

        if ( StringUtils.isNotBlank( NCBI_API_KEY ) ) {
            searchUrlString = searchUrlString + "&api_key=" + NCBI_API_KEY;
        }

        URL searchUrl = new URL( searchUrlString );

        Document searchDocument;
        URLConnection conn = searchUrl.openConnection();
        conn.connect();
        try (InputStream is = conn.getInputStream()) {

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

        conn = fetchUrl.openConnection();
        conn.connect();

        XPathFactory xFactory = XPathFactory.newInstance();
        XPath xpath = xFactory.newXPath();

        try {
            // Get relevant data from the XML file
            XPathExpression xaccession = xpath.compile( "//DocSum/Item[@Name='GSE']" );
            XPathExpression xtitle = xpath.compile( "//DocSum/Item[@Name='title']" );
            XPathExpression xnumSamples = xpath.compile( "//DocSum/Item[@Name='n_samples']" );
            XPathExpression xreleaseDate = xpath.compile( "//DocSum/Item[@Name='PDAT']" );
            XPathExpression xorganisms = xpath.compile( "//DocSum/Item[@Name='taxon']" );
            XPathExpression xgpl = xpath.compile( "//DocSum/Item[@Name='GPL']" );
            XPathExpression xsummary = xpath.compile( "//DocSum/Item[@Name='summary']" );
            XPathExpression xtype = xpath.compile( "//DocSum/Item[@Name='gdsType']" );

            XPathExpression xpubmed = xpath.compile( "//DocSum/Item[@Name='PubMedIds']" ); // list; also in miniml

            Document summaryDocument;
            try (InputStream is = conn.getInputStream()) {
                DocumentBuilder builder = GeoBrowser.docFactory.newDocumentBuilder();
                summaryDocument = builder.parse( is );

                Object accessions = xaccession.evaluate( summaryDocument, XPathConstants.NODESET );
                NodeList accNodes = ( NodeList ) accessions;

                Object titles = xtitle.evaluate( summaryDocument, XPathConstants.NODESET );
                NodeList titleNodes = ( NodeList ) titles;

                Object samples = xnumSamples.evaluate( summaryDocument, XPathConstants.NODESET );
                NodeList sampleNodes = ( NodeList ) samples;

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

                    record.setTitle( titleNodes.item( i ).getTextContent() );

                    record.setNumSamples( Integer.parseInt( sampleNodes.item( i ).getTextContent() ) );

                    Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                    record.setReleaseDate( date );

                    record.setOrganisms( this.getTaxonCollection( orgnNodes.item( i ).getTextContent() ) );

                    // there can be more than one, delimited by ';'
                    String[] platformlist = StringUtils.split( platformNodes.item( i ).getTextContent(), ";" );
                    List<String> finalPlatformIds = new ArrayList<>();
                    for ( String p : platformlist ) {
                        finalPlatformIds.add( "GPL" + p );
                    }
                    String platformS = StringUtils.join( finalPlatformIds, ";" );

                    record.setPlatform( platformS );
                    record.setSeriesType( typeNodes.item( i ).getTextContent() );
                    record.setSummary( summaryNodes.item( i ).getTextContent() );
                    record.setPubMedIds( StringUtils.strip( pubmedNodes.item( i ).getTextContent() ) );
                    record.setSuperSeries( record.getTitle().contains( "SuperSeries" ) || record.getSummary().contains( "SuperSeries" ) );

                    // at the moment, getDetails only determines if it is a subseries, so we avoid checking if we can
                    if ( detailed && !record.isSuperSeries() ) {
                        getDetails( record );
                    }

                    records.add( record );
                }

            } catch ( ParserConfigurationException | ParseException | XPathExpressionException | SAXException e ) {
                throw new IOException( "Could not parse data: " + searchUrl, e );
            }

        } catch ( XPathException e ) {
            throw new RuntimeException( e );
        }

        if ( records.isEmpty() ) {
            GeoBrowser.log.warn( "No records obtained" );
        } else {
            log.debug( "Parsed " + records.size() + " records" );
        }

        return records;

    }

    /**
     * Do another query to NCBI to fetch additional information not present in the eutils. Specifically: whether this is
     * a subseries.
     * 
     * @param  record
     * @throws MalformedURLException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    private void getDetails( GeoRecord record )
            throws MalformedURLException, IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        URL detailsURL = new URL(
                "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?targ=gse&form=xml&view=full&acc=" + record.getGeoAccession() );

        /*
         * This is pretty painful because it is slow. I can't find a better way, though: the eutils doesn't provide this
         * information other than mentioning when a series is a superseries. Determining subseries status is impossible
         * without another query. I don't think batch queries for MiniML is possible.
         */

        URLConnection dconn = detailsURL.openConnection();
        dconn.connect();

        try (InputStream isd = dconn.getInputStream()) {
            parseMINiML( record, isd );
        }
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
    void parseMINiML( GeoRecord record, InputStream is )
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {

        XPathFactory xFactoryD = XPathFactory.newInstance();
        XPath xpathD = xFactoryD.newXPath();
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE180363&targ=gse&form=xml&view=full
        XPathExpression xRelationType = xpathD.compile( "//MINiML/Series/Relation" );

        try {
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
        } catch ( UTFDataFormatException e ) {
            log.error( e.getMessage() + " while processing MINiML for " + record.getGeoAccession()
                    + ", subseries status will not be determined." );
        }
    }

    /**
     * Retrieves and parses tab delimited file from GEO. File contains pageSize GEO records starting from startPage. The
     * retrieved information is pretty minimal.
     *
     * @param  startPage      start page
     * @param  pageSize       page size
     * @return                list of GeoRecords
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
        try (InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

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
     * Extracts taxon names from input string; returns a collection of taxon names
     *
     * @param  input input
     * @return       taxon names
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
