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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.entrez.EntrezRetmode;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.entrez.EntrezXmlUtils;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.model.GeoSeriesType;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryCallable;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.MedicalSubjectHeading;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.xml.xpath.XPathExpression;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.loader.expression.geo.service.GeoUtils.getUrlForSeriesFamily;
import static ubic.gemma.core.util.XMLUtils.*;

@CommonsLog
public class GeoBrowserImpl implements GeoBrowser {

    /**
     * Retry policy for I/O exception.
     */
    private static final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( new SimpleRetryPolicy( 3, 1001, 1.5 ), IOException.class, GeoBrowserImpl.class.getName() );

    /**
     * Maximum size of a MINiML record in bytes.
     * @see #openUrlWithMaxSize(URL, long)
     */
    private static final long MAX_MINIML_RECORD_SIZE = 100_000_000;

    private static final String GEO_BROWSE = "https://www.ncbi.nlm.nih.gov/geo/browse/";
    private static final String FLANKING_QUOTES_REGEX = "^\"|\"$";

    // Get relevant data from the XML file
    private static final XPathExpression xaccession = compile( "Item[@Name='GSE']" );
    private static final XPathExpression xgpl = compile( "Item[@Name='GPL']" );
    private static final XPathExpression xnumSamples = compile( "Item[@Name='n_samples']" );
    private static final XPathExpression xorganisms = compile( "Item[@Name='taxon']" );
    private static final XPathExpression xPlataccession = compile( "Item[@Name='GPL']" );
    private static final XPathExpression xPlatformTech = compile( "Item[@Name='ptechType']" );
    // list; also in miniml
    private static final XPathExpression xpubmed = compile( "Item[@Name='PubMedIds']" );
    private static final XPathExpression xreleaseDate = compile( "Item[@Name='PDAT']" );
    private static final XPathExpression xsummary = compile( "Item[@Name='summary']" );
    private static final XPathExpression xtitle = compile( "Item[@Name='title']" );
    private static final XPathExpression xtype = compile( "Item[@Name='gdsType']" );

    // for the detailed MINiML
    private static final XPathExpression xRelationType = compile( "//MINiML/Series/Relation" );
    private static final XPathExpression xOverallDesign = compile( "//MINiML/Series/Overall-Design" );
    private static final XPathExpression xSampleGeoAccession = compile( "//MINiML/Sample/Accession[@database='GEO']" );
    private static final XPathExpression xChannel = compile( "//MINiML/Sample/Channel" );
    private static final XPathExpression xSampleDescription = compile( "//MINiML/Sample/Description" );
    private static final XPathExpression xDataProcessing = compile( "//MINiML/Sample/Data-Processing" );
    private static final XPathExpression xLibraryStrategy = compile( "//MINiML/Sample/Library-Strategy" );
    private static final XPathExpression xLibrarySource = compile( "//MINiML/Sample/Library-Source" );

    /* locale */
    private static final Locale GEO_LOCALE = Locale.ENGLISH;
    private static final String[] GEO_DATE_FORMATS = new String[] { "MMM dd, yyyy" };

    private final String ncbiApiKey;
    private final PubMedSearch pubmedFetcher;

    public GeoBrowserImpl( String ncbiApiKey ) {
        this.ncbiApiKey = ncbiApiKey;
        this.pubmedFetcher = new PubMedSearch( ncbiApiKey );
    }

    @Nullable
    @Override
    public GeoRecord getGeoRecord( GeoRecordType recordType, String accession, GeoRetrieveConfig config ) throws IOException {
        GeoQuery query = searchGeoRecords( recordType, entryTypeFromRecordType( recordType ) + "[" + GeoSearchField.ENTRY_TYPE + "] AND " + quoteTerm( accession ) + "[" + GeoSearchField.GEO_ACCESSION + "]" );
        if ( query.getTotalRecords() > 1 ) {
            throw new IllegalStateException( "More than one record found for " + accession + "." );
        }
        Collection<GeoRecord> records = retrieveAllGeoRecords( query, config );
        if ( records.isEmpty() ) {
            return null;
        } else {
            return records.iterator().next();
        }
    }

