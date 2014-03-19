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
import java.util.List;
import java.util.Map;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.loader.entrez.pubmed.XMLUtils;
import ubic.gemma.loader.expression.geo.model.GeoRecord;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoBrowser {

    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";

    private static Log log = LogFactory.getLog( GeoBrowser.class.getName() );

    // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv
    // because of garbled tabbed lines returned
    // from GEO.
    private String GEO_BROWSE_URL = "http://www.ncbi.nlm.nih.gov/geo/browse/?view=series&zsort=date&mode=csv&page=";
    private String GEO_BROWSE_SUFFIX = "&display=";

    private String[] DATE_FORMATS = new String[] { "MMM dd, yyyy" };

    // Used by getGeoRecordsBySearchTerm (will look for GSE entries only)
    private static String ESEARCH = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=gse[ETYP]+AND+";
    private static String EFETCH = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gds&";

    static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

    private XPathExpression xaccession;
    private XPathExpression xtitle;
    private XPathExpression xnumSamples;
    private XPathExpression xreleaseDate;
    private XPathExpression xorganisms;

    /**
     * Performs an E-utilities query of the GEO database with the given searchTerms. Returns at most pageSize records
     * (if found) starting at record #start.
     * 
     * @param start
     * @param pageSize
     * @param searchTerms
     * @return list of GeoRecords
     * @throws IOException
     * @throws RunTimeException
     */
    public List<GeoRecord> getGeoRecordsBySearchTerm( String searchTerms, int start, int pageSize ) throws IOException,
            RuntimeException {

        List<GeoRecord> records = new ArrayList<GeoRecord>();
        URL searchUrl = new URL( ESEARCH + searchTerms + "&retstart=" + start + "&retmax=" + pageSize + "&usehistory=y" );
        Document searchDocument;
        URLConnection conn = searchUrl.openConnection();
        conn.connect();
        try (InputStream is = conn.getInputStream();) {

            docFactory.setIgnoringComments( true );
            docFactory.setValidating( false );

            DocumentBuilder builder = docFactory.newDocumentBuilder();
            searchDocument = builder.parse( is );
        } catch ( ParserConfigurationException | SAXException e ) {
            throw new RuntimeException( e );
        }

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );
        Node countEl = countNode.item( 0 );

        int count = 0;
        try {
            count = Integer.parseInt( XMLUtils.getTextValue( ( Element ) countEl ) );
        } catch ( NumberFormatException e ) {
            throw new IOException( "Could not parse count from: " + searchUrl );
        }

        if ( count == 0 ) throw new IOException( "Got no records from: " + searchUrl );

        NodeList qnode = searchDocument.getElementsByTagName( "QueryKey" );

        Element queryIdEl = ( Element ) qnode.item( 0 );

        NodeList cknode = searchDocument.getElementsByTagName( "WebEnv" );
        Element cookieEl = ( Element ) cknode.item( 0 );

        String queryId = XMLUtils.getTextValue( queryIdEl );
        String cookie = XMLUtils.getTextValue( cookieEl );

        URL fetchUrl = new URL( EFETCH + "&mode=mode.text" + "&query_key=" + queryId + "&retstart=" + start
                + "&retmax=" + pageSize + "&WebEnv=" + cookie );

        conn = fetchUrl.openConnection();
        conn.connect();
        Document summaryDocument;
        try (InputStream is = conn.getInputStream();) {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            summaryDocument = builder.parse( is );

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();

            // Get relevant data from the XML file
            xaccession = xpath.compile( "//DocSum/Item[@Name='GSE']" );
            xtitle = xpath.compile( "//DocSum/Item[@Name='title']" );
            xnumSamples = xpath.compile( "//DocSum/Item[@Name='n_samples']" );
            xreleaseDate = xpath.compile( "//DocSum/Item[@Name='PDAT']" );
            xorganisms = xpath.compile( "//DocSum/Item[@Name='taxon']" );

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

            // Create GeoRecords using information parsed from XML file
            for ( int i = 0; i < accNodes.getLength(); i++ ) {

                GeoRecord record = new GeoRecord();

                record.setGeoAccession( "GSE" + accNodes.item( i ).getTextContent() );

                record.setTitle( titleNodes.item( i ).getTextContent() );

                record.setNumSamples( Integer.parseInt( sampleNodes.item( i ).getTextContent() ) );

                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", dateNodes.item( i ).getTextContent() );
                record.setReleaseDate( date );

                record.setOrganisms( getTaxonCollection( orgnNodes.item( i ).getTextContent() ) );

                records.add( record );
            }

            if ( records.isEmpty() ) {
                log.warn( "No records obtained" );
            }
        } catch ( ParserConfigurationException | ParseException | XPathExpressionException | SAXException e ) {
            throw new IOException( "Could not parse data: " + searchUrl, e );
        }
        return records;

    }

    /**
     * Retrieves and parses tab delimited file from GEO. File contains pageSize GEO records starting from startPage.
     * 
     * @param startPage
     * @param pageSize
     * @return list of GeoRecords
     * @throws IOException
     * @throws ParseException
     */
    public List<GeoRecord> getRecentGeoRecords( int startPage, int pageSize ) throws IOException, ParseException {

        if ( startPage < 0 || pageSize < 0 ) throw new IllegalArgumentException( "Values must be greater than zero " );

        List<GeoRecord> records = new ArrayList<GeoRecord>();
        URL url = null;
        try {
            url = new URL( GEO_BROWSE_URL + startPage + GEO_BROWSE_SUFFIX + pageSize );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( "Invalid URL " + url, e );
        }

        URLConnection conn = url.openConnection();
        conn.connect();
        try (InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );) {

            // We are getting a tab delimited file.

            // Read columns headers.
            String headerLine = br.readLine();
            String[] headers = StringUtil.csvSplit( headerLine );

            // Map column names to their indices (handy later).
            Map<String, Integer> columnNameToIndex = new HashMap<String, Integer>();
            for ( int i = 0; i < headers.length; i++ ) {
                columnNameToIndex.put( headers[i], i );
            }

            // Read the rest of the file.
            String line = null;
            while ( ( line = br.readLine() ) != null ) {
                String[] fields = StringUtil.csvSplit( line );

                GeoRecord geoRecord = new GeoRecord();
                geoRecord.setGeoAccession( fields[columnNameToIndex.get( "Accession" )] );
                geoRecord.setTitle( StringUtils.strip( fields[columnNameToIndex.get( "Title" )].replaceAll(
                        FLANKING_QUOTES_REGEX, "" ) ) );

                String sampleCountS = fields[columnNameToIndex.get( "Sample Count" )];
                if ( StringUtils.isNotBlank( sampleCountS ) ) {
                    try {
                        geoRecord.setNumSamples( Integer.parseInt( sampleCountS ) );
                    } catch ( NumberFormatException e ) {
                        throw new RuntimeException( "Could not parse sample count: " + sampleCountS );
                    }
                } else {
                    log.warn( "No sample count for " + geoRecord.getGeoAccession() );
                }
                geoRecord.setContactName( fields[columnNameToIndex.get( "Contact" )].replaceAll( FLANKING_QUOTES_REGEX,
                        "" ) );

                String[] taxons = fields[columnNameToIndex.get( "Taxonomy" )].replaceAll( FLANKING_QUOTES_REGEX, "" )
                        .split( ";" );
                geoRecord.getOrganisms().addAll( Arrays.asList( taxons ) );

                Date date = DateUtils.parseDate(
                        fields[columnNameToIndex.get( "Release Date" )].replaceAll( FLANKING_QUOTES_REGEX, "" ),
                        DATE_FORMATS );
                geoRecord.setReleaseDate( date );

                geoRecord.setSeriesType( fields[columnNameToIndex.get( "Series Type" )] );

                records.add( geoRecord );
            }

        }

        if ( records.isEmpty() ) {
            log.warn( "No records obtained" );
        }
        return records;

    }

    /* Extracts taxon names from input string; returns a collection of taxon names */
    private Collection<String> getTaxonCollection( String input ) {
        Collection<String> taxa = new ArrayList<String>();

        input = input.replace( "; ", ";" );
        String[] taxonArray = input.split( ";" );

        for ( int i = 0; i < Array.getLength( taxonArray ); i++ ) {
            taxa.add( taxonArray[i].trim() );
        }
        return taxa;
    }

}
