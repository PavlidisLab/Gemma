/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.description.BibliographicReference;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Refreshes the information in all the bibliographic references in the system.
 *
 * @author Paul
 */
public class BibRefUpdaterCli extends AbstractCLIContextCLI {

    @Override
    public String getCommandName() {
        return "updatePubMeds";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.METADATA;
    }

    @Override
    public String getShortDesc() {
        return ( "Refresh stored information on publications" );
    }

    @Override
    protected void buildOptions() {
        super.addUserNameAndPasswordOptions( true );
        super.addOption( "pmids", null, "Pubmed ids, comma-delimited; default is to do all in DB", "ids" );
    }

    @Override
    protected void doWork() throws Exception {
        BibliographicReferenceService bibliographicReferenceService = this
                .getBean( BibliographicReferenceService.class );

        Collection<Long> bibrefIds = new ArrayList<>();
        if ( this.hasOption( "pmids" ) ) {
            for ( String s : StringUtils.split( this.getOptionValue( "pmids" ), "," ) ) {

                BibliographicReference found = bibliographicReferenceService.findByExternalId( s );
                if ( found == null ) {
                    log.warn( "Did not find " + s );
                    continue;
                }
                bibrefIds.add( found.getId() );

            }

        } else {
            log.info( "Updating all bibrefs in the system ..." );
            bibrefIds = bibliographicReferenceService.listAll();
        }
        log.info( "There are " + bibrefIds.size() + " to update" );
        for ( Long id : bibrefIds ) {
            BibliographicReference bibref = bibliographicReferenceService.load( id );
            if ( bibref == null ) {
                log.info( "No reference with id=" + id );
                continue;
            }
            bibref = bibliographicReferenceService.thaw( bibref );
            try {
                BibliographicReference updated = bibliographicReferenceService.refresh( bibref.getPubAccession()
                        .getAccession() );
                log.info( updated );
            } catch ( Exception e ) {
                log.info( "Failed to update: " + bibref + " (" + e.getMessage() + ")" );
            }
            Thread.sleep( RandomUtils.nextInt( 1000 ) );
        }
    }

}