    /**
     * Retrieve records for experiments
     * @param accessions of experiments
     * @return collection of records
     */
    @Override
    public Collection<GeoRecord> getGeoRecords( GeoRecordType recordType, Collection<String> accessions, GeoRetrieveConfig config ) throws IOException {
        List<GeoRecord> records = new ArrayList<>( accessions.size() );
        for ( List<String> chunk : ListUtils.partition( new ArrayList<>( accessions ), 10 ) ) {
            GeoQuery query = searchGeoRecords( recordType, entryTypeFromRecordType( recordType ) + "[" + GeoSearchField.ENTRY_TYPE + "] AND (" + chunk.stream().map( c -> quoteTerm( c ) + "[" + GeoSearchField.GEO_ACCESSION + "]" ).collect( Collectors.joining( " OR " ) ) + ")" );
            records.addAll( retrieveAllGeoRecords( query, config ) );
        }
        return records;
    }

    @Override
    public Collection<GeoRecord> getAllGeoRecords( GeoRecordType recordType, @Nullable Collection<String> allowedTaxa, int limit ) throws IOException {
        Assert.isTrue( recordType == GeoRecordType.PLATFORM, "Only platforms are supported" );

        String term = entryTypeFromRecordType( recordType ) + "[" + GeoSearchField.ENTRY_TYPE + "]";
        if ( allowedTaxa != null && !allowedTaxa.isEmpty() ) {
            term += " AND (" + allowedTaxa.stream().map( t -> quoteTerm( t ) + "[" + GeoSearchField.ORGANISM + "]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        GeoQuery query = searchGeoRecords( recordType, term );

        // cap to 10000
        if ( limit > 0 && query.getTotalRecords() > limit ) {
            log.warn( "More than " + limit + " records found: " + query.getTotalRecords() + ", only the first " + limit + " will be retrieved." );
            query = new GeoQuery( query.getRecordType(), query.getQueryId(), query.getCookie(), limit );
        }

        return retrieveAllGeoRecords( query, GeoRetrieveConfig.DEFAULT );
    }

    @Override
    public Slice<GeoRecord> getRecentGeoRecords( GeoRecordType recordType, int start, int pageSize ) throws IOException {
        Assert.isTrue( recordType == GeoRecordType.SERIES, "Only series are supported" );
        Assert.isTrue( start >= 0, "The starting must be zero or greater." );
        Assert.isTrue( pageSize > 0, "The page size must be one or greater." );

        // mode=tsv : tells GEO to give us tab delimited file -- PP changed to csv
        // because of garbled tabbed lines returned
        // from GEO.
        URL url = new URL( GEO_BROWSE + "?view=series&zsort=date&mode=csv&page=" + start + "&display=" + pageSize );

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
                        .replaceAll( GeoBrowserImpl.FLANKING_QUOTES_REGEX, "" ) ) );

                String sampleCountS = fields[columnNameToIndex.get( "Sample Count" )];
                if ( StringUtils.isNotBlank( sampleCountS ) ) {
                    try {
                        geoRecord.setNumSamples( Integer.parseInt( sampleCountS ) );
                    } catch ( NumberFormatException e ) {
                        throw new RuntimeException( "Could not parse sample count: " + sampleCountS );
                    }
                } else {
                    GeoBrowserImpl.log.warn( "No sample count for " + geoRecord.getGeoAccession() );
                }
                geoRecord.setContactName(
                        fields[columnNameToIndex.get( "Contact" )].replaceAll( GeoBrowserImpl.FLANKING_QUOTES_REGEX, "" ) );

                List<String> taxons = Arrays.stream( fields[columnNameToIndex.get( "Taxonomy" )]
                                .replaceAll( GeoBrowserImpl.FLANKING_QUOTES_REGEX, "" )
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
                            .replaceAll( GeoBrowserImpl.FLANKING_QUOTES_REGEX, "" ), GEO_LOCALE, GEO_DATE_FORMATS );
                    geoRecord.setReleaseDate( date );
                } catch ( ParseException e ) {
                    log.error( String.format( "Failed to parse date for %s", geoRecord.getGeoAccession() ) );
                }

                geoRecord.setSeriesType( fields[columnNameToIndex.get( "Series Type" )] );

