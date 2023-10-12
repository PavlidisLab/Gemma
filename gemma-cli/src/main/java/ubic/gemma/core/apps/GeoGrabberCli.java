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
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Scans GEO for experiments that are not in Gemma, subject to some filtering criteria, outputs to a file for further
 * screening. See https://github.com/PavlidisLab/Gemma/issues/169
 *
 * @author paul
 */
public class GeoGrabberCli extends AbstractAuthenticatedCLI {

    private static final int NCBI_CHUNK_SIZE = 100;
    private static final int MAX_RETRIES = 5; // on failures
    private static final int MAX_EMPTY_CHUNKS_IN_A_ROW = 20; // stop condition when we stop seeing useful records
    private Date dateLimit;
    private String gseLimit;
    private String outputFileName = "";
    private boolean getPlatforms = false;
    private Collection<String> limitPlatform = new ArrayList<>();
    private String startFrom = null;
    private Date startDate = null;

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
        options.addOption(
                Option.builder( "date" ).longOpt( null ).desc( "A release date to stop the search in format yyyy.MM.dd e.g. 2010.01.12" )
                        .argName( "date limit" ).hasArg().build() );
        options.addOption(
                Option.builder( "gselimit" ).longOpt( null ).desc( "A GSE at which to stop the search" ).argName( "GSE identifier" ).hasArg()
                        .build() );
        options.addOption( Option.builder( "platformLimit" ).longOpt( null ).desc( "Limit to selected platforms" )
                .argName( "comma-delimited list of GPLs" ).hasArg().build() );

        options.addOption( Option.builder( "output" ).desc( "File path for output (required)" ).argName( "path" ).hasArg().required().build() );

        options.addOption(
                Option.builder( "platforms" ).desc( "Fetch a list of all platforms instead of experiments (date and gselimit ignored)" ).build() );

        options.addOption(
                Option.builder( "startat" ).hasArg().argName( "GSE ID" ).desc( "Attempt to 'fast-rewind' to the given GSE ID and continue retreiving from there " ).build() );

        options.addOption(
                Option.builder( "startdate" ).hasArg().argName( "date" ).desc( "Attempt to 'fast-rewind' to the given date (yyyy.MM.dd) " +
                        "and continue retreiving from there " ).build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

        if ( !commandLine.hasOption( "output" ) ) {
            throw new IllegalArgumentException( "You must provide an output file name" );
        }

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


        if ( commandLine.hasOption( "platforms" ) ) {
            this.getPlatforms = true;
        }

        if ( commandLine.hasOption( "platformLimit" ) ) {
            //      this.limitPlatform  = AbstractCLIContextCLI.readListFileToStrings( commandLine.getOptionValue( "platformLimit" ) );
            String gpls = commandLine.getOptionValue( "platformLimit" );
            this.limitPlatform = Arrays.asList( StringUtils.split( gpls, "," ) );
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
                this.startDate = DateUtils.parseDate( commandLine.getOptionValue( "startdate" ), new String[] { "yyyy.MM.dd" } );
            } catch ( ParseException e ) {
                throw new IllegalArgumentException( "Could not parse date " + commandLine.getOptionValue( "startdate" ) );
            }
            if ( dateLimit != null ) {
                if ( dateLimit.after( startDate ) ) {
                    throw new IllegalArgumentException( "startdate has to be later than the -date option" );
                }
            }
        }

