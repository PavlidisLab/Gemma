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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.experiment.BlacklistedExperiment;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityDao;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Add entries to the blacklist
 *
 * @author paul
 */
public class BlacklistCli extends AbstractCLIContextCLI {

    String fileName = null;
    private boolean remove = false;

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
    protected void buildOptions() {
        super.addOption( "file", null,
                "Tab-delimited file with blacklist. Format: first column is GEO accession; second column is reason for blacklist; optional "
                        + "additional columns: name, description of entity",
                "file name" );
        super.addOption( "undo", "Remove items from blacklist instead of adding" );

    }

    @Override
    protected boolean requireLogin() {
        return true;
    }

    @Override
    protected void doWork() throws Exception {
        BlacklistedEntityDao blacklistedEntityDao = this.getBean( BlacklistedEntityDao.class );
        ExternalDatabaseDao externalDatabaseDao = this.getBean( ExternalDatabaseDao.class );

        ExternalDatabase geo = externalDatabaseDao.findByName( "GEO" );

        if ( geo == null )
            throw new IllegalStateException( "GEO not found as an external database in the system" );

        try ( BufferedReader in = new BufferedReader( new FileReader( fileName ) ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() )
                    continue;

                String[] split = StringUtils.split( line, "\t" );

                if ( split.length < 2 ) {
                    throw new IllegalArgumentException( "Not enough fields, expected at least 2 tab-delimited" );
                }

                BlacklistedEntity blee;

                String accession = split[0];

                boolean alreadyBlacklisted = blacklistedEntityDao.isBlacklisted( accession );
                if ( remove ) {
                    blacklistedEntityDao.remove( blacklistedEntityDao.findByAccession( accession ) );
                    log.info( "De-blacklisted " + accession );
                    continue;
                } else if ( alreadyBlacklisted ) {
                    log.warn( accession + " is already blacklisted, skipping" );
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

                if ( blacklistedEntityDao.findByAccession( accession ) != null ) {
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

                blacklistedEntityDao.create( blee );

                log.info( "Blacklisted " + accession );
            }

        } catch ( Exception e ) {
            throw e;
        }
    }

    @Override
    protected void processOptions() {
        if ( this.hasOption( "file" ) ) {
            this.fileName = this.getOptionValue( "file" );
        } else {
            throw new IllegalArgumentException( "Must provide an input file" );
        }

        if ( this.hasOption( "undo" ) ) {
            this.remove = true;
        }
    }

}
