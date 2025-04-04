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
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.model.GeoSeriesType;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
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

    private static final int NCBI_CHUNK_SIZE = 100;
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
    private int chunkSize = NCBI_CHUNK_SIZE;

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
        gbs = new GeoBrowser( ncbiApiKey );
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
        options.addOption( Option.builder( "output" ).desc( "File path for output (defaults to standard output)" ).argName( "path" ).hasArg().type( Path.class ).build() );

        // for retrieving platforms
        options.addOption( Option.builder( PLATFORMS_OPTION ).desc( "Fetch a list of all platforms instead of experiments (-startdate, -date, -startat and -gselimit are ignored)" ).build() );

        // for retrieving datasets
        options.addOption( Option.builder( ACCESSION_OPTION ).longOpt( "acc" ).hasArg().desc( "A comma-delimited list of accessions to retrieve from GEO" ).build() );
        options.addOption( Option.builder( ACCESSION_FILE_OPTION ).longOpt( "file" ).hasArg().desc( "A file containing accessions to retrieve from GEO" ).type( Path.class ).build() );

        // for browsing datasets
        options.addOption(
                Option.builder( "date" ).longOpt( null ).desc( "A release date to stop the search in format yyyy-MM-dd or yyyy.MM.dd (e.g. 2010.01.12)" )
                        .argName( "date limit" ).hasArg().build() );
        options.addOption(
                Option.builder( "gselimit" ).longOpt( null ).desc( "A GSE at which to stop the search" ).argName( "GSE identifier" ).hasArg()
                        .build() );
        options.addOption( Option.builder( "platformLimit" ).longOpt( null ).desc( "Limit to selected platforms" )
                .argName( "comma-delimited list of GPLs" ).hasArg().build() );

        options.addOption(
                Option.builder( "startat" ).hasArg().argName( "GSE ID" ).desc( "Attempt to 'fast-rewind' to the given GSE ID and continue retrieving from there " ).build() );

        options.addOption(
                Option.builder( "startdate" ).hasArg().argName( "date" ).desc( "Attempt to 'fast-rewind' to the given date in format yyyy-MM-dd or yyyy.MM.dd " +
                        "and continue retrieving from there " ).build() );

        options.addOption( Option.builder( "chunkSize" ).hasArg().type( Number.class ).desc( "Chunk size to use when retrieving GEO records (defaults to " + NCBI_CHUNK_SIZE + ")" ).build() );
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
                throw new IllegalArgumentException( "Could not parse date " + commandLine.getOptionValue( "date" ) + ", use the yyyy-MM-dd or yyyy.MM.dd format." );
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
                throw new IllegalArgumentException( "Could not parse date " + commandLine.getOptionValue( "startdate" ) + ", use the yyyy-MM-dd or yyyy.MM.dd format." );
            }
            if ( dateLimit != null ) {
                if ( dateLimit.after( startDate ) ) {
                    throw new IllegalArgumentException( "startdate has to be later than the -date option" );
                }
            }
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
        Collection<GeoRecord> allGEOPlatforms = gbs.getAllGEOPlatforms( getAllowedTaxa() );
        log.info( "Fetched " + allGEOPlatforms.size() + " records" );
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            for ( GeoRecord geoRecord : allGEOPlatforms ) {
                os.printRecord( geoRecord.getGeoAccession(), dateFormat.format( geoRecord.getReleaseDate() ),
                        StringUtils.join( geoRecord.getOrganisms(), "," ), geoRecord.getTitle(),
                        geoRecord.getSummary(), geoRecord.getSeriesType() );
            }
        }
    }

    private static final String[] DATASET_HEADER = { "Acc", "ReleaseDate", "Taxa", "Platforms", "AllPlatformsInGemma",
            "Affy", "NumSamples", "Type", "SuperSeries", "SubSeriesOf", "PubMed", "Title", "Summary", "MeSH",
            "SampleTerms", "LibraryStrategy", "OverallDesign" };

    private void getDatasets( Collection<String> accessions ) throws IOException {
        if ( accessions.isEmpty() ) {
            throw new IllegalArgumentException( "No datasets accessions provided." );
        }
        Map<String, ArrayDesign> seenPlatforms = new HashMap<>();
        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( DATASET_HEADER )
                .build();
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            for ( GeoRecord record : gbs.getGeoRecords( accessions, true ) ) {
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

        int start = seekRewindPoint( startFrom, gseLimit, startDate, dateLimit, limitPlatform, allowedTaxa, seriesTypes );
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
            for ( ; ; start += chunkSize ) {
                log.debug( "Searching from " + start + ", seeking " + chunkSize + " records" );

                List<GeoRecord> recs = getGeoRecords( start, chunkSize, allowedTaxa, true, limitPlatform, seriesTypes );

                if ( recs.isEmpty() ) {
                    log.info( "Seem to have hit end of records, bailing" );
                    break;
                }

                log.debug( "Retrieved " + recs.size() + " GEO records." ); // we skip ones that are not using taxa of interest

                for ( GeoRecord geoRecord : recs ) {
                    // check ending conditions, especially if we are fast-rewinding
                    if ( dateLimit != null && beforeOrEquals( geoRecord.getReleaseDate(), dateLimit ) ) {
                        log.info( "Stopping as reached date limit" );
                        break;
                    }
                    if ( gseLimit != null && gseLimit.equals( geoRecord.getGeoAccession() ) ) {
                        log.info( "Stopping as have reached " + gseLimit );
                        break;
                    }

                    log.debug( "Processing " + geoRecord.getGeoAccession() + " released on " + geoRecord.getReleaseDate() + "..." );

                    numProcessed++;
                    if ( numProcessed % 50 == 0 ) {
                        log.info( "Processed " + numProcessed + " GEO records, retained " + numUsed + " so far" );
                    }

                    if ( !seen.add( geoRecord.getGeoAccession() ) ) {
                        log.warn( geoRecord + ": skipping, this dataset was already seen." ); // this would be a bug IMO, want to avoid!
                        continue;
                    }

                    if ( ees.isBlackListed( geoRecord.getGeoAccession() ) ) {
                        continue;
                    }

                    if ( ees.findByShortName( geoRecord.getGeoAccession() ) != null ) {
                        continue;
                    }

                    if ( !ees.findByAccession( geoRecord.getGeoAccession() ).isEmpty() ) {
                        continue;
                    }

                    if ( StringUtils.isBlank( geoRecord.getPlatform() ) ) {
                        log.warn( geoRecord.getGeoAccession() + ": skipping, does not have any platform." );
                        continue;
                    }

                    // we skip if all the platforms for the GSE are blacklisted
                    if ( areAllPlatformsBlacklisted( geoRecord ) ) {
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
    private int seekRewindPoint( @Nullable String startFrom, @Nullable String gseLimit, @Nullable Date startDate, @Nullable Date dateLimit, @Nullable Collection<String> limitPlatform, Collection<String> allowedTaxa, Collection<GeoSeriesType> seriesTypes ) throws IOException {
        if ( startFrom == null && startDate == null ) {
            return 0;
        } else if ( startFrom != null ) {
            log.info( "Seeking rewind point from " + startFrom );
        } else {
            log.info( "Seeking rewind point from " + startDate );
        }
        // we're not fetching details, so we can handle larger chunks
        int rewindChunkSize = 10 * NCBI_CHUNK_SIZE;
        for ( int start = 0; ; start += rewindChunkSize ) {
            List<GeoRecord> records = getGeoRecords( start, rewindChunkSize, allowedTaxa, false, limitPlatform, seriesTypes );
            if ( records.isEmpty() ) {
                log.info( "Reached end of records while seeking." );
                return -1;
            }
            GeoRecord firstRecord = records.iterator().next();
            log.info( "Currently at " + firstRecord.getGeoAccession() + " released on " + firstRecord.getReleaseDate() );
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
                    log.info( "Located requested starting point of " + startFrom + ", resuming detailed queries at " + i + "." );
                    return i;
                } else if ( startDate != null && beforeOrEquals( geoRecord.getReleaseDate(), startDate ) ) {
                    log.info( "Located requested starting date of " + startDate + ", resuming detailed queries at " + i + "." );
                    return i;
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

    private Slice<GeoRecord> getGeoRecords( int start, int chunkSize, Collection<String> allowedTaxa, boolean fetchDetails, @Nullable Collection<String> limitPlatform, Collection<GeoSeriesType> seriesTypes ) throws IOException {
        return retryTemplate.execute( ( attempt, lastAttempt ) -> gbs.searchGeoRecords( null, null, allowedTaxa, limitPlatform, seriesTypes, start, chunkSize, fetchDetails ), "fetching GEO records" );
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

    private void writeDataset( GeoRecord geoRecord, boolean allPlatformsInGemma, boolean isAffymetrix, CSVPrinter os ) throws IOException {
        os.printRecord( geoRecord.getGeoAccession(), dateFormat.format( geoRecord.getReleaseDate() ),
                StringUtils.join( geoRecord.getOrganisms(), "," ), geoRecord.getPlatform(), allPlatformsInGemma,
                isAffymetrix, geoRecord.getNumSamples(), geoRecord.getSeriesType(), geoRecord.isSuperSeries(),
                geoRecord.getSubSeriesOf(), StringUtils.join( geoRecord.getPubMedIds(), "," ), geoRecord.getTitle(), geoRecord.getSummary(),
                StringUtils.join( geoRecord.getMeshHeadings(), "," ), geoRecord.getSampleDetails(), geoRecord.getLibraryStrategy(),
                StringUtils.replace( geoRecord.getOverallDesign(), "\n", "\\n" ) );
    }
}