        this.outputFileName = commandLine.getOptionValue( "output" );

    }

    @Override
    protected void doWork() throws Exception {
        Set<String> seen = new HashSet<>();
        GeoBrowser gbs = new GeoBrowser();
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        TaxonService ts = this.getBean( TaxonService.class );
        ArrayDesignService ads = this.getBean( ArrayDesignService.class );
        DateFormat dateFormat = new SimpleDateFormat( "yyyy.MM.dd", Locale.ENGLISH );

        int start = 0;

        assert outputFileName != null;
        File outputFile = new File( outputFileName );
        log.info( "Writing output to " + outputFile.getPath() );

        if ( outputFile.exists() ) {
            log.warn( "Overwriting existing file ..." );
            Thread.sleep( 500 );
        }

        outputFile.createNewFile();

        if ( getPlatforms ) {
            Collection<GeoRecord> allGEOPlatforms = gbs.getAllGEOPlatforms();
            log.info( "Fetched " + allGEOPlatforms.size() + " records" );
            try ( Writer os = new FileWriter( outputFile ) ) {
                os.append( "Acc\tRelaseDate\tTaxa\tTitle\tSummary\tTechType\n" );
                for ( GeoRecord geoRecord : allGEOPlatforms ) {

                    os.write(
                            geoRecord.getGeoAccession()
                                    + "\t" + dateFormat.format( geoRecord.getReleaseDate() )
                                    + "\t" + StringUtils.join( geoRecord.getOrganisms(), "," )
                                    + "\t" + geoRecord.getTitle()
                                    + "\t" + geoRecord.getSummary()
                                    + "\t" + geoRecord.getSeriesType()
                                    + "\n" );

                }
            }
            return;
        }

        Map<Long, ArrayDesign> seenArrayDesigns = new HashMap<>();

        try ( Writer os = new FileWriter( outputFile ) ) {

            os.append( "Acc\tReleaseDate\tTaxa\tPlatforms\tAllPlatformsInGemma\tAffy\tNumSamples\tType\tSuperSeries\tSubSeriesOf"
                    + "\tPubMed\tTitle\tSummary\tMeSH\tSampleTerms\tLibraryStrategy\tOverallDesign\n" );
            os.flush();

            int numProcessed = 0;
            int numUsed = 0;
            boolean keepGoing = true;

            Collection<String> allowedTaxa = new HashSet<>();
            for ( Taxon t : ts.loadAll() ) {
                allowedTaxa.add( t.getScientificName() );
            }
            log.info( allowedTaxa.size() + " Taxa considered usable" );
            int retries = 0;
            int numSkippedChunks = 0;
            boolean reachedRewindPoint = ( startDate == null && startFrom == null );

            while ( keepGoing ) {

                log.debug( "Searching from " + start + ", seeking " + NCBI_CHUNK_SIZE + " records" );
                List<GeoRecord> recs = null;

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
                        if (numSkippedChunks > MAX_EMPTY_CHUNKS_IN_A_ROW ) {
                            log.info("Have already skipped " + numSkippedChunks + " chunks, still no records: bailing");
                            break;
                        }
                        start += NCBI_CHUNK_SIZE;
                        continue;
                    }
                } catch ( IOException e ) {
                    // this definitely can happen, occasional 500s from NCBI
                    retries++;
                    if ( retries <= MAX_RETRIES ) {
                        log.warn( "Failure while fetching records, retrying " + e.getMessage() );
                        Thread.sleep( 500 * retries );
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
                        if ( startFrom != null && geoRecord.getGeoAccession().equals( startFrom ) ) {
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
                        System.err.println( "Processed " + numProcessed + " GEO records, retained " + numUsed + " so far" );
                    }

                    if ( this.dateLimit != null && dateLimit.after( geoRecord.getReleaseDate() ) ) {
                        log.info( "Stopping as reached date limit" );
                        keepGoing = false;
                        break;
                    }

                    if ( this.gseLimit != null && geoRecord.getGeoAccession().equals( this.gseLimit ) ) {
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

                    boolean allPlatformsInGemma = true;
                    boolean anyNonBlacklistedPlatforms = false;
                    boolean isAffymetrix = false;
                    String[] platforms = geoRecord.getPlatform().split( ";" );
                    for ( String p : platforms ) {

                        ArrayDesign ad = ads.findByShortName( p );
                        if ( ad == null ) {
                            allPlatformsInGemma = false; // don't skip, just indicate
                            break;
                        }

                        if ( seenArrayDesigns.containsKey( ad.getId() ) ) {
                            ad = seenArrayDesigns.get( ad.getId() ); // cache
                        } else {
                            ad = ads.thawLite( ad );
                            seenArrayDesigns.put( ad.getId(), ad );
                        }
                        isAffymetrix = ad.getDesignProvider() != null && "Affymetrix".equals( ad.getDesignProvider().getName() );

                        if ( !ees.isBlackListed( p ) ) {
                            anyNonBlacklistedPlatforms = true;
                        }
                        // check for Affymetrix
                    }

                    // we skip if all the platforms for the GSE are blacklisted
                    if ( !anyNonBlacklistedPlatforms ) {
                        continue;
                    }

                    os.write(
                            geoRecord.getGeoAccession()
                                    + "\t" + dateFormat.format( geoRecord.getReleaseDate() )
                                    + "\t" + StringUtils.join( geoRecord.getOrganisms(), "," )
                                    + "\t" + geoRecord.getPlatform()
                                    + "\t" + allPlatformsInGemma
                                    + "\t" + isAffymetrix
                                    + "\t" + geoRecord.getNumSamples()
                                    + "\t" + geoRecord.getSeriesType()
                                    + "\t" + geoRecord.isSuperSeries()
                                    + "\t" + geoRecord.getSubSeriesOf()
                                    + "\t" + geoRecord.getPubMedIds()
                                    + "\t" + geoRecord.getTitle()
                                    + "\t" + geoRecord.getSummary()
                                    + "\t" + geoRecord.getMeshHeadings()
                                    + "\t" + geoRecord.getSampleDetails()
                                    + "\t" + geoRecord.getLibraryStrategy()
                                    + "\t" + geoRecord.getOverallDesign() + "\n" );

                    seen.add( geoRecord.getGeoAccession() );

                    os.flush();

                    numUsed++;
                }
                os.flush();

            }
        }

    }

}
