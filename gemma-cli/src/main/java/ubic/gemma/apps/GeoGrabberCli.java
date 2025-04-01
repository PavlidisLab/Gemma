/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.model.GeoSeriesType;
import ubic.gemma.core.loader.expression.geo.service.*;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Scans GEO for experiments that are not in Gemma, subject to some filtering criteria, outputs to a file for further
 * screening. See <a href="https://github.com/PavlidisLab/Gemma/issues/169">#160</a>
 *
 * @author paul
 */
public class GeoGrabberCli extends AbstractAuthenticatedCLI implements InitializingBean {

    private static final String
            ACCESSION_OPTION = "e",
            ACCESSION_FILE_OPTION = "f",
            PLATFORMS_OPTION = "y";

    private enum Mode {
        GET_PLATFORMS,
        GET_DATASETS,
        BROWSE_DATASETS
    }

    /**
     * Retry policy for retrieving GEO records.
     */
    private static final SimpleRetry<IOException> retryTemplate = new SimpleRetry<>( 5, 500, 1.5, IOException.class, GeoGrabberCli.class.getName() );

    /**
     * Default chunk size to use when querying GEO records.
     */
    private static final int DEFAULT_CHUNK_SIZE = 100;
    /**
     * Chunk size to use when rewinding.
     */
    private static final int REWIND_CHUNK_SIZE = 10 * DEFAULT_CHUNK_SIZE;