                records.add( geoRecord );
            }

        }

        if ( records.isEmpty() ) {
            GeoBrowserImpl.log.warn( "No records obtained" );
        }

        return new Slice<>( records, Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), start, pageSize, null );
    }

    @Override
    public GeoQuery searchGeoRecords( GeoRecordType recordType, @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<GeoSeriesType> seriesTypes ) throws IOException {
        String term = entryTypeFromRecordType( recordType ) + "[" + GeoSearchField.ENTRY_TYPE + "]";

        if ( StringUtils.isNotBlank( searchTerms ) ) {
            term += " AND " + quoteTerm( searchTerms );
            if ( field != null ) {
                term += "[" + field + "]";
            }
        }

        if ( limitPlatforms != null && !limitPlatforms.isEmpty() ) {
            term += " AND (" + limitPlatforms.stream().map( s -> quoteTerm( s ) + "[" + GeoSearchField.GEO_ACCESSION + "]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        if ( allowedTaxa != null && !allowedTaxa.isEmpty() ) {
            term += " AND (" + allowedTaxa.stream().map( s -> quoteTerm( s ) + "[" + GeoSearchField.ORGANISM + "]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        if ( seriesTypes != null ) {
            term += " AND (" + seriesTypes.stream().map( s -> quoteTerm( s.getIdentifier() ) + "[" + GeoSearchField.DATASET_TYPE + "]" ).collect( Collectors.joining( " OR " ) ) + ")";
        }

        return searchGeoRecords( recordType, term );
    }

    @Override
    public GeoQuery searchGeoRecords( GeoRecordType recordType, String term ) throws IOException {
        URL searchUrl = EntrezUtils.search( "gds", term, EntrezRetmode.XML, ncbiApiKey );

        log.debug( "Searching GEO with: " + searchUrl );
        // FIXME: this is not a MINiML document
        Document searchDocument = parseMiniMLDocument( searchUrl );

        int count = EntrezXmlUtils.getCount( searchDocument );
        String queryId = EntrezXmlUtils.getQueryId( searchDocument );
        String cookie = EntrezXmlUtils.getCookie( searchDocument );

        return new GeoQuery( recordType, queryId, cookie, count );
    }

    @Override
    public Slice<GeoRecord> retrieveGeoRecords( GeoQuery query, int start, int pageSize, GeoRetrieveConfig config ) throws IOException {
        Assert.isTrue( start >= 0, "Start must be zero or greater." );
        Assert.isTrue( pageSize >= 1, "Page size must be one or greater." );
        int count = query.getTotalRecords();

        if ( count == 0 ) {
            return new Slice<>( Collections.emptyList(), Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), 0, pageSize, ( long ) count );
        }

        // if start > count, it should be empty
        int expectedRecords = Math.min( pageSize, Math.max( count - start, 0 ) );

        URL fetchUrl = EntrezUtils.summary( "gds", query, EntrezRetmode.XML, start, pageSize, ncbiApiKey );

        StopWatch t = new StopWatch();
        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH ); // for logging

        t.start();

        Document summaryDocument = parseMiniMLDocument( fetchUrl );

        List<GeoRecord> records = new ArrayList<>();
        for ( Node node = summaryDocument.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling() ) {
            if ( !node.getNodeName().equals( "DocSum" ) ) {
                continue;
            }
            t.reset();
            t.start();
            GeoRecord record = fillRecord( query.getRecordType(), node, config );
            records.add( record );
            log.debug( "Processed: " + record.getGeoAccession() + " (" + dateFormat.format( record.getReleaseDate() ) + "), " + record.getNumSamples() + " samples, " + t.getTime() / 1000 + "s " );
        }

        if ( records.size() != expectedRecords ) {
            throw new IOException( String.format( "Unexpected number of GEO records: %d, page should contain: %d.",
                    records.size(), expectedRecords ) );
        }

        if ( records.isEmpty() ) {
            // When there are raw records, all that happened is we filtered them all out.
            GeoBrowserImpl.log.warn( "No records retained from query - all filtered out; number of raw records was " + expectedRecords );
        }

        GeoBrowserImpl.log.debug( "Parsed " + expectedRecords + " records, " + records.size() + " retained at this stage" );

        return new Slice<>( records, Sort.by( null, "releaseDate", Sort.Direction.DESC, Sort.NullMode.DEFAULT ), start, pageSize, ( long ) count );
    }

    @Override
    public Collection<GeoRecord> retrieveAllGeoRecords( GeoQuery query, GeoRetrieveConfig config ) throws IOException {
        if ( query.getTotalRecords() == 0 ) {
            return Collections.emptyList();
        }

        URL fetchUrl = EntrezUtils.summary( "gds", query, EntrezRetmode.XML, 0, query.getTotalRecords(), ncbiApiKey );

        StopWatch t = new StopWatch();
        t.start();

        Document summaryDocument;
        try {
            summaryDocument = parseMiniMLDocument( fetchUrl );
        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to parse MINiML document at " + fetchUrl, e );
        }

        // consider n_samples (number of elements) and the number of GSEs, but not every record has them, so it would be trickier.
        List<GeoRecord> records = new ArrayList<>( query.getTotalRecords() );
        for ( Node node = summaryDocument.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling() ) {
            if ( !node.getNodeName().equals( "DocSum" ) ) {
                continue;
            }
            records.add( fillRecord( query.getRecordType(), node, config ) );
        }

        if ( records.size() != query.getTotalRecords() ) {
            throw new IOException( String.format( "Unexpected number of GEO records: %d, page should contain: %d.",
                    records.size(), query.getTotalRecords() ) );
        }

        return records;
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

    private GeoRecord fillRecord( GeoRecordType recordType, Node node, GeoRetrieveConfig config ) {
        switch ( recordType ) {
            case SERIES:
                return fillSeriesRecord( node, config );
            case PLATFORM:
                return fillPlatformRecord( node );
            default:
                throw new IllegalArgumentException( "Unsupported record type: " + recordType + "." );
        }
    }

    private GeoRecord fillPlatformRecord( Node node ) {
        GeoRecord record = new GeoRecord();
        record.setGeoAccession( "GPL" + evaluateToString( xPlataccession, node ) );
        try {
            Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", evaluateToString( xreleaseDate, node ) );
            record.setReleaseDate( date );
        } catch ( ParseException e ) {
            log.error( String.format( "Failed to parse release date for %s", record.getGeoAccession() ), e );
        }
        record.setTitle( evaluateToString( xtitle, node ) );
        record.setSummary( StringUtils.strip( evaluateToString( xsummary, node ) ) );
        record.setSeriesType( evaluateToString( xPlatformTech, node ) ); // slight abuse
        fillOrganisms( record, evaluateToString( xorganisms, node ) );
        return record;
    }

    private GeoRecord fillSeriesRecord( Node node, GeoRetrieveConfig config ) {
        GeoRecord record = new GeoRecord();
        record.setGeoAccession( "GSE" + evaluateToString( xaccession, node ) );
        record.setSeriesType( evaluateToString( xtype, node ) );
        fillOrganisms( record, evaluateToString( xorganisms, node ) );
        record.setTitle( evaluateToString( xtitle, node ) );
        record.setNumSamples( Integer.parseInt( requireNonNull( evaluateToString( xnumSamples, node ) ) ) );
        try {
            Date date = DateUtil.convertStringToDate( "yyyy/MM/dd", evaluateToString( xreleaseDate, node ) );
            record.setReleaseDate( date );
        } catch ( ParseException e ) {
            log.error( String.format( "Failed to parse date for %s", record.getGeoAccession() ), e );
        }
        fillPlatforms( record, evaluateToString( xgpl, node ) );
        record.setSummary( StringUtils.strip( evaluateToString( xsummary, node ) ) );
        fillPubMedIds( record, evaluateToString( xpubmed, node ) );
        record.setSuperSeries( record.getTitle().contains( "SuperSeries" ) || record.getSummary().contains( "SuperSeries" ) );
        fillDetails( record, config );
        return record;
    }

    /**
     * Do another query to NCBI to fetch additional information not present in the eutils. Specifically: whether this is
     * a subseries, and MeSH headings associated with any publication.
     * <p>
     * This method is resilient to various errors, ensuring that the GEO record can still be returned even if all the
     * details might not be filled.
     */
    private void fillDetails( GeoRecord record, GeoRetrieveConfig config ) {
        if ( !config.isSubSeriesStatus() && !config.isMeshHeadings() && !config.isLibraryStrategy() && !config.isSampleDetails() ) {
            log.debug( "No need to fill details for " + record + "." );
            return;
        }

        // without details this goes a lot quicker so feedback isn't very important
        log.debug( "Obtaining details for " + record.getGeoAccession() + " " + record.getNumSamples() + " samples..." );

        Document document;
        try {
            document = fetchDetailedGeoSeriesFamilyFromGeo( record.getGeoAccession() );
        } catch ( IOException | SAXParseException e ) {
            if ( config.isIgnoreErrors() ) {
                log.error( "Error while processing MINiML for " + record.getGeoAccession() + ", sample details will not be obtained.", e );
                return;
            } else {
                throw new RuntimeException( "Error while processing MINiML for " + record.getGeoAccession() + ".", e );
            }
        }

        if ( document == null ) {
            log.warn( "Could not find any details for " + record );
            return;
        }

        if ( !record.isSuperSeries() && config.isSubSeriesStatus() ) {
            /*
             * I can't find a better way to get subseries info: the eutils doesn't provide this
             * information other than mentioning (in the summary text) when a series is a superseries. Determining
             * subseries
             * status is impossible without another query. I don't think batch queries for MiniML is possible.
             */
            fillSubSeriesStatus( record, document );
        }

        // another query. Note that new Pubmed IDs generally don't have mesh headings yet, so this might not be that useful.
        if ( config.isMeshHeadings() ) {
            try {
                fillMeshHeadings( record );
            } catch ( IOException e ) {
                String message = "Could not get MeSH headings for " + record.getGeoAccession() + ".";
                if ( config.isIgnoreErrors() ) {
                    log.error( message, e );
                } else {
                    throw new RuntimeException( message, e );
                }
            }
        }

        if ( config.isLibraryStrategy() ) {
            fillLibraryStrategy( record, document );
            fillLibrarySource( record, document );
        }

        if ( config.isSampleDetails() ) {
            // get sample information
            log.debug( String.format( "Obtaining sample details for %s...", record ) );
            fillSampleGeoAccessions( record, document );
            fillSampleChannelDetails( record, document );
            fillSampleDescription( record, document );
            fillDataProcessing( record, document );
        }
    }

    private void fillPlatforms( GeoRecord record, String item ) {
        String[] platformlist = requireNonNull( StringUtils.split( item, ";" ) );
        List<String> finalPlatformIds = new ArrayList<>();
        for ( String p : platformlist ) {
            finalPlatformIds.add( "GPL" + p.trim() );
        }
        record.setPlatform( StringUtils.join( finalPlatformIds, ";" ) );
    }

    /**
     * exposed for testing
     */
    void fillSubSeriesStatus( GeoRecord record, Document detailsDocument ) {
        // e.g. https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE180363&targ=gse&form=xml&view=full
        String result = evaluateToString( xOverallDesign, detailsDocument );
        record.setOverallDesign( StringUtils.strip( result ) );
        NodeList relTypeNodes = evaluate( xRelationType, detailsDocument );
        for ( int i = 0; i < relTypeNodes.getLength(); i++ ) {
            String relType = requireNonNull( relTypeNodes.item( i ) ).getAttributes().getNamedItem( "type" ).getTextContent();
            String relTo = requireNonNull( relTypeNodes.item( i ) ).getAttributes().getNamedItem( "target" ).getTextContent();
            if ( relType != null && relType.startsWith( "SubSeries" ) ) {
                record.setSubSeries( true );
                record.setSubSeriesOf( relTo );
            }
        }
    }

    /**
     * exposed for testing
     */
    void fillSampleChannelDetails( GeoRecord record, Document detailsDocument ) {
        NodeList channelNodes = evaluate( xChannel, detailsDocument );
        Set<String> props = new HashSet<>(); // expect duplicate terms
        Set<String> sampleMolecules = new HashSet<>();
        List<String> sampleExtractProtocols = new ArrayList<>();
        Set<String> sampleLabels = new HashSet<>();
        List<String> sampleLabelProtocols = new ArrayList<>();
        for ( int i = 0; i < channelNodes.getLength(); i++ ) {
            Node item = channelNodes.item( i );
            // this section used to use XPath, but traversing NodeList objects is very inefficient
            for ( Node node = item.getFirstChild(); node != null; node = node.getNextSibling() ) {
                if ( node.getNodeName().equals( "Source" ) || node.getNodeName().equals( "Characteristics" ) ) {
                    String s = node.getTextContent();
                    // skip unadorned numbers
                    if ( StringUtils.isBlank( s ) || StringUtils.isNumericSpace( s ) ) {
                        continue;
                    }
                    props.add( StringUtils.strip( s ) );
                }
                if ( node.getNodeName().equals( "Molecule" ) ) {
                    sampleMolecules.add( getTextValue( node ) );
                }
                if ( node.getNodeName().equals( "Extract-Protocol" ) ) {
                    sampleExtractProtocols.add( getTextValue( node ) );
                }
                if ( node.getNodeName().equals( "Label" ) ) {
                    sampleLabels.add( getTextValue( node ) );
                }
                if ( node.getNodeName().equals( "Label-Protocol" ) ) {
                    sampleLabelProtocols.add( getTextValue( node ) );
                }
            }
        }
        record.setSampleDetails( StringUtils.join( props, ";" ) );
        record.setSampleMolecules( StringUtils.join( sampleMolecules, ";" ) );
        record.setSampleExtractProtocols( StringUtils.join( sampleExtractProtocols, "\n" ) );
        record.setSampleLabels( StringUtils.join( sampleLabels, ";" ) );
        record.setSampleLabelProtocols( StringUtils.join( sampleLabelProtocols, "\n" ) );
    }

    void fillSampleGeoAccessions( GeoRecord record, Document document ) {
        NodeList ls = evaluate( xSampleGeoAccession, document );
        List<String> dp = new ArrayList<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            dp.add( getTextValue( getItem( ls, i ) ) );
        }
        record.setSampleGEOAccessions( dp );
    }

    void fillSampleDescription( GeoRecord record, Document document ) {
        NodeList ls = evaluate( xSampleDescription, document );
        List<String> dp = new ArrayList<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            dp.add( getTextValue( getItem( ls, i ) ) );
        }
        record.setSampleDescriptions( String.join( "\n", dp ) );
    }

    void fillDataProcessing( GeoRecord record, Document document ) {
        NodeList ls = evaluate( xDataProcessing, document );
        List<String> dp = new ArrayList<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            dp.add( getTextValue( getItem( ls, i ) ) );
        }
        record.setSampleDataProcessing( String.join( "\n", dp ) );
    }

    void fillLibraryStrategy( GeoRecord record, Document document ) {
        NodeList ls = evaluate( xLibraryStrategy, document );
        Set<String> libraryStrategies = new HashSet<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            libraryStrategies.add( getTextValue( getItem( ls, i ) ) );
        }
        record.setLibraryStrategy( StringUtils.join( libraryStrategies, ";" ) );
    }

    private void fillLibrarySource( GeoRecord record, Document document ) {
        NodeList ls = evaluate( xLibrarySource, document );
        Set<String> librarySources = new HashSet<>();
        for ( int i = 0; i < ls.getLength(); i++ ) {
            librarySources.add( getTextValue( getItem( ls, i ) ) );
        }
        record.setLibrarySource( StringUtils.join( librarySources, ";" ) );
    }

    private void fillOrganisms( GeoRecord record, String item ) {
        String input = item.replace( "; ", ";" );
        String[] taxonArray = input.split( ";" );
        Collection<String> taxa = Arrays.stream( taxonArray ).map( String::trim ).collect( Collectors.toList() );
        record.setOrganisms( taxa );
    }

    private void fillPubMedIds( GeoRecord record, String item ) {
        List<String> pubmedIds = Arrays.stream( item.split( "\\n" ) )
                .map( StringUtils::stripToNull )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );
        record.setPubMedIds( pubmedIds );
    }

    private void fillMeshHeadings( GeoRecord record ) throws IOException {
        if ( record.getPubMedIds() == null || record.getPubMedIds().isEmpty() ) {
            return;
        }
        Collection<BibliographicReference> refs = pubmedFetcher.retrieve( record.getPubMedIds() );
        Collection<String> meshheadings = new ArrayList<>();
        for ( BibliographicReference ref : refs ) {
            for ( MedicalSubjectHeading mesh : ref.getMeshTerms() ) {
                meshheadings.add( mesh.getTerm() );
            }
        }
        record.setMeshHeadings( meshheadings );
    }

    private String entryTypeFromRecordType( GeoRecordType recordType ) {
        switch ( recordType ) {
            case SERIES:
                return "gse";
            case PLATFORM:
                return "gpl";
            default:
                throw new UnsupportedOperationException( "Unsupported record type: " + recordType );
        }
    }

    @Nullable
    private Document fetchDetailedGeoSeriesFamilyFromGeo( String geoAccession ) throws IOException, SAXParseException {
        Document document;
        try {
            if ( ( document = fetchDetailedGeoSeriesFamilyFromGeoFtp( geoAccession ) ) != null ) {
                return document;
            }
        } catch ( Exception e ) {
            if ( e.getCause() instanceof SAXParseException ) {
                // the stacktrace is not very useful, and if the error is reproducible, it will also happen on the GEO Query side
                log.warn( "Failed to parse detailed GEO series MINiML from FTP for " + geoAccession + ", will attempt to retrieve it from GEO query...", e );
                if ( ( document = fetchDetailedGeoSeriesFamilyFromGeoQuery( geoAccession ) ) != null ) {
                    return document;
                }
            } else {
                throw e;
            }
        }
        return null;
    }

    @Nullable
    Document fetchDetailedGeoSeriesFamilyFromGeoFtp( String geoAccession ) throws IOException {
        try {
            return fetchDetailedGeoSeriesFamilyFromGeoFtp( geoAccession, null );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof SAXParseException ) {
                // the stack trace is not very useful
                log.warn( "GEO series file for " + geoAccession + " appears to contain invalid characters, will attempt to parse it with Windows-1251 character encoding." );
                return fetchDetailedGeoSeriesFamilyFromGeoFtp( geoAccession, "windows-1252" );
            } else {
                throw e;
            }
        }
    }

    /**
     * Fetch a detailed GEO series MINiML document.
     */
    @Nullable
    private Document fetchDetailedGeoSeriesFamilyFromGeoFtp( String geoAccession, @Nullable String encoding ) throws IOException {
        URL documentUrl = getUrlForSeriesFamily( geoAccession, GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML );
        return execute( ( ctx ) -> {
            // important note: GZIPInputStream can fail and prevent the stream from being closed, so there must be two
            // try-with-resources statements
            try ( InputStream is = documentUrl.openStream(); TarArchiveInputStream tis = new TarArchiveInputStream( new GZIPInputStream( is ) ) ) {
                TarArchiveEntry entry;
                while ( ( entry = tis.getNextEntry() ) != null ) {
                    if ( entry.getName().equals( geoAccession + "_family.xml" ) ) {
                        String entryUrl = documentUrl + "!" + entry.getName();
                        log.debug( "Parsing MINiML for " + geoAccession + " from " + entryUrl + "..." );
                        return encoding != null ? EntrezXmlUtils.parse( tis, encoding ) : EntrezXmlUtils.parse( tis );
                    }
                }
            } catch ( FileNotFoundException e ) {
                return null;
            }
            log.warn( "No entry with name " + geoAccession + "_family.xml" + " found in " + documentUrl + "." );
            return null;
        }, "retrieve " + documentUrl );
    }

    /**
     * Retrieve a detailed GEO series document directly from GEO.
     */
    @Nullable
    Document fetchDetailedGeoSeriesFamilyFromGeoQuery( String geoAccession ) throws IOException {
        URL documentUrl = getUrlForSeriesFamily( geoAccession, GeoSource.QUERY, GeoFormat.MINIML );
        return execute( ( ctx ) -> {
            try ( InputStream tis = documentUrl.openStream() ) {
                log.debug( "Parsing MINiML for " + geoAccession + " from " + documentUrl + "..." );
                return EntrezXmlUtils.parse( tis );
            } catch ( FileNotFoundException e ) {
                return null;
            }
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
        return execute( EntrezUtils.retryNicely( ( ctx ) -> {
            try ( InputStream is = openUrlWithMaxSize( url, MAX_MINIML_RECORD_SIZE ) ) {
                return EntrezXmlUtils.parse( is );
            } catch ( IOException ioe ) {
                if ( isEligibleForRetry( ioe ) ) {
                    throw ioe;
                } else {
                    throw new NonRetryableIOException( ioe );
                }
            }
        }, ncbiApiKey ), "parse MINiML from URL " + url );
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
        } catch ( NonRetryableIOException e ) {
            throw e.getCause();
        }
    }

    /**
     * Allows for raising an {@link IOException} without having it retried.
     */
    private static class NonRetryableIOException extends RuntimeException {

        private NonRetryableIOException( IOException cause ) {
            super( cause );
        }

        @Override
        public IOException getCause() {
            return ( IOException ) super.getCause();
        }
    }
}