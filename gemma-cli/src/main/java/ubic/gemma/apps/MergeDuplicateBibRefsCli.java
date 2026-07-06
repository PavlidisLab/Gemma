/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceReadService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Merge duplicate {@link BibliographicReference} rows that share a PubMed accession.
 * <p>
 * Prod has accessions with 2-3 duplicate bibref rows (a data issue that 500'd
 * {@code PUT /datasets/{id}/publications} until the read-side lookups were made duplicate-tolerant). For
 * each targeted PubMed id this CLI keeps the <strong>lowest-id (oldest, canonical)</strong> row, repoints
 * every experiment that references a duplicate (its {@code primaryPublication} and its
 * {@code otherRelevantPublications}) onto the canonical row, and then deletes the now-unreferenced
 * duplicate.
 * <p>
 * Safety: it is a <strong>dry run by default</strong> — it reports exactly what it would do and changes
 * nothing unless {@code -commit} is given. Even with {@code -commit}, a duplicate is deleted only after its
 * experiment references are repointed and a re-check finds no experiment still pointing at it; if the
 * delete fails because something outside the experiment surface still holds the row (e.g. a
 * {@code GeneSet.literatureSources}), it is left in place and logged rather than forced. Nothing is
 * merged that would lose a reference.
 *
 * @author gemma
 */
public class MergeDuplicateBibRefsCli extends AbstractAuthenticatedCLI {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private BibliographicReferenceReadService bibliographicReferenceReadService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    private String[] pubMedIds;
    private boolean commit = false;

    @Override
    public String getCommandName() {
        return "mergeDuplicateBibRefs";
    }

    @Override
    public String getShortDesc() {
        return "Merge duplicate BibliographicReference rows (same PubMed accession) into the lowest-id row, "
                + "repointing experiments; dry-run unless -commit.";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "pmids" ).hasArg().required().argName( "pmids" )
                .desc( "Comma-separated PubMed IDs to de-duplicate (e.g. the accessions flagged as duplicated)." )
                .build() );
        options.addOption( Option.builder( "commit" )
                .desc( "Actually apply the merge. Without this the CLI only reports what it would do (dry run)." )
                .build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        this.pubMedIds = commandLine.getOptionValue( "pmids" ).split( "," );
        this.commit = commandLine.hasOption( "commit" );
        if ( this.commit ) {
            // -commit repoints experiments and deletes bibrefs — both GROUP_ADMIN-only.
            // Require authentication up front so an unauthenticated (anonymous) run fails
            // fast with a clear message instead of silently no-opping when the secured
            // writes are denied. Dry runs may still be executed anonymously.
            setRequireLogin();
        }
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        log.info( ( commit ? "COMMIT" : "DRY-RUN" ) + " merge of duplicate bibrefs for " + pubMedIds.length + " PubMed id(s)." );

        int groupsWithDuplicates = 0, referencesRepointed = 0, duplicatesDeleted = 0, duplicatesSkipped = 0;

        for ( String raw : pubMedIds ) {
            String pmid = raw.trim();
            if ( pmid.isEmpty() ) {
                continue;
            }
            List<BibliographicReference> rows = bibliographicReferenceReadService.findAllByExternalId( pmid, ExternalDatabases.PUBMED );
            if ( rows.size() <= 1 ) {
                log.info( "PMID " + pmid + ": " + rows.size() + " row(s) — no duplicates, nothing to do." );
                continue;
            }
            groupsWithDuplicates++;
            BibliographicReference canonical = rows.get( 0 ); // lowest id
            List<BibliographicReference> duplicates = rows.subList( 1, rows.size() );
            log.info( "PMID " + pmid + ": " + rows.size() + " rows; keeping canonical id=" + canonical.getId()
                    + ", merging duplicates=" + duplicates.stream().map( BibliographicReference::getId ).collect( Collectors.toList() ) );

            for ( BibliographicReference dup : duplicates ) {
                Collection<ExpressionExperiment> ees = expressionExperimentService.findByBibliographicReference( dup );
                for ( ExpressionExperiment lite : ees ) {
                    ExpressionExperiment ee = expressionExperimentService.loadWithPrimaryPublicationAndOtherRelevantPublications( lite.getId() );
                    if ( ee == null ) {
                        continue;
                    }
                    boolean touched = false;
                    if ( ee.getPrimaryPublication() != null && dup.getId().equals( ee.getPrimaryPublication().getId() ) ) {
                        log.info( "  " + ee.getShortName() + ": primaryPublication " + dup.getId() + " -> " + canonical.getId() );
                        if ( commit ) {
                            ee.setPrimaryPublication( canonical );
                        }
                        touched = true;
                    }
                    if ( ee.getOtherRelevantPublications().stream().anyMatch( r -> dup.getId().equals( r.getId() ) ) ) {
                        log.info( "  " + ee.getShortName() + ": otherRelevantPublications " + dup.getId() + " -> " + canonical.getId() );
                        if ( commit ) {
                            ee.getOtherRelevantPublications().removeIf( r -> dup.getId().equals( r.getId() ) );
                            ee.getOtherRelevantPublications().add( canonical );
                        }
                        touched = true;
                    }
                    if ( touched ) {
                        referencesRepointed++;
                        if ( commit ) {
                            expressionExperimentService.update( ee );
                        }
                    }
                }

                if ( !commit ) {
                    log.info( "  would delete duplicate bibref id=" + dup.getId() + " (after repointing its experiments)." );
                    continue;
                }
                // Only delete once nothing on the experiment surface still points at the duplicate.
                Collection<ExpressionExperiment> remaining = expressionExperimentService.findByBibliographicReference( dup );
                if ( !remaining.isEmpty() ) {
                    log.warn( "  duplicate bibref id=" + dup.getId() + " still referenced by " + remaining.size()
                            + " experiment(s) after repoint — NOT deleting." );
                    duplicatesSkipped++;
                    continue;
                }
                try {
                    bibliographicReferenceService.remove( dup );
                    duplicatesDeleted++;
                    log.info( "  deleted duplicate bibref id=" + dup.getId() + "." );
                } catch ( Exception e ) {
                    duplicatesSkipped++;
                    log.warn( "  could not delete duplicate bibref id=" + dup.getId()
                            + " — likely referenced outside experiments (e.g. a gene set). Left in place. " + e.getMessage() );
                }
            }
        }

        log.info( "Summary: groupsWithDuplicates=" + groupsWithDuplicates + ", experimentReferencesRepointed="
                + referencesRepointed + ", duplicatesDeleted=" + duplicatesDeleted + ", duplicatesSkipped="
                + duplicatesSkipped + ( commit ? "." : " (DRY RUN — nothing was changed)." ) );
    }
}
