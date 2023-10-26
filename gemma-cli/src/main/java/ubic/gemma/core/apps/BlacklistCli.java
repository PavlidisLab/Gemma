/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.experiment.BlacklistedExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Add entries to the blacklist
 *
 * @author paul
 */
public class BlacklistCli extends AbstractAuthenticatedCLI {

    private static final int MAX_RETRIES = 3;
    String fileName = null;
    private boolean remove = false;
    private boolean proactive = false;
    private Collection<String> platformsToScreen;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.METADATA;
    }

    @Override
    public String getCommandName() {
        return "blackList";
    }

    @Override
    public String getShortDesc() {
        return "Add GEO entities (series or platforms) to the blacklist";
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "file" ).longOpt( null )
                .desc( "Tab-delimited file with blacklist. Format: first column is GEO accession; second column is reason for blacklist; optional "
                        + "additional columns: name, description of entity; lines starting with '#' are ignored" )
                .argName( "file name" ).hasArg().build() );
        options.addOption( "undo", "Remove items from blacklist instead of adding. File can contain just one column of IDs" );
        options.addOption( "pp",
                "Special: proactively blacklist GEO datasets for blacklisted platforms (cannot be combined with other options except -a)" );
        options.addOption( "a", true, "A comma-delimited of GPL IDs to check. Combine with -pp, not relevant to any other option" );
    }

    @Override
    protected boolean requireLogin() {
        return true;
    }

    @Override
    protected void doWork() throws Exception {
        BlacklistedEntityService blacklistedEntityService = this.getBean( BlacklistedEntityService.class );
        ExternalDatabaseService externalDatabaseService = this.getBean( ExternalDatabaseService.class );

        ExternalDatabase geo = externalDatabaseService.findByName( "GEO" );

        if ( geo == null )
            throw new IllegalStateException( "GEO not found as an external database in the system" );

        if ( proactive ) {
            log.info( "Searching GEO for experiments to blacklist based on their use of blacklisted platforms" );
            proactivelyBlacklistExperiments( geo );
            return;
        }

        try ( BufferedReader in = new BufferedReader( new FileReader( fileName ) ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() )
                    continue;

                String[] split = StringUtils.split( line, "\t" );

                if ( split.length < 2 && !remove ) {
                    throw new IllegalArgumentException( "Not enough fields, expected at least 2 tab-delimited" );
                }

                BlacklistedEntity blee;

                String accession = split[0];

                boolean alreadyBlacklisted = blacklistedEntityService.isBlacklisted( accession );
                if ( remove ) {
                    if ( !alreadyBlacklisted ) {
                        log.warn( "Attempting to de-blacklist " + accession + " but it is not blacklisted" );
                        continue;
                    }
                    blacklistedEntityService.remove( blacklistedEntityService.findByAccession( accession ) );
                    log.info( "De-blacklisted " + accession );
                    continue;
                } else if ( alreadyBlacklisted ) {
                    log.warn( accession + " is already blacklisted, skipping. To update the 'reason' please unblacklist it and blacklist again" );
                    continue;
                }

                if ( accession.startsWith( "GPL" ) ) {
                    blee = new BlacklistedPlatform();
                } else if ( accession.startsWith( "GSE" ) ) {
                    blee = new BlacklistedExperiment();
                } else {
                    throw new IllegalArgumentException( "Unrecognized ID class: " + accession
                            + "; was expecting something starting with GPL or GSE" );
                }

                String reason = split[1];
                if ( StringUtils.isBlank( reason ) ) {
                    throw new IllegalArgumentException( "A reason for blacklisting must be provided for " + accession );
                }

                if ( blacklistedEntityService.findByAccession( accession ) != null ) {
                    log.warn( accession + " is already on the blacklist, skipping" );
                    continue;
                }

                blee.setShortName( accession );
                blee.setReason( reason );

                DatabaseEntry d = DatabaseEntry.Factory.newInstance( accession, null, null, geo );
                blee.setExternalAccession( d );

                /*
                 * Remember that the entity will not be in the system, so having the name and description here might be
                 * useful.
                 */
                if ( split.length > 2 ) {
                    blee.setName( split[2] );
                }
                if ( split.length > 3 ) {
                    blee.setDescription( split[3] );
                }

                blacklistedEntityService.create( blee );

                log.info( "Blacklisted " + accession );
            }

        } catch ( Exception e ) {
            throw e;
        }
    }

    /**
     *
     */
    private void proactivelyBlacklistExperiments( ExternalDatabase geo ) throws Exception {
        GeoBrowser gbs = new GeoBrowser();
        BlacklistedEntityService blacklistedEntityDao = this.getBean( BlacklistedEntityService.class );

        Collection<String> candidates = new ArrayList<>();
        int numChecked = 0;
        int numBlacklisted = 0;
        for ( BlacklistedEntity be : blacklistedEntityDao.loadAll() ) {
            if ( be instanceof BlacklistedPlatform ) {

                if ( platformsToScreen == null || !platformsToScreen.isEmpty()
                        || platformsToScreen.contains( be.getExternalAccession().getAccession() ) ) {
                    candidates.add( be.getExternalAccession().getAccession() );
                    numChecked++;
                }
            }

            if ( candidates.size() == 5 ) { // too many will break eutils query
                log.info( "Looking for batch of candidates using: " + StringUtils.join( candidates, "," ) );
                numBlacklisted += fetchAndBlacklist( geo, gbs, blacklistedEntityDao, candidates );
                candidates.clear();
            }
        }

        // finish the last batch
        fetchAndBlacklist( geo, gbs, blacklistedEntityDao, candidates );

        log.info( "Checked " + numChecked + " blacklisted platforms for experiment in GEO, blacklisted " + numBlacklisted + " GSEs" );

    }

    /**
     * @return number of actually blacklisted experiments in this batch.
     */
    private int fetchAndBlacklist( ExternalDatabase geo, GeoBrowser gbs, BlacklistedEntityService blacklistedEntityDao, Collection<String> candidates )
            throws InterruptedException {
        int start = 0;

        ExpressionExperimentService expressionExperimentService = this.getBean( ExpressionExperimentService.class );

        boolean keepGoing = true;
        int numBlacklisted = 0;
        int retries = 0;
        while ( keepGoing ) {

            // code copied from GeoGrabberCli
            List<GeoRecord> recs = null;

            try {
                recs = gbs.getGeoRecordsBySearchTerm( null, start, 100, false /* details */, null, candidates );
                retries = 0;
            } catch ( IOException e ) {
                // this definitely can happen, occasional 500s from NCBI
                retries++;
                if ( retries <= MAX_RETRIES ) {
                    log.warn( "Failure while fetching records, retrying " + e.getMessage() );
                    Thread.sleep( 500 );
                    continue; // try again
                }
                AbstractCLI.log.info( "Too many failures, giving up" );
                keepGoing = false;
            }

            if ( recs == null || recs.isEmpty() ) {
                keepGoing = false;
                break;
            }

            for ( GeoRecord geoRecord : recs ) {
                boolean skip = false;
                String eeAcc = geoRecord.getGeoAccession();
                if ( null != blacklistedEntityDao.findByAccession( eeAcc ) ) {
                    log.debug( "Already blacklisted: " + eeAcc );
                    continue;
                }

                String[] platforms = geoRecord.getPlatform().split( ";" );
                for ( String p : platforms ) {

                    BlacklistedEntity bli = blacklistedEntityDao.findByAccession( p );

                    if ( bli == null ) {
                        // then at least one platform it uses isn't blacklisted, we won't blacklist the experiment
                        skip = true;
                        break;
                    }

                }

                if ( skip ) {
                    continue;
                }

                Collection<ExpressionExperiment> ee = expressionExperimentService.findByAccession( eeAcc );
                if ( !ee.isEmpty() ) {
                    log.warn( "Warning:  " + eeAcc + " is in Gemma but will be blacklisted" );
                }

                log.info( "Blacklisting: " + eeAcc + " " + geoRecord.getTitle() + " (" + geoRecord.getPlatform() + ")" );
                BlacklistedEntity b = new BlacklistedExperiment();
                DatabaseEntry d = DatabaseEntry.Factory.newInstance( eeAcc, null, null, geo );
                b.setExternalAccession( d );
                b.setDescription( geoRecord.getTitle() );
                b.setReason( "Unsupported platform" );

                blacklistedEntityDao.create( b );
                numBlacklisted++;

            }

            start += 100;

        }
        return numBlacklisted;
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

        if ( commandLine.hasOption( "pp" ) ) {
            if ( this.remove || this.fileName != null ) {
                throw new IllegalArgumentException( "The pp option cannot be combined with others" );
            }
            this.proactive = true;

            if ( commandLine.hasOption( "a" ) ) {
                this.platformsToScreen = Arrays.asList( StringUtils.split( commandLine.getOptionValue( "a" ) ) );
            }

            return;
        }

        if ( commandLine.hasOption( "file" ) ) {
            this.fileName = commandLine.getOptionValue( "file" );
        } else {
            throw new IllegalArgumentException( "Must provide an input file" );
        }

        if ( commandLine.hasOption( "undo" ) ) {
            this.remove = true;
        }

    }

}
