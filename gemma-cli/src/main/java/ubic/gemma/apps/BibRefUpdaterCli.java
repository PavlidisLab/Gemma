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
import java.util.List;
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
        int refreshed = 0, failed = 0, missing = 0;
        // `retracted` is the one publication attribute that changes AFTER ingestion — a paper is
        // ingested clean and retracted years later — and because the column defaults to false,
        // "never re-checked" and "checked, not retracted" read identically. So the interesting
        // output of this command is not that it ran, but which records CHANGED. Collect those:
        // otherwise the answer is buried in one log line per reference.
        List<String> newlyRetracted = new ArrayList<>();
        for ( Long id : bibrefIds ) {
            BibliographicReference bibref = bibliographicReferenceService.load( id );
            if ( bibref == null ) {
                log.info( "No reference with id=" + id );
                missing++;
                continue;
            }
            bibref = bibliographicReferenceService.thaw( bibref );
            String accession = bibref.getPubAccession().getAccession();
            boolean wasRetracted = Boolean.TRUE.equals( bibref.getRetracted() );
            try {
                BibliographicReference updated = bibliographicReferenceService.refresh( accession );
                log.info( updated );
                refreshed++;
                if ( !wasRetracted && updated != null && Boolean.TRUE.equals( updated.getRetracted() ) ) {
                    newlyRetracted.add( accession );
                    log.warn( "PMID " + accession + " is now flagged retracted (was not): " + updated );
                }
            } catch ( Exception e ) {
                log.info( "Failed to update: " + bibref + " (" + e.getMessage() + ")" );
                failed++;
            }
            Thread.sleep( random.nextInt( 1000 ) );
        }
        log.info( "updatePubMeds finished: " + refreshed + " refreshed, " + failed + " failed, "
                + missing + " missing, " + newlyRetracted.size() + " newly retracted." );
        if ( !newlyRetracted.isEmpty() ) {
            log.warn( "Newly retracted PMIDs (" + newlyRetracted.size() + "): "
                    + String.join( ",", newlyRetracted ) );
        }
    }

}
