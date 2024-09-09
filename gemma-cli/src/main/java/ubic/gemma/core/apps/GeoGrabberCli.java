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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Scans GEO for experiments that are not in Gemma, subject to some filtering criteria, outputs to a file for further
 * screening. See <a href="https://github.com/PavlidisLab/Gemma/issues/169">#160</a>
 *
 * @author paul
 */
public class GeoGrabberCli extends AbstractAuthenticatedCLI {

    private static final String
            ACCESSION_OPTION = "accession",
            ACCESSION_FILE_OPTION = "accessionFile";

    private enum Mode {
        GET_PLATFORMS,
        GET_DATASETS,
        BROWSE_DATASETS
    }

    private static final int NCBI_CHUNK_SIZE = 100;
    private static final int MAX_RETRIES = 5; // on failures
    private static final int MAX_EMPTY_CHUNKS_IN_A_ROW = 50; // stop condition when we stop seeing useful records
    private static final DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH );

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

    @Autowired
    private ExpressionExperimentService ees;
    @Autowired
    private TaxonService ts;
    @Autowired
    private ArrayDesignService ads;

    private final GeoBrowser gbs = new GeoBrowser();

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
        options.addOption(
                Option.builder( "platforms" ).desc( "Fetch a list of all platforms instead of experiments (-startdate, -date, -startat and -gselimit are ignored)" ).build() );

        // for retrieving datasets
        options.addOption( Option.builder( ACCESSION_OPTION ).longOpt( "accession" ).hasArg().desc( "A comma-delimited list of accessions to retrieve from GEO" ).build() );
        options.addOption( Option.builder( ACCESSION_FILE_OPTION ).longOpt( "accession-file" ).hasArg().desc( "A file containing accessions to retrieve from GEO" ).type( Path.class ).build() );

        // for browsing datasets
        options.addOption(
                Option.builder( "date" ).longOpt( null ).desc( "A release date to stop the search in format yyyy.MM.dd e.g. 2010.01.12" )
                        .argName( "date limit" ).hasArg().build() );
        options.addOption(
                Option.builder( "gselimit" ).longOpt( null ).desc( "A GSE at which to stop the search" ).argName( "GSE identifier" ).hasArg()
                        .build() );
        options.addOption( Option.builder( "platformLimit" ).longOpt( null ).desc( "Limit to selected platforms" )
                .argName( "comma-delimited list of GPLs" ).hasArg().build() );

        options.addOption(
                Option.builder( "startat" ).hasArg().argName( "GSE ID" ).desc( "Attempt to 'fast-rewind' to the given GSE ID and continue retreiving from there " ).build() );

        options.addOption(
                Option.builder( "startdate" ).hasArg().argName( "date" ).desc( "Attempt to 'fast-rewind' to the given date (yyyy.MM.dd) " +
                        "and continue retreiving from there " ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws org.apache.commons.cli.ParseException {
        if ( commandLine.hasOption( "platforms" ) ) {
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
                this.dateLimit = DateUtils.parseDate( commandLine.getOptionValue( "date" ), new String[] { "yyyy.MM.dd" } );
            } catch ( ParseException e ) {
                throw new IllegalArgumentException( "Could not parse date " + commandLine.getOptionValue( "date" ) );
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
                this.startDate = DateUtils.parseDate( commandLine.getOptionValue( "startdate" ), "yyyy.MM.dd" );
            } catch ( ParseException e ) {
                throw new IllegalArgumentException( "Could not parse date " + commandLine.getOptionValue( "startdate" ) );
            }
            if ( dateLimit != null ) {
                if ( dateLimit.after( startDate ) ) {
                    throw new IllegalArgumentException( "startdate has to be later than the -date option" );
                }
            }
        }
    }

    @Override
    protected void doWork() throws Exception {
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
                browseDatasets( startFrom, gseLimit, startDate, dateLimit, limitPlatform );
                break;
            default:
                throw new IllegalStateException( "Unknown mode " + mode );
        }
    }

    private void getPlatforms() throws IOException {
        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( "Acc", "ReleaseDate", "Taxa", "Title", "Summary", "TechType" )
                .build();
        Collection<GeoRecord> allGEOPlatforms = gbs.getAllGEOPlatforms();
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
            for ( GeoRecord record : gbs.getGeoRecords( accessions ) ) {
                writeDataset( record, areAllPlatformsInGemma( record, seenPlatforms ), isAffymetrix( record, seenPlatforms ), os );
            }
        }
    }

    private void browseDatasets( @Nullable String startFrom, @Nullable String gseLimit, @Nullable Date startDate, @Nullable Date dateLimit, @Nullable Set<String> limitPlatform ) throws IOException, InterruptedException {
        Set<String> seen = new HashSet<>();

        Map<String, ArrayDesign> seenArrayDesigns = new HashMap<>();

        Collection<String> allowedTaxa = new HashSet<>();
        for ( Taxon t : ts.loadAll() ) {
            allowedTaxa.add( t.getScientificName() );
        }
        log.info( allowedTaxa.size() + " Taxa considered usable" );

        CSVFormat tsvFormat = CSVFormat.TDF.builder()
                .setHeader( DATASET_HEADER )
                .build();
        try ( CSVPrinter os = tsvFormat.print( getOutputWriter() ) ) {
            int start = 0;
            int numProcessed = 0;
            int numUsed = 0;
            boolean keepGoing = true;

            int retries = 0;
            int numSkippedChunks = 0;
            boolean reachedRewindPoint = ( startDate == null && startFrom == null );
            GeoRecord lastValidRecord = null;
            while ( keepGoing ) {
                log.debug( "Searching from " + start + ", seeking " + NCBI_CHUNK_SIZE + " records" );
                List<GeoRecord> recs;

                try {

                    if ( !reachedRewindPoint ) {
                        // skip details in queries to go faster
                        if ( startDate != null ) {
                            log.info( "Seeking rewind point " + startDate + " from record " + start + " ..." );
                        } else if ( startFrom != null ) {
                            log.info( "Seeking rewind point " + startFrom + " from record " + start + " ..." );
                        }
                        recs = gbs.getGeoRecordsBySearchTerm( null, start, NCBI_CHUNK_SIZE, false /* details */, allowedTaxa, limitPlatform );
                    } else {
                        recs = gbs.getGeoRecordsBySearchTerm( null, start, NCBI_CHUNK_SIZE, true /* details */, allowedTaxa, limitPlatform );
                    }

                    if ( recs == null || recs.isEmpty() ) {
                        // When this happens, the issue is that we filtered out all the results. So we should just ignore and keep going.
                        AbstractCLI.log.info( "No records received for start=" + start + ", advancing" );
                        numSkippedChunks++;

                        // repeated empty results can just mean we ran out of records.
                        if ( numSkippedChunks > MAX_EMPTY_CHUNKS_IN_A_ROW ) {
                            if ( lastValidRecord != null && lastValidRecord.getReleaseDate().before( new Date( 2007, Calendar.JANUARY, 1 ) ) ) {
                                log.info( "Seem to have hit end of records, bailing" );
                                break;
                            } else {
                                // no op fo rnow.
                            }
                        }
                        start += NCBI_CHUNK_SIZE;
                        continue;
                    }
                } catch ( IOException e ) {
                    // this definitely can happen, occasional 500s from NCBI
                    retries++;
                    if ( retries <= MAX_RETRIES ) {
                        log.warn( "Failure while fetching records, retrying " + e.getMessage() );
                        Thread.sleep( 500L * retries );
                        continue;
                    }
                    throw new IOException( "Too many failures: " + e.getMessage() );

                }

                retries = 0;
                numSkippedChunks = 0;

                log.debug( "Retrieved " + recs.size() ); // we skip ones that are not using taxa of interest
                start += NCBI_CHUNK_SIZE; // this seems the best way to avoid hitting them more than once.

                for ( GeoRecord geoRecord : recs ) {

                    log.debug( geoRecord.getGeoAccession() );

                    /*
                    If we are trying to fast-rewind, we need to reset and redo the last query while fetching details.
                     */
                    if ( !reachedRewindPoint ) {
                        if ( geoRecord.getGeoAccession().equals( startFrom ) ) {
                            log.info( "Located requested starting point of " + startFrom + ", resuming detailed queries" );
                            reachedRewindPoint = true;
                            start = Math.max( 0, start - NCBI_CHUNK_SIZE );
                            break;
                        } else if ( startDate != null && geoRecord.getReleaseDate().before( startDate ) ) {
                            log.info( "Located requested starting date of " + startDate + ", resuming detailed queries" );
                            reachedRewindPoint = true;
                            start = Math.max( 0, start - NCBI_CHUNK_SIZE );
                            break;
                        } else {
                            continue;
                        }
                    }

                    numProcessed++;

                    if ( numProcessed % 50 == 0 ) {
                        log.info( "Processed " + numProcessed + " GEO records, retained " + numUsed + " so far" );
                    }

                    if ( dateLimit != null && dateLimit.after( geoRecord.getReleaseDate() ) ) {
                        log.info( "Stopping as reached date limit" );
                        keepGoing = false;
                        break;
                    }

                    if ( geoRecord.getGeoAccession().equals( gseLimit ) ) {
                        log.info( "Stopping as have reached " + gseLimit );
                        keepGoing = false;
                        break;
                    }

                    if ( seen.contains( geoRecord.getGeoAccession() ) ) {
                        log.info( "Already saw " + geoRecord.getGeoAccession() ); // this would be a bug IMO, want to avoid!
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

                    boolean allPlatformsInGemma = areAllPlatformsInGemma( geoRecord, seenArrayDesigns );
                    boolean anyNonBlacklistedPlatforms = hasBlacklistedPlatform( geoRecord );
                    boolean isAffymetrix = isAffymetrix( geoRecord, seenArrayDesigns );

                    // we skip if all the platforms for the GSE are blacklisted
                    if ( !anyNonBlacklistedPlatforms ) {
                        continue;
                    }

                    writeDataset( geoRecord, allPlatformsInGemma, isAffymetrix, os );

                    seen.add( geoRecord.getGeoAccession() );

                    numUsed++;
                    lastValidRecord = geoRecord;
                }
                os.flush();

            }
        }
    }

    private Appendable getOutputWriter() throws IOException {
        if ( outputFileName != null ) {
            log.info( "Writing output to " + outputFileName );
            if ( Files.exists( outputFileName ) ) {
                log.warn( "Overwriting existing file..." );
            }
            return Files.newBufferedWriter( outputFileName );
        } else {
            return System.out;
        }
    }

    /**
     * Check if all the platforms referenced in a GEO record are present in Gemma.
     */
    private boolean areAllPlatformsInGemma( GeoRecord geoRecord, Map<String, ArrayDesign> seenPlatforms ) {
        String[] platforms = geoRecord.getPlatform().split( ";" );
        for ( String p : platforms ) {
            ArrayDesign ad = seenPlatforms.computeIfAbsent( p, shortName -> {
                ArrayDesign found = ads.findByShortName( shortName );
                if ( found != null ) {
                    found = ads.thawLite( found );
                }
                return found;
            } );
            if ( ad == null ) {
                return false; // don't skip, just indicate
            }
        }
        return true;
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

    private boolean hasBlacklistedPlatform( GeoRecord geoRecord ) {
        String[] platforms = geoRecord.getPlatform().split( ";" );
        for ( String p : platforms ) {
            if ( !ees.isBlackListed( p ) ) {
                return true;
            }
        }
        return false;
    }

    private void writeDataset( GeoRecord geoRecord, boolean allPlatformsInGemma, boolean isAffymetrix, CSVPrinter os ) throws IOException {
        os.printRecord( geoRecord.getGeoAccession(), dateFormat.format( geoRecord.getReleaseDate() ),
                StringUtils.join( geoRecord.getOrganisms(), "," ), geoRecord.getPlatform(), allPlatformsInGemma,
                isAffymetrix, geoRecord.getNumSamples(), geoRecord.getSeriesType(), geoRecord.isSuperSeries(),
                geoRecord.getSubSeriesOf(), geoRecord.getPubMedIds(), geoRecord.getTitle(), geoRecord.getSummary(),
                geoRecord.getMeshHeadings(), geoRecord.getSampleDetails(), geoRecord.getLibraryStrategy(), geoRecord.getOverallDesign() );
    }
}