    private static final DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH );

    private static final GeoSeriesType[] SERIES_TYPES = new GeoSeriesType[] {
            GeoSeriesType.EXPRESSION_PROFILING_BY_ARRAY,
            GeoSeriesType.EXPRESSION_PROFILING_BY_HIGH_THROUGHPUT_SEQUENCING,
            GeoSeriesType.EXPRESSION_PROFILING_BY_MPSS,
            GeoSeriesType.EXPRESSION_PROFILING_BY_RT_PRC,
            GeoSeriesType.EXPRESSION_PROFILING_BY_SAGE,
            GeoSeriesType.EXPRESSION_PROFILING_BY_SNP_ARRAY,
            GeoSeriesType.EXPRESSION_PROFILING_BY_TILING_ARRAY
    };

    /**
     * Header used when browsing or obtaining datasets.
     */
    private static final String[] DATASET_HEADER = { "Acc", "ReleaseDate", "Taxa", "Platforms", "AllPlatformsInGemma",
            "Affy", "NumSamples", "Type", "SuperSeries", "SubSeriesOf", "PubMed", "Title", "Summary", "MeSH",
            "SampleTerms", "LibraryStrategy", "LibrarySource", "OverallDesign", "Keywords" };

    /**
     * Preset when seeking records.
     */
    public static final GeoRetrieveConfig MINIMAL = GeoRetrieveConfig.builder().build();

    /**
     * Detailed preset for this CLI used when fetching GEO records.
     * @see GeoRetrieveConfig#DETAILED
     */
    private static final GeoRetrieveConfig DETAILED = GeoRetrieveConfig.builder()
            .subSeriesStatus( true )
            .meshHeadings( true )
            .libraryStrategy( true )
            .sampleDetails( true )
            // ignore errors when fetching additional information
            .ignoreErrors( true )
            .build();

    // operating mode
    private Mode mode;

    @Nullable
    private Path outputFileName;

    // options for retrieving datasets
    private String accession;
    private Path accessionFile;

    // options for browsing datasets
    @Nullable
    private Date dateLimit;
    @Nullable
    private String gseLimit;
    @Nullable
    private Set<String> limitPlatform = null;
    @Nullable
    private String startFrom = null;
    @Nullable
    private Date startDate = null;
    private int chunkSize = DEFAULT_CHUNK_SIZE;
    private boolean ignoreBlacklisted = true;
    private boolean ignoreAlreadyInGemma = true;
    private boolean ignoreNoPlatform = true;

    @Autowired
    private ExpressionExperimentService ees;
    @Autowired
    private TaxonService ts;
    @Autowired
    private ArrayDesignService ads;
    // TODO: this should be an injected component
    private GeoBrowser gbs;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    @Override
    public void afterPropertiesSet() {
        gbs = new GeoBrowserImpl( ncbiApiKey );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.ANALYSIS;
    }

    @Override
    public String getCommandName() {
        return "listGEOData";
    }

    @Override
    public String getShortDesc() {
        return "Grab information on GEO data sets not yet in the system, working backwards in time";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "output" )
                .longOpt( "output" )
                .hasArg().type( Path.class )
                .desc( "File path for output (defaults to standard output)" )
                .build() );

        // for retrieving platforms
        options.addOption( PLATFORMS_OPTION, false, "Fetch a list of all platforms instead of experiments (-startdate, -date, -startat and -gselimit are ignored)" );

        // for retrieving datasets
        options.addOption( ACCESSION_OPTION, "acc", true, "A comma-delimited list of accessions to retrieve from GEO" );
        options.addOption( Option.builder( ACCESSION_FILE_OPTION )
                .longOpt( "file" )
                .hasArg().type( Path.class )
                .desc( "A file containing accessions to retrieve from GEO" )
                .build() );

        // for browsing datasets
        options.addOption( "platformLimit", true, "Limit to selected platforms" );
        options.addOption( "startdate", true, "Attempt to 'fast-rewind' to the given date in format yyyy-MM-dd or yyyy.MM.dd and continue retrieving from there" );
        options.addOption( "date", true, "A release date to stop the search in format yyyy-MM-dd or yyyy.MM.dd (e.g. 2010.01.12). Records on that date will not be processed." );
        options.addOption( "startat", true, "Attempt to 'fast-rewind' to the given GSE ID and continue retrieving from there " );
        options.addOption( "gselimit", true, "A GSE at which to stop the search. Record with the GSE ID will not be processed." );
        options.addOption( "raw", false, "Display raw, unfiltered results from GEO. Datasets that are blacklisted or already in Gemma will be included in the output." );

        options.addOption( Option.builder( "chunkSize" )
                .longOpt( "chunk-size" )
                .hasArg().type( Number.class )
                .desc( "Chunk size to use when retrieving GEO records (defaults to " + DEFAULT_CHUNK_SIZE + ")" )
                .build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws org.apache.commons.cli.ParseException {
        if ( commandLine.hasOption( PLATFORMS_OPTION ) ) {
            mode = Mode.GET_PLATFORMS;
        } else if ( commandLine.hasOption( ACCESSION_OPTION ) || commandLine.hasOption( ACCESSION_FILE_OPTION ) ) {
            mode = Mode.GET_DATASETS;
        } else {
            mode = Mode.BROWSE_DATASETS;
        }

        if ( commandLine.hasOption( "output" ) ) {
            this.outputFileName = commandLine.getParsedOptionValue( "output" );
        }

        this.accession = commandLine.getOptionValue( ACCESSION_OPTION );
        this.accessionFile = commandLine.getParsedOptionValue( ACCESSION_FILE_OPTION );

        if ( commandLine.hasOption( "date" ) ) {
            try {
                // this is a user input, so we have to respect its locale
                this.dateLimit = DateUtils.parseDate( commandLine.getOptionValue( "date" ), "yyyy-MM-dd", "yyyy.MM.dd" );
            } catch ( ParseException e ) {
                throw new IllegalArgumentException( "Could not parse -date: " + commandLine.getOptionValue( "date" ) + ", use the yyyy-MM-dd or yyyy.MM.dd format." );
            }
        }
        if ( commandLine.hasOption( "gselimit" ) ) {
            this.gseLimit = commandLine.getOptionValue( "gselimit" );
        }

        if ( commandLine.hasOption( "platformLimit" ) ) {
            //      this.limitPlatform  = AbstractCLIContextCLI.readListFileToStrings( commandLine.getOptionValue( "platformLimit" ) );
            String gpls = commandLine.getOptionValue( "platformLimit" );
            this.limitPlatform = new HashSet<>( Arrays.asList( StringUtils.split( gpls, "," ) ) );
        }
        if ( commandLine.hasOption( "startat" ) ) {
            this.startFrom = commandLine.getOptionValue( "startat" );
            if ( !startFrom.startsWith( "GSE" ) ) {
                throw new IllegalArgumentException( "Must provide a valid GSE for the startat option" );
            }
        }
        if ( commandLine.hasOption( "startdate" ) ) {
            try {
                // this is a user input, so we have to respect its locale
                this.startDate = DateUtils.parseDate( commandLine.getOptionValue( "startdate" ), "yyyy-MM-dd", "yyyy.MM.dd" );
            } catch ( ParseException e ) {
                throw new IllegalArgumentException( "Could not parse start date: " + commandLine.getOptionValue( "startdate" ) + ", use the yyyy-MM-dd or yyyy.MM.dd format." );
            }
            if ( dateLimit != null ) {
                if ( !beforeOrEquals( dateLimit, startDate ) ) {
                    throw new IllegalArgumentException( "The -date option: " + dateLimit + " has to be earlier than -startdate: " + startDate + "." );
                }
            }
        }

        if ( commandLine.hasOption( "raw" ) ) {
            log.warn( "The -raw option is specified, no filtering will be applied on GEO records. This option is not suitable for a GEO scrape." );
            ignoreBlacklisted = false;
            ignoreAlreadyInGemma = false;
            ignoreNoPlatform = false;
        }

        if ( commandLine.hasOption( "chunkSize" ) ) {
            int cs = ( ( Number ) commandLine.getParsedOptionValue( "chunkSize" ) ).intValue();
            Assert.isTrue( cs >= 1, "Chunk size must be at least one." );
            this.chunkSize = cs;
        }
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        switch ( mode ) {
            case GET_PLATFORMS:
                getPlatforms();
                break;
            case GET_DATASETS:
                Set<String> accessions = new HashSet<>();
                if ( accession != null ) {
                    Collections.addAll( accessions, StringUtils.split( accession, "," ) );
                }
                if ( accessionFile != null ) {
                    accessions.addAll( Files.readAllLines( accessionFile ) );
                }
                accessions.removeIf( StringUtils::isBlank );
                getDatasets( accessions );
                break;
            case BROWSE_DATASETS:
                browseDatasets( startFrom, gseLimit, startDate, dateLimit, limitPlatform, Arrays.asList( SERIES_TYPES ) );
                break;
            default:
                throw new IllegalStateException( "Unknown mode " + mode );
        }
    }

    private void getPlatforms() throws IOException {
        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( "Acc", "ReleaseDate", "Taxa", "Title", "Summary", "TechType" )
                .build();
        Collection<GeoRecord> allGEOPlatforms = gbs.getAllGeoRecords( GeoRecordType.PLATFORM, getAllowedTaxa(), 10000 );
        log.info( "Fetched " + allGEOPlatforms.size() + " records" );
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            for ( GeoRecord geoRecord : allGEOPlatforms ) {
                os.printRecord( geoRecord.getGeoAccession(), dateFormat.format( geoRecord.getReleaseDate() ),
                        StringUtils.join( geoRecord.getOrganisms(), "," ), geoRecord.getTitle(),
                        geoRecord.getSummary(), geoRecord.getSeriesType() );
                os.flush();
            }
        }
    }

    private void getDatasets( Collection<String> accessions ) throws IOException {
        if ( accessions.isEmpty() ) {
            throw new IllegalArgumentException( "No datasets accessions provided." );
        }
        Map<String, ArrayDesign> seenPlatforms = new HashMap<>();
        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( DATASET_HEADER )
                .build();
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            for ( GeoRecord record : gbs.getGeoRecords( GeoRecordType.SERIES, accessions, DETAILED ) ) {
                writeDataset( record, areAllPlatformsInGemma( record, seenPlatforms ), isAffymetrix( record, seenPlatforms ), os );
            }
        }
    }

    private void browseDatasets( @Nullable String startFrom, @Nullable String gseLimit, @Nullable Date startDate, @Nullable Date dateLimit, @Nullable Collection<String> limitPlatform, Collection<GeoSeriesType> seriesTypes ) throws IOException {
        Set<String> seen = new HashSet<>();
        Map<String, ArrayDesign> seenArrayDesigns = new HashMap<>();
        Collection<String> allowedTaxa = getAllowedTaxa();

        String searchMessage = "Browsing GEO series with the following characteristics:";
        searchMessage += "\n\t" + "Taxa: " + String.join( ", ", allowedTaxa );
        if ( limitPlatform != null ) {
            searchMessage += "\n\t" + "Platforms: " + String.join( ", ", limitPlatform );
        }
        searchMessage += "\n\tSeries Types: " + seriesTypes.stream().map( GeoSeriesType::getIdentifier ).collect( Collectors.joining( ", " ) );
        log.info( searchMessage );

        GeoQuery query = searchGeoSeries( allowedTaxa, limitPlatform, seriesTypes );

        int start = seekRewindPoint( query, startFrom, gseLimit, startDate, dateLimit );
        if ( start == -1 ) {
            log.info( "Could not find the rewind point." );
            return;
        }

        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( DATASET_HEADER )
                .build();
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            int numProcessed = 0;
            int numUsed = 0;
            int totalRecords = Integer.MAX_VALUE;
            for ( ; start < totalRecords; start += chunkSize ) {
                log.debug( "Searching from " + start + ", seeking " + chunkSize + " records" );

                Slice<GeoRecord> recs = getGeoRecords( query, start, chunkSize, true );
                totalRecords = requireNonNull( recs.getTotalElements() ).intValue();

                log.debug( "Retrieved " + recs.size() + " GEO records." ); // we skip ones that are not using taxa of interest

                for ( GeoRecord geoRecord : recs ) {
                    if ( numProcessed > 0 && numProcessed % 50 == 0 ) {
                        log.info( "Processed " + numProcessed + " GEO records, retained " + numUsed + " so far" );
                    }
                    numProcessed++;

                    // check ending conditions, especially if we are fast-rewinding
                    if ( dateLimit != null && beforeOrEquals( geoRecord.getReleaseDate(), dateLimit ) ) {
                        log.info( "Stopping as reached date limit" );
                        return;
                    }
                    if ( gseLimit != null && gseLimit.equals( geoRecord.getGeoAccession() ) ) {
                        log.info( "Stopping as have reached " + gseLimit );
                        return;
                    }

                    log.debug( "Processing " + geoRecord.getGeoAccession() + " released on " + geoRecord.getReleaseDate() + "..." );

                    if ( !seen.add( geoRecord.getGeoAccession() ) ) {
                        log.warn( geoRecord + ": skipping, this dataset was already seen." ); // this would be a bug IMO, want to avoid!
                        continue;
                    }

                    if ( ignoreBlacklisted && ees.isBlackListed( geoRecord.getGeoAccession() ) ) {
                        log.warn( geoRecord + ": skipping, blacklisted." );
                        continue;
                    }

                    if ( ignoreAlreadyInGemma && ees.findByShortName( geoRecord.getGeoAccession() ) != null ) {
                        log.debug( geoRecord + ": skipping, already in Gemma (by short name)." );
                        continue;
                    }

                    if ( ignoreAlreadyInGemma && !ees.findByAccession( geoRecord.getGeoAccession() ).isEmpty() ) {
                        log.debug( geoRecord + ": skipping, already in Gemma (by accession)." );
                        continue;
                    }

                    if ( ignoreNoPlatform && StringUtils.isBlank( geoRecord.getPlatform() ) ) {
                        log.warn( geoRecord.getGeoAccession() + ": skipping, does not have any platform." );
                        continue;
                    }

                    // we skip if all the platforms for the GSE are blacklisted
                    if ( ignoreBlacklisted && areAllPlatformsBlacklisted( geoRecord ) ) {
                        log.warn( geoRecord.getGeoAccession() + ": skipping, all platforms are blacklisted." );
                        continue;
                    }

                    writeDataset( geoRecord, areAllPlatformsInGemma( geoRecord, seenArrayDesigns ), isAffymetrix( geoRecord, seenArrayDesigns ), os );

                    numUsed++;
                }
            }
        }
    }

    /**
     * TODO: use a binary search for date-based rewinding
     * @return the rewind point if found, otherwise -1
     */
    private int seekRewindPoint( GeoQuery query, @Nullable String startFrom, @Nullable String gseLimit, @Nullable Date startDate, @Nullable Date dateLimit ) throws IOException {
        if ( startFrom == null && startDate == null ) {
            return 0;
        } else if ( startFrom != null ) {
            return seekRewindPointByAccession( query, startFrom, gseLimit, dateLimit );
        } else {
            return seekRewindPointByDate( query, startDate, dateLimit );
        }
    }

    private int seekRewindPointByAccession( GeoQuery query, String startFrom, @Nullable String gseLimit, @Nullable Date dateLimit ) throws IOException {
        log.info( "Seeking rewind point from " + startFrom + " with chunks of " + REWIND_CHUNK_SIZE + " records..." );
        // we're not fetching details, so we can handle larger chunks
        int totalRecords = Integer.MAX_VALUE;
        for ( int start = 0; start < totalRecords; start += REWIND_CHUNK_SIZE ) {
            Slice<GeoRecord> records = getGeoRecords( query, start, REWIND_CHUNK_SIZE, false );
            totalRecords = requireNonNull( records.getTotalElements() ).intValue();
            GeoRecord firstRecord = records.iterator().next();
            log.info( String.format( "Currently at %s (%d/%d) released on %s", firstRecord.getGeoAccession(),
                    start, totalRecords, firstRecord.getReleaseDate() ) );
            for ( int i = 0; i < records.size(); i++ ) {
                GeoRecord geoRecord = records.get( i );
                // check ending conditions, especially if we are fast-rewinding
                if ( dateLimit != null && beforeOrEquals( geoRecord.getReleaseDate(), dateLimit ) ) {
                    log.info( "Stopped rewinding as reached date limit." );
                    return -1;
                }
                if ( gseLimit != null && gseLimit.equals( geoRecord.getGeoAccession() ) ) {
                    log.info( "Stopped rewinding as have reached " + gseLimit + "." );
                    return -1;
                }
                if ( startFrom != null && startFrom.equals( geoRecord.getGeoAccession() ) ) {
                    log.info( "Located requested starting point of " + startFrom + ", resuming detailed queries at " + ( start + i ) + "." );
                    return start + i;
                }
            }
        }
        log.info( "Reached end of records while seeking rewind point." );
        return -1;
    }

    private int seekRewindPointByDate( GeoQuery query, Date startDate, @Nullable Date dateLimit ) throws IOException {
        log.info( "Seeking rewind point from " + startDate + " using a binary search..." );
        // we're not fetching details, so we can handle larger chunks
        int start = 0;
        int end = query.getTotalRecords();
        while ( true ) {
            int pos = start + ( ( end - start ) / 2 );
            Slice<GeoRecord> records = getGeoRecords( query, pos, 1, false );
            GeoRecord record = records.iterator().next();

            log.info( "Currently at " + record.getGeoAccession() + " (" + pos + "/" + start + ".." + end + ") released on " + record.getReleaseDate() );

            if ( beforeOrEquals( record.getReleaseDate(), startDate ) ) {
                end = pos;
            } else {
                start = pos + 1;
            }

            if ( start == end ) {
                // check ending conditions, especially if we are fast-rewinding
                if ( dateLimit != null && beforeOrEquals( record.getReleaseDate(), dateLimit ) ) {
                    log.info( "Stopped rewinding as reached date limit." );
                    return -1;
                } else {
                    log.info( "Located requested starting date of " + startDate + ", resuming detailed queries at " + pos + "." );
                    return start; // done, I guess?
                }
            }
        }
    }

    private Set<String> getAllowedTaxa() {
        Set<String> allowedTaxa = new HashSet<>();
        for ( Taxon t : ts.loadAll() ) {
            allowedTaxa.add( t.getScientificName() );
        }
        log.info( allowedTaxa.size() + " Taxa considered usable" );
        return allowedTaxa;
    }

    /**
     * Check if a give date is before or exactly on another one.
     */
    private boolean beforeOrEquals( @Nullable Date a, Date b ) {
        return a != null && ( a.equals( b ) || a.before( b ) );
    }

    private GeoQuery searchGeoSeries( Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatform, Collection<GeoSeriesType> seriesTypes ) throws IOException {
        return retryTemplate.execute( ( attempt, lastAttempt ) -> gbs.searchGeoRecords( GeoRecordType.SERIES, null, null, allowedTaxa, limitPlatform, seriesTypes ), "searching GEO records" );
    }

    private Slice<GeoRecord> getGeoRecords( GeoQuery query, int start, int chunkSize, boolean fetchDetails ) throws IOException {
        return retryTemplate.execute( ( attempt, lastAttempt ) -> gbs.retrieveGeoRecords( query, start, chunkSize, fetchDetails ? DETAILED : MINIMAL ), "fetching GEO records" );
    }

    private Appendable getOutputWriter() throws IOException {
        if ( outputFileName != null ) {
            log.info( "Writing output to " + outputFileName );
            if ( Files.exists( outputFileName ) ) {
                log.warn( "Overwriting existing file..." );
            }
            return Files.newBufferedWriter( outputFileName );
        } else {
            return getCliContext().getOutputStream();
        }
    }

    private boolean areAllPlatformsBlacklisted( GeoRecord geoRecord ) {
        return Arrays.stream( geoRecord.getPlatform().split( ";" ) )
                .allMatch( p -> ees.isBlackListed( p ) );
    }

    /**
     * Check if all the platforms referenced in a GEO record are present in Gemma.
     */
    private boolean areAllPlatformsInGemma( GeoRecord geoRecord, Map<String, ArrayDesign> seenPlatforms ) {
        return Arrays.stream( geoRecord.getPlatform().split( ";" ) )
                .allMatch( p -> {
                    ArrayDesign ad = seenPlatforms.computeIfAbsent( p, shortName -> {
                        ArrayDesign found = ads.findByShortName( shortName );
                        if ( found != null ) {
                            found = ads.thawLite( found );
                        }
                        return found;
                    } );
                    return ad != null;
                } );
    }

    private boolean isAffymetrix( GeoRecord geoRecord, Map<String, ArrayDesign> seenPlatforms ) {
        String[] platforms = geoRecord.getPlatform().split( ";" );
        for ( String p : platforms ) {
            ArrayDesign ad = seenPlatforms.computeIfAbsent( p, shortName -> {
                ArrayDesign found = ads.findByShortName( shortName );
                if ( found != null ) {
                    found = ads.thawLite( found );
                }
                return found;
            } );
            if ( ad != null && ad.getDesignProvider() != null && "Affymetrix".equals( ad.getDesignProvider().getName() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stop words to exclude when extracting keywords from GEO records.
     */
    private static final Set<String> stopWords = new HashSet<>( Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not",
            "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was",
            "will", "with" ) );

    /**
     * Extract keywords from a GEO record.
     */
    Set<String> extractKeywords( GeoRecord geoRecord ) {
        String allText = geoRecord.getTitle() + "\n"
                + geoRecord.getSummary() + "\n"
                + geoRecord.getOverallDesign() + "\n"
                + geoRecord.getSampleDescriptions() + "\n"
                + geoRecord.getSampleExtractProtocols() + "\n"
                + geoRecord.getSampleLabelProtocols() + "\n"
                + geoRecord.getSampleDataProcessing();
        HashSet<String> keywords = new HashSet<>( Arrays.asList( StringUtils.split( allText.toLowerCase()
                // this will split on any punctuation except hyphens
                .replaceAll( "[^\\P{Punct}-]", " " ) ) ) );
        keywords.removeAll( stopWords );
        return keywords;
    }

    private void writeDataset( GeoRecord geoRecord, boolean allPlatformsInGemma, boolean isAffymetrix, CSVPrinter os ) throws IOException {
        os.printRecord( geoRecord.getGeoAccession(), dateFormat.format( geoRecord.getReleaseDate() ),
                StringUtils.join( geoRecord.getOrganisms(), "," ), geoRecord.getPlatform(), allPlatformsInGemma,
                isAffymetrix, geoRecord.getNumSamples(), geoRecord.getSeriesType(), geoRecord.isSuperSeries(),
                geoRecord.getSubSeriesOf(), StringUtils.join( geoRecord.getPubMedIds(), "," ), geoRecord.getTitle(), geoRecord.getSummary(),
                StringUtils.join( geoRecord.getMeshHeadings(), "," ), geoRecord.getSampleDetails(),
                geoRecord.getLibraryStrategy(), geoRecord.getLibrarySource(),
                StringUtils.replace( geoRecord.getOverallDesign(), "\n", "\\n" ),
                extractKeywords( geoRecord ).stream().sorted().collect( Collectors.joining( ";" ) ) );
        os.flush();
    }
}
