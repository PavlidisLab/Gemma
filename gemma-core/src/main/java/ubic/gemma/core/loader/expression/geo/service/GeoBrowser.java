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
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryCallable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static ubic.gemma.core.util.XMLUtils.*;

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
     * Retry policy for I/O exception.
     */
    private static final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( 3, 1001, 1.5, IOException.class, GeoBrowser.class.getName() );

    /**
     * Maximum size of a MINiML record in bytes.
     * @see #openUrlWithMaxSize(URL, long)
     */
    private static final long MAX_MINIML_RECORD_SIZE = 100_000_000;

    private static final String ESEARCH = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds";
    private static final String ESUMMARY = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gds";
    private static final String GEO_BROWSE = "https://www.ncbi.nlm.nih.gov/geo/browse/";
    private static final String GEO_FTP = "https://ftp.ncbi.nlm.nih.gov/geo/series/";
    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";
    private static final Log log = LogFactory.getLog( GeoBrowser.class.getName() );

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

    static {
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

    private final String ncbiApiKey;
    private final PubMedXMLFetcher pubmedFetcher;

    public GeoBrowser( String ncbiApiKey ) {
        this.ncbiApiKey = ncbiApiKey;
        this.pubmedFetcher = new PubMedXMLFetcher( ncbiApiKey );
    }

    /**
     * Retrieve a single GEO record.
     */
    @Nullable
    public GeoRecord getGeoRecord( String accession, boolean detailed ) throws IOException {
        List<GeoRecord> records = new ArrayList<>();
        String searchUrl = ESEARCH
                + "&term=" + urlEncode( "gse[ETYP] AND " + quoteTerm( accession ) + "[accn]" )
                + "&usehistory=y";
        getGeoBasicRecords( searchUrl, records );
        if ( detailed ) {
            records.forEach( this::fillDetails );
        }
        if ( records.size() > 1 ) {
            throw new IllegalStateException( "More than one record found for " + accession );
        }
        return records.iterator().next();
    }

    /**
     * Retrieve records for experiments
     * @param accessions of experiments
     * @return collection of records
     */
    public Collection<GeoRecord> getGeoRecords( Collection<String> accessions, boolean detailed ) throws IOException {
        List<GeoRecord> records = new ArrayList<>();
        //https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gds&term=GSE[ETYP]+AND+(GSE100[accn]+OR+GSE101[accn])&retmax=5000&usehistory=y

        for ( List<String> chunk : ListUtils.partition( new ArrayList<>( accessions ), 10 ) ) {
            String searchUrl = ESEARCH
                    + "&term=" + urlEncode( "gse[ETYP] AND (" + chunk.stream().map( c -> quoteTerm( c ) + "[accn]" ).collect( Collectors.joining( " OR " ) ) + ")" )
                    + "&usehistory=y";
            if ( StringUtils.isNotBlank( ncbiApiKey ) ) {
                searchUrl = searchUrl + "&api_key=" + urlEncode( ncbiApiKey );
            }
            getGeoBasicRecords( searchUrl, records );
        }

        if ( detailed ) {
            records.forEach( this::fillDetails );
        }

        return records;
    }

    private void getGeoBasicRecords( String searchUrl, List<GeoRecord> records ) throws IOException {
        Document searchDocument = parseMiniMLDocument( new URL( searchUrl ) );

        int count = Integer.parseInt( getTextValue( getItem( searchDocument.getElementsByTagName( "Count" ), 0 ) ) );
        if ( count == 0 ) {
            log.warn( "Got no records from: " + searchUrl );
            return;
        }

        String queryId = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "QueryKey" ) ) );
        String cookie = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "WebEnv" ) ) );

        String fetchUrl = GeoBrowser.ESUMMARY
                + "&mode=mode.text"
                + "&query_key=" + urlEncode( queryId )
                + "&WebEnv=" + urlEncode( cookie )
                + ( StringUtils.isNotBlank( ncbiApiKey ) ? "&api_key=" + urlEncode( ncbiApiKey ) : "" );

        StopWatch t = new StopWatch();
        t.start();

        Document summaryDocument = parseMiniMLDocument( new URL( fetchUrl ) );

        NodeList accNodes = evaluate( xaccession, summaryDocument );
        NodeList titleNodes = evaluate( xtitle, summaryDocument );
        NodeList sampleNodes = evaluate( xnumSamples, summaryDocument );
        NodeList dateNodes = evaluate( xreleaseDate, summaryDocument );
        NodeList orgnNodes = evaluate( xorganisms, summaryDocument );
        NodeList platformNodes = evaluate( xgpl, summaryDocument );
        NodeList summaryNodes = evaluate( xsummary, summaryDocument );
        NodeList typeNodes = evaluate( xtype, summaryDocument );
        NodeList pubmedNodes = evaluate( xpubmed, summaryDocument );

        // Create GeoRecords using information parsed from XML file
        log.debug( "Got " + accNodes.getLength() + " XML records" );

        for ( int i = 0; i < accNodes.getLength(); i++ ) {

            GeoRecord record = new GeoRecord();
            record.setGeoAccession( "GSE" + getTextValue( getItem( accNodes, i ) ) );

            record.setSeriesType( getTextValue( getItem( typeNodes, i ) ) );
            if ( !record.getSeriesType().contains( "Expression profiling" ) ) {
                continue;
            }

            fillOrganisms( record, getItem( orgnNodes, i ) );

            record.setTitle( getTextValue( getItem( titleNodes, i ) ) );

            record.setNumSamples( Integer.parseInt( getTextValue( getItem( sampleNodes, i ) ) ) );

            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", getTextValue( getItem( dateNodes, i ) ) );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse date for %s", record.getGeoAccession() ), e );
            }

            fillPlatforms( record, getItem( platformNodes, i ) );

            record.setSummary( getTextValue( getItem( summaryNodes, i ) ) );
            fillPubMedIds( record, getItem( pubmedNodes, i ) );
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
     * @param allowedTaxa a collection of allowed taxa, ignored if null or empty
     * @return all relevant platforms up to single-query limit of NCBI
     */
    public Collection<GeoRecord> getAllGEOPlatforms( @Nullable Collection<String> allowedTaxa ) throws IOException {
        String term = "gpl[ETYPE]";
        if ( allowedTaxa != null && !allowedTaxa.isEmpty() ) {
            term += " AND (" + allowedTaxa.stream().map( t -> quoteTerm( t ) + "[ORGN]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        String searchUrl = ESEARCH
                + "&term=" + urlEncode( term )
                + "&retmax=" + 10000
                + "&usehistory=y"; //10k is the limit.

        if ( StringUtils.isNotBlank( ncbiApiKey ) ) {
            searchUrl += "&api_key=" + urlEncode( ncbiApiKey );
        }

        Document searchDocument = parseMiniMLDocument( new URL( searchUrl ) );

        NodeList countNode = searchDocument.getElementsByTagName( "Count" );

        int count = Integer.parseInt( getTextValue( getUniqueItem( countNode ) ) );
        if ( count == 0 )
            throw new RuntimeException( "Got no records from: " + searchUrl );

        String queryId = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "QueryKey" ) ) );
        String cookie = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "WebEnv" ) ) );

        String fetchUrl = GeoBrowser.ESUMMARY
                + "&mode=mode.text"
                + "&query_key=" + urlEncode( queryId )
                + "&retmax=" + 10000
                + "&WebEnv=" + urlEncode( cookie )
                + ( StringUtils.isNotBlank( ncbiApiKey ) ? "&api_key=" + urlEncode( ncbiApiKey ) : "" );

        StopWatch t = new StopWatch();
        t.start();

        Document summaryDocument = parseMiniMLDocument( new URL( fetchUrl ) );
        NodeList accNodes = evaluate( xPlataccession, summaryDocument );
        NodeList titleNodes = evaluate( xtitle, summaryDocument );
        NodeList summaryNodes = evaluate( xsummary, summaryDocument );
        NodeList techNodes = evaluate( xPlatformTech, summaryDocument );
        NodeList orgnNodes = evaluate( xorganisms, summaryDocument );
        NodeList dateNodes = evaluate( xreleaseDate, summaryDocument );

        // consider n_samples (number of elements) and the number of GSEs, but not every record has them, so it would be trickier.

        log.debug( "Got " + accNodes.getLength() + " XML records" );

        List<GeoRecord> records = new ArrayList<>( accNodes.getLength() );
        for ( int i = 0; i < accNodes.getLength(); i++ ) {
            GeoRecord record = new GeoRecord();
            record.setGeoAccession( "GPL" + getTextValue( getItem( accNodes, i ) ) );
            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", getTextValue( getItem( dateNodes, i ) ) );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse release date for %s", record.getGeoAccession() ), e );
            }
            record.setTitle( getTextValue( getItem( titleNodes, i ) ) );
            record.setOrganisms( null );
            record.setSummary( getTextValue( getItem( summaryNodes, i ) ) );
            record.setSeriesType( getTextValue( getItem( techNodes, i ) ) ); // slight abuse
            fillOrganisms( record, getItem( orgnNodes, i ) );
            records.add( record );
        }

        return records;
    }

    /**
     * Provides more details than getRecentGeoRecords. Performs an E-utilities query of the GEO database with the given
     * searchTerms (search terms can be omitted). Returns at most pageSize records. Does some screening of results for
     * expression studies, and (optionally) taxa. This is used for identifying data sets for loading.
     * <p>
     * Note that the search is reversed in time. You get the most recent records first.
     *
     * @param searchTerms    search terms in NCBI Entrez query format, ignored if null or blank
     * @param field          a field to search in or null to search everywhere
     * @param allowedTaxa    restrict search to the given taxa if not null
     * @param limitPlatforms restrict search to the given platforms if not null
     * @param seriesTypes    restrict search to the given series types (i.e. Expression profiling by array)
     * @param start          start an offset to retrieve batches
     * @param pageSize       number of results to retrieve in a batch
     * @param detailed       if true, additional information is fetched (slower)
     * @return a list of GeoRecords
     * @throws IOException if there is a problem obtaining or manipulating the file (some exceptions are not thrown and just logged)
     */
    public Slice<GeoRecord> searchGeoRecords( @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<String> seriesTypes, int start, int pageSize, boolean detailed ) throws IOException {
        String term = "gse[ETYP]";

        if ( StringUtils.isNotBlank( searchTerms ) ) {
            term += " AND " + quoteTerm( searchTerms );
            if ( field != null ) {
                term += "[" + field + "]";
            }
        }

        if ( limitPlatforms != null && !limitPlatforms.isEmpty() ) {
            term += " AND (" + limitPlatforms.stream().map( s -> quoteTerm( s ) + "[ACCN]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        if ( allowedTaxa != null && !allowedTaxa.isEmpty() ) {
            term += " AND (" + allowedTaxa.stream().map( s -> quoteTerm( s ) + "[ORGN]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        if ( seriesTypes != null ) {
            term += " AND (" + seriesTypes.stream().map( s -> quoteTerm( s ) + "[DataSet Type]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        String searchUrl = ESEARCH
                + "&term=" + urlEncode( term )
                + "&retstart=" + start
                + "&retmax=" + pageSize
                + "&usehistory=y";

        if ( StringUtils.isNotBlank( ncbiApiKey ) ) {
            searchUrl += "&api_key=" + ncbiApiKey;
        }

        log.debug( "Searching GEO with: " + searchUrl );
        Document searchDocument = parseMiniMLDocument( new URL( searchUrl ) );

        int count;
        String textValue = getTextValue( getItem( searchDocument.getElementsByTagName( "Count" ), 0 ) );
        if ( textValue == null ) {
            throw new IOException( "Got no valid record count element from: " + searchUrl );
        } else {
            try {
                count = Integer.parseInt( textValue );
            } catch ( NumberFormatException e ) {
                throw new IOException( "Got no valid record count from: " + searchUrl + "(" + e.getMessage() + ")" );
            }
        }

        if ( count == 0 ) {
            return new Slice<>( Collections.emptyList(), Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), 0, pageSize, ( long ) count );
        }

        int expectedRecords = Math.min( pageSize, count - start );

        String queryId = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "QueryKey" ) ) );
        String cookie = getTextValue( getUniqueItem( searchDocument.getElementsByTagName( "WebEnv" ) ) );

        String fetchUrl = GeoBrowser.ESUMMARY
                + "&mode=mode.text"
                + "&query_key=" + urlEncode( queryId )
                + "&retstart=" + start
                + "&retmax=" + pageSize
                + "&WebEnv=" + urlEncode( cookie )
                + ( StringUtils.isNotBlank( ncbiApiKey ) ? "&api_key=" + urlEncode( ncbiApiKey ) : "" );

        StopWatch t = new StopWatch();
        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH ); // for logging

        t.start();

        Document summaryDocument = parseMiniMLDocument( new URL( fetchUrl ) );

        NodeList accNodes = evaluate( xaccession, summaryDocument );
        NodeList titleNodes = evaluate( xtitle, summaryDocument );
        NodeList sampleNodes = evaluate( xnumSamples, summaryDocument );
        NodeList dateNodes = evaluate( xreleaseDate, summaryDocument );
        NodeList orgnNodes = evaluate( xorganisms, summaryDocument );
        NodeList platformNodes = evaluate( xgpl, summaryDocument );
        NodeList summaryNodes = evaluate( xsummary, summaryDocument );
        NodeList typeNodes = evaluate( xtype, summaryDocument );
        NodeList pubmedNodes = evaluate( xpubmed, summaryDocument );
        // NodeList sampleLists = ( NodeList ) xsamples.evaluate( summaryDocument, XPathConstants.NODESET );

        // Create GeoRecords using information parsed from XML file
        log.debug( String.format( "Got %d XML records in %d ms", accNodes.getLength(), t.getTime( TimeUnit.MILLISECONDS ) ) );

        if ( accNodes.getLength() != expectedRecords ) {
            throw new IOException( String.format( "Unexpected number of GEO records: %d, page should contain: %d.",
                    accNodes.getLength(), expectedRecords ) );
        }

        List<GeoRecord> records = new ArrayList<>();
        for ( int i = 0; i < expectedRecords; i++ ) {
            t.reset();
            t.start();

            GeoRecord record = new GeoRecord();
            record.setGeoAccession( "GSE" + getTextValue( getItem( accNodes, i ) ) );
            record.setSeriesType( getTextValue( getItem( typeNodes, i ) ) );
            fillOrganisms( record, getItem( orgnNodes, i ) );
            record.setTitle( getTextValue( getItem( titleNodes, i ) ) );
            record.setNumSamples( Integer.parseInt( getTextValue( getItem( sampleNodes, i ) ) ) );

            try {
                Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", getTextValue( getItem( dateNodes, i ) ) );
                record.setReleaseDate( date );
            } catch ( ParseException e ) {
                log.error( String.format( "Failed to parse date for %s", record.getGeoAccession() ) );
            }

            // there can be more than one, delimited by ';'
            fillPlatforms( record, getItem( platformNodes, i ) );

            record.setSummary( getTextValue( getItem( summaryNodes, i ) ) );
            fillPubMedIds( record, getItem( pubmedNodes, i ) );
            record.setSuperSeries( record.getTitle().contains( "SuperSeries" ) || record.getSummary().contains( "SuperSeries" ) );

            if ( detailed ) {
                // without details this goes a lot quicker so feedback isn't very important
                log.debug( "Obtaining details for " + record.getGeoAccession() + " " + record.getNumSamples() + " samples..." );
                fillDetails( record );
            }

            records.add( record );

            log.debug( "Processed: " + record.getGeoAccession() + " (" + dateFormat.format( record.getReleaseDate() ) + "), " + record.getNumSamples() + " samples, " + t.getTime() / 1000 + "s " );
        }

        if ( records.isEmpty() ) {
            // When there are raw records, all that happened is we filtered them all out.
            GeoBrowser.log.warn( "No records retained from query - all filtered out; number of raw records was " + accNodes.getLength() );
        }

        GeoBrowser.log.debug( "Parsed " + accNodes.getLength() + " records, " + records.size() + " retained at this stage" );

        return new Slice<>( records, Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), start, pageSize, ( long ) count );
    }

    private void fillPlatforms( GeoRecord record, Node item ) {
        String[] platformlist = StringUtils.split( getTextValue( item ), ";" );
        List<String> finalPlatformIds = new ArrayList<>();
        for ( String p : platformlist ) {
            finalPlatformIds.add( "GPL" + p.trim() );
        }
        record.setPlatform( StringUtils.join( finalPlatformIds, ";" ) );
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
    public Slice<GeoRecord> getRecentGeoRecords( int startPage, int pageSize ) throws IOException {
        if ( startPage < 0 || pageSize < 0 )
            throw new IllegalArgumentException( "Values must be greater than zero " );

        // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv
        // because of garbled tabbed lines returned
        // from GEO.
        URL url = new URL( GEO_BROWSE + "?view=series&zsort=date&mode=csv&page=" + startPage + "&display=" + pageSize );

        List<GeoRecord> records = new ArrayList<>();
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

                List<String> taxons = Arrays.stream( fields[columnNameToIndex.get( "Taxonomy" )]
                                .replaceAll( GeoBrowser.FLANKING_QUOTES_REGEX, "" )
                                .split( ";" ) )
                        .map( String::trim )
                        .collect( Collectors.toList() );
                if ( geoRecord.getOrganisms() == null ) {
                    geoRecord.setOrganisms( taxons );
                } else {
                    geoRecord.getOrganisms().addAll( taxons );
                }

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

        return new Slice<>( records, Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), startPage, pageSize, null );
    }

    /**
     * Quote a term if needed.
     * <p>
     * Refer to <a href="https://www.ncbi.nlm.nih.gov/books/NBK3837/">Entrez Help</a> for more details about the search
     * query syntax.
     */
    private String quoteTerm( String c ) {
        // strip quotes, I don't think it's possible to escape them
        c = c.replaceAll( "\"", "" );
        // : is used for range queries (i.e. 1:10[Sequence Length])
        // [] are used for fielded search
        // () are used for grouping boolean expressions
        // * is used for wildcard
        // spaces must be quoted, or else they will be treated as separate terms
        // '/' are used in date formatting
        if ( c.matches( "[:\\[\\]()*/\\s]" ) ) {
            return "\"" + c + "\"";
        }
        return c;
    }

    /**
     * exposed for testing
     */
    void fillSubSeriesStatus( GeoRecord record, Document detailsDocument ) {
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE180363&targ=gse&form=xml&view=full
        String result;
        try {
            result = ( String ) xOverallDesign.evaluate( detailsDocument, XPathConstants.STRING );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( e );
        }
        record.setOverallDesign( StringUtils.strip( result ) );
        NodeList relTypeNodes = evaluate( xRelationType, detailsDocument );
        for ( int i = 0; i < relTypeNodes.getLength(); i++ ) {
            String relType = getTextValue( ( getItem( relTypeNodes, i ) ).getAttributes().getNamedItem( "type" ) );
            String relTo = getTextValue( ( getItem( relTypeNodes, i ) ).getAttributes().getNamedItem( "target" ) );
            if ( relType != null && relType.startsWith( "SubSeries" ) ) {
                record.setSubSeries( true );
                record.setSubSeriesOf( relTo );
            }
        }
    }

    /**
     * exposed for testing
     */
    void fillSampleDetails( GeoRecord record, Document detailsDocument ) {
        // Source, Characteristics
        NodeList channelNodes = evaluate( xChannel, detailsDocument );
        Set<String> props = new HashSet<>(); // expect duplicate terms

        for ( int i = 0; i < channelNodes.getLength(); i++ ) {
            Node item = getItem( channelNodes, i );
            NodeList sources = evaluate( source, item );
            for ( int k = 0; k < sources.getLength(); k++ ) {
                String s = getTextValue( getItem( sources, k ) );
                try {
                    Double.parseDouble( s );
                    // skip unadorned numbers
                } catch ( NumberFormatException e ) {
                    props.add( s );
                }
            }
            NodeList chars = evaluate( characteristics, item );
            for ( int k = 0; k < chars.getLength(); k++ ) {
                String v = getTextValue( getItem( chars, k ) );
                try {
                    Double.parseDouble( v );
                } catch ( NumberFormatException e ) {
                    props.add( v );
                }
            }
        }

        NodeList ls = evaluate( xLibraryStrategy, detailsDocument );
        Set<String> libraryStrategies = new HashSet<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            libraryStrategies.add( getTextValue( getItem( ls, i ) ) );
        }

        if ( !libraryStrategies.isEmpty() ) {
            record.setLibraryStrategy( StringUtils.join( libraryStrategies, ";" ) );
        }
        record.setSampleDetails( StringUtils.join( props, ";" ) );
    }

    private NodeList evaluate( XPathExpression xpath, Node item ) {
        try {
            return ( NodeList ) xpath.evaluate( item, XPathConstants.NODESET );
        } catch ( XPathExpressionException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Do another query to NCBI to fetch additional information not present in the eutils. Specifically: whether this is
     * a subseries, and MeSH headings associated with any publication.
     * <p>
     * This method is resilient to various errors, ensuring that the GEO record can still be returned even if all the
     * details might not be filled.
     */
    private void fillDetails( GeoRecord record ) {
        Document document;
        try {
            document = parseMINiMLDocument( record.getGeoAccession() );
        } catch ( IOException e ) {
            log.error( "Error while processing MINiML for " + record.getGeoAccession() + ", sample details will not be obtained", e );
            return;
        }

        if ( !record.isSuperSeries() ) {
            /*
             * I can't find a better way to get subseries info: the eutils doesn't provide this
             * information other than mentioning (in the summary text) when a series is a superseries. Determining
             * subseries
             * status is impossible without another query. I don't think batch queries for MiniML is possible.
             */
            fillSubSeriesStatus( record, document );
        }

        // Fetch miniML for the samples.
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE171682&targ=gsm&form=xml&view=full
        log.debug( String.format( "Obtaining sample details for %s...", record ) );
        fillSampleDetails( record, document );

        // another query. Note that new Pubmed IDs generally don't have mesh headings yet, so this might not be that useful.
        try {
            fillMeshHeadings( record );
        } catch ( IOException e ) {
            log.error( "Could not get MeSH headings for " + record.getGeoAccession() + ".", e );
        }

        // get sample information
        // https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSM5230452&targ=self&form=xml&view=full
    }

    private void fillOrganisms( GeoRecord record, Node item ) {
        String input = getTextValue( item );
        input = input.replace( "; ", ";" );
        String[] taxonArray = input.split( ";" );
        Collection<String> taxa = Arrays.stream( taxonArray ).map( String::trim ).collect( Collectors.toList() );
        record.setOrganisms( taxa );
    }

    private void fillPubMedIds( GeoRecord record, Node item ) {
        List<String> pubmedIds = Arrays.stream( getTextValue( item ).split( "\\n" ) )
                .map( StringUtils::stripToNull )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        record.setPubMedIds( pubmedIds );
    }

    private void fillMeshHeadings( GeoRecord record ) throws IOException {
        if ( record.getPubMedIds() == null || record.getPubMedIds().isEmpty() ) {
            return;
        }
        List<Integer> idints = new ArrayList<>();
        for ( String s : record.getPubMedIds() ) {
            try {
                idints.add( Integer.parseInt( s ) );
            } catch ( NumberFormatException e ) {
                log.warn( "Invalid PubMed ID '" + s + "' for " + record.getGeoAccession() );
            }
        }
        Collection<BibliographicReference> refs = pubmedFetcher.retrieveByHTTP( idints );
        Collection<String> meshheadings = new ArrayList<>();
        for ( BibliographicReference ref : refs ) {
            for ( MedicalSubjectHeading mesh : ref.getMeshTerms() ) {
                meshheadings.add( mesh.getTerm() );
            }
        }
        record.setMeshHeadings( meshheadings );
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Parse a MINiML document for given GEO series accession.
     */
    private Document parseMINiMLDocument( String geoAccession ) throws IOException {
        URL documentUrl = new URL( GEO_FTP + geoAccession.substring( 0, geoAccession.length() - 3 ) + "nnn/" + geoAccession + "/miniml/" + geoAccession + "_family.xml.tgz" );
        return execute( ( attempt, lastAttempt ) -> {
            try ( TarInputStream tis = new TarInputStream( new GZIPInputStream( documentUrl.openStream() ) ) ) {
                TarEntry entry;
                while ( ( entry = tis.getNextEntry() ) != null ) {
                    if ( entry.getName().equals( geoAccession + "_family.xml" ) ) {
                        log.debug( "Parsing MINiML for " + geoAccession + " from " + documentUrl + "!" + entry.getName() + "..." );
                        return createDocumentBuilder().parse( tis );
                    }
                }
            } catch ( ParserConfigurationException | SAXException e ) {
                if ( isCausedByAnEmptyXmlDocument( e ) ) {
                    throw new IOException( e );
                } else if ( isCausedByAnHtmlDocument( e ) ) {
                    throw new IOException( "Detected what was likely an HTML document.", e );
                } else {
                    throw new RuntimeException( e );
                }
            } catch ( FileNotFoundException e ) {
                throw new NonRetryableIOException( e );
            }
            throw new NonRetryableIOException( new FileNotFoundException( String.format( "No entry with name %s_family.xml in %s.", geoAccession, documentUrl ) ) );
        }, "retrieve " + documentUrl );
    }

    /**
     *
     * @param url an URL from which the document should be parsed
     * @return a parsed MINiML document
     * @throws IOException if there is a problem while manipulating the file or if the number of records in the document
     * exceeds {@link #MAX_MINIML_RECORD_SIZE}
     */
    Document parseMiniMLDocument( URL url ) throws IOException {
        return execute( ( retries, lastAttempt ) -> {
            try ( InputStream is = openUrlWithMaxSize( url, MAX_MINIML_RECORD_SIZE ) ) {
                return createDocumentBuilder().parse( is );
            } catch ( ParserConfigurationException | SAXException e ) {
                if ( isCausedByAnEmptyXmlDocument( e ) ) {
                    throw new IOException( "Got an empty document for " + url + ".", e );
                } else if ( isCausedByAnHtmlDocument( e ) ) {
                    throw new IOException( "Detected what was likely an HTML document.", e );
                } else {
                    throw new RuntimeException( e );
                }
            } catch ( IOException ioe ) {
                if ( isEligibleForRetry( ioe ) ) {
                    throw ioe;
                } else {
                    throw new NonRetryableIOException( ioe );
                }
            }
        }, "parse MINiML from URL " + url );
    }

    /**
     * Check if the given exception is eligible for being retried.
     * <p>
     * For now, just exclude inputs that are too large from being reattempted.
     */
    private boolean isEligibleForRetry( IOException e ) {
        return !ExceptionUtils.hasCause( e, InputTooLargeException.class );
    }

    /**
     * Check if an exception is caused by an empty MINiML document.
     */
    private boolean isCausedByAnEmptyXmlDocument( Exception e ) {
        return e instanceof SAXParseException && e.getMessage().contains( "Premature end of file." );
    }

    /**
     * Check if an exception is caused by an HTML document being served instead of an XML document.
     */
    private boolean isCausedByAnHtmlDocument( Exception e ) {
        return e instanceof SAXParseException && e.getMessage().contains( "White spaces are required between publicId and systemId." );
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
                throw new InputTooLargeException( String.format( "Document exceeds %d B.", maxSize ) );
            }
        };
    }

    private static class InputTooLargeException extends IOException {
        public InputTooLargeException( String message ) {
            super( message );
        }
    }

    private <T> T execute( SimpleRetryCallable<T, IOException> callable, Object what ) throws IOException {
        try {
            return retryTemplate.execute( callable, what );
        } catch ( NonRetryableIOException w ) {
            throw w.getCause();
        }
    }

    /**
     * Allows for raising an {@link IOException} without having it retried.
     */
    private static class NonRetryableIOException extends RuntimeException {

        private final IOException cause;

        private NonRetryableIOException( IOException cause ) {
            this.cause = cause;
        }

        @Override
        public IOException getCause() {
            return cause;
        }
    }
}