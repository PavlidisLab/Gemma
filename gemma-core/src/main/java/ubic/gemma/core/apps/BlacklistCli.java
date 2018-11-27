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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.core.util.AbstractSpringAwareCLI;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.experiment.BlacklistedExperiment;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityDao;

/**
 * Add entries to the blacklist
 * 
 * @author paul
 */
public class BlacklistCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "blackList";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        super.addUserNameAndPasswordOptions( true );
        super.addOption( "file", "file", true,
                "Tab-delimitd file with blacklist. Format: first column is GEO accession; second column is reason for blacklist; optional "
                        + "additional columns: name, description of entity" );

    }

    @Override
    public String getShortDesc() {
        return "Add entities to the blacklist";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( "file" ) ) {
            this.fileName = this.getOptionValue( "file" );
        } else {
            throw new IllegalArgumentException( "Must provide an input file" );
        }
    }

    String fileName = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception ex = super.processCommandLine( args );
        if ( ex != null ) return ex;

        BlacklistedEntityDao blacklistedEntityDao = this.getBean( BlacklistedEntityDao.class );
        ExternalDatabaseDao externalDatabaseDao = this.getBean( ExternalDatabaseDao.class );

        ExternalDatabase geo = externalDatabaseDao.findByName( "geo" );

        if ( geo == null ) throw new IllegalStateException( "GEO not found as an external database in the system" );

        try (BufferedReader in = new BufferedReader( new FileReader( fileName ) )) {
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

                if ( blacklistedEntityDao.isBlacklisted( accession ) ) {
                    log.warn( accession + " is already blacklisted, skipping" );
                    continue;
                }

                String reason = split[1];
                if ( accession.startsWith( "GPL" ) ) {
                    blee = new BlacklistedPlatform();
                } else if ( accession.startsWith( "GSE" ) ) {
                    blee = new BlacklistedExperiment();
                } else {
                    throw new IllegalArgumentException(
                            "Unrecognized ID class: " + accession + "; was expecting something starting with GPL or GSE" );

                }
                blee.setShortName( accession );
                blee.setReason( reason );

                DatabaseEntry d = DatabaseEntry.Factory.newInstance( accession, null, null, geo );
                blee.setExternalAccession( d );

                if ( split.length > 2 ) {
                    blee.setName( split[2] );
                }
                if ( split.length > 3 ) {
                    blee.setDescription( split[3] );
                }

                blacklistedEntityDao.create( blee );

                log.info( "Blacklisted " + accession );
            }

        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        BlacklistCli e = new BlacklistCli();
        Exception ex = e.doWork( args );
        if ( ex != null ) {
            log.fatal( ex, ex );
        }

    }

}
