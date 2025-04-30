/*
 * The Gemma project
 *
 * Copyright (c) 2021 University of British Columbia
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
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowserImpl;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Identify experiments in Gemma that have no publication
 * Fetch their GEO records and check for pubmed IDs
 * Add the publications where we find them.
 */
public class UpdatePubMedCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ExpressionExperimentService eeserv;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private PersisterHelper persisterHelper;

    @Value("${entrez.efetch.apikey")
    private String ncbiApiKey;

    @Override
    public String getCommandName() {
        return "findDatasetPubs";
    }


    @Override
    public String getShortDesc() {
        return "Identify experiments that have no publication in Gemma and try to fill it in.";
    }

    @Override
    protected void buildOptions( Options options ) {
        addBatchOption( options );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Map<String, ExpressionExperiment> toFetch = new HashMap<>();
        Collection<ExpressionExperiment> ees = eeserv.getExperimentsLackingPublications();
        for ( ExpressionExperiment ee : ees ) {
            String shortName = ee.getShortName();
            if ( shortName.contains( "." ) ) {
                ee = eeserv.thawLite( ee );
                if ( ee.getAccession() != null ) {
                    shortName = ee.getAccession().getAccession();
                }
            }
            toFetch.put( shortName, ee );
        }
        log.info( "Found " + toFetch.size() + " experiments lacking publications in Gemma.." );

        GeoBrowser gbs = new GeoBrowserImpl( ncbiApiKey );
        Collection<GeoRecord> geoRecords = gbs.getGeoRecords( GeoRecordType.SERIES, toFetch.keySet() );

        int numFound = 0;
        for ( GeoRecord rec : geoRecords ) {
            if ( rec.getPubMedIds() == null || rec.getPubMedIds().isEmpty() ) {
                continue;
            }
            log.info( "New PubMed(s) for " + rec.getGeoAccession() );

            ExpressionExperiment expressionExperiment = toFetch.get( rec.getGeoAccession() );

            expressionExperiment = eeserv.thawLite( expressionExperiment );

            try {
                Collection<String> pmids = rec.getPubMedIds();

                String pubmedId = pmids.iterator().next();

                BibliographicReference publication = getBibliographicReference( pubmedId );

                if ( publication != null ) {
                    expressionExperiment.setPrimaryPublication( publication );
                }

                if ( pmids.size() > 1 ) {
                    for ( int i = 1; i < pmids.size(); i++ ) {
                        publication = getBibliographicReference( pubmedId );

                        if ( publication != null ) {
                            expressionExperiment.getOtherRelevantPublications().add( publication );
                        }
                    }
                }

                eeserv.update( expressionExperiment );
                numFound++;
                addSuccessObject( expressionExperiment.getShortName(), "Publication(s) added" );
            } catch ( Exception e ) {
                log.error( e.getMessage() + " while processing " + rec.getGeoAccession() );
                addErrorObject( expressionExperiment.getShortName(), e.getMessage() );
            }


        }
        log.info( "Found publications for " + numFound + " experiments" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.EXPERIMENT;
    }


    /**
     * Find and persist (if necessary) the given publication.
     * @param pubmedId pubmedID
     * @return persisted reference
     */
    private BibliographicReference getBibliographicReference( String pubmedId ) {
        // check if it already in the system
        BibliographicReference publication = bibliographicReferenceService.findByExternalId( pubmedId );
        if ( publication == null ) {
            PubMedSearch pms = new PubMedSearch( ncbiApiKey );
            Collection<String> searchTerms = new ArrayList<>();
            searchTerms.add( pubmedId );
            Collection<BibliographicReference> publications;
            try {
                publications = pms.fetchById( searchTerms );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
            publication = publications.iterator().next();

            DatabaseEntry pubAccession = DatabaseEntry.Factory.newInstance();
            pubAccession.setAccession( pubmedId );
            ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
            ed.setName( "PubMed" );
            pubAccession.setExternalDatabase( ed );

            publication.setPubAccession( pubAccession );
            publication = ( BibliographicReference ) persisterHelper.persist( publication );

        }
        return publication;
    }
}
