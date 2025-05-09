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

package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * Refreshes the information in all the bibliographic references in the system.
 *
 * @author Paul
 */
public class BibRefUpdaterCli extends AbstractAuthenticatedCLI {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    private final Random random = new Random();

    private String[] pmids;

    public BibRefUpdaterCli() {
        setRequireLogin();
    }

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
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "pmids" ).longOpt( null ).desc( "Pubmed ids, comma-delimited; default is to do all in DB" ).argName( "ids" ).hasArg().build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        pmids = StringUtils.split( commandLine.getOptionValue( "pmids" ), "," );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Collection<Long> bibrefIds = new ArrayList<>();
        if ( this.pmids != null ) {
            for ( String s : pmids ) {

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
            Thread.sleep( random.nextInt( 1000 ) );
        }
    }

}
