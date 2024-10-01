/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import cern.colt.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.entrez.pubmed.ExpressionExperimentBibRefFinder;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Update the primary publication for experiments.
 *
 * @author paul
 */
public class ExpressionExperimentPrimaryPubCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionExperimentService ees;
    @Autowired
    private PersisterHelper persisterHelper;

    private String pubmedIdFilename;
    private Map<String, Integer> pubmedIds = new HashMap<>();

    @Override
    public String getCommandName() {
        return "pubmedAssociateToExperiments";
    }

    @Override
    public String getShortDesc() {
        return "Set or update the primary publication for experiments by fetching from GEO";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option pubmedOption = Option.builder( "pubmedIDFile" ).hasArg().argName( "pubmedIDFile" ).desc(
                        "A text file which contains the list of pubmed IDs associated with each experiment ID. "
                                + "If the pubmed ID is not found, it will try to use the existing pubmed ID associated "
                                + "with the experiment. Each row has two columns: pubmedId and experiment shortName, "
                                + "e.g. 22438826<TAB>GSE27715" )
                .build();

        options.addOption( pubmedOption );
        this.addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( "pmidFile" ) ) {
            this.pubmedIdFilename = commandLine.getOptionValue( "pmidFile" );
            try {
                this.pubmedIds = parsePubmedIdFile( this.pubmedIdFilename );
            } catch ( IOException e ) {
                log.error( "Failed to parse PubMed ID file: " + this.pubmedIdFilename + ".", e );
            }
        }
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();

        // collect some statistics
        Collection<String> nullPubCount = new ArrayList<>();
        Collection<String> samePubCount = new ArrayList<>();
        Collection<String> diffPubCount = new ArrayList<>();
        Collection<String> failedEe = new ArrayList<>();

        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        for ( BioAssaySet bioassay : expressionExperiments ) {
            if ( !( bioassay instanceof ExpressionExperiment ) ) {
                log.info( bioassay.getName() + " is not an ExpressionExperiment" );
                continue;
            }
            ExpressionExperiment experiment = ( ExpressionExperiment ) bioassay;
            try {
                // if ( experiment.getPrimaryPublication() != null ) continue;
                if ( experiment.getPrimaryPublication() == null ) {
                    log.warn( experiment + " has no existing primary publication, will attempt to find" );
                } else {
                    log.info( experiment.getPrimaryPublication() + " has a primary publication, updating" );
                }
                experiment = ees.thawLite( experiment );

                // get from GEO or get from a file
                BibliographicReference ref = fetcher.retrieveByHTTP( pubmedIds.get( experiment.getShortName() ) );

                if ( ref == null ) {
                    if ( this.pubmedIdFilename != null ) {
                        log.warn( "Pubmed ID for " + experiment.getShortName() + " was not found in "
                                + this.pubmedIdFilename );
                    }
                    try {
                        ref = finder.locatePrimaryReference( experiment );
                    } catch ( IOException e ) {
                        log.error( e );
                        continue;
                    }

                    if ( ref == null ) {
                        log.error( "No ref for " + experiment );
                        failedEe.add( experiment.getShortName() );
                        continue;
                    }
                }

                // collect some statistics
                if ( experiment.getPrimaryPublication() == null ) {
                    nullPubCount.add( experiment.getShortName() );
                } else if ( experiment.getPrimaryPublication().getPubAccession().getAccession()
                        .equals( pubmedIds.get( experiment.getShortName() ).toString() ) ) {
                    samePubCount.add( experiment.getShortName() );
                } else {
                    diffPubCount.add( experiment.getShortName() );
                }

                log.info( "Found pubAccession " + ref.getPubAccession().getAccession() + " for " + experiment );
                ref = ( BibliographicReference ) persisterHelper.persist( ref );
                experiment.setPrimaryPublication( ref );
                ees.update( experiment );
            } catch ( Exception e ) {
                log.error( experiment.getShortName() + " (id=" + experiment.getId() + ") update failed.", e );
            }
        }

        // print statistics
        log.info( "\n\n========== Summary ==========" );
        log.info( "Total number of experiments: " + expressionExperiments.size() );
        log.info( "Same publication: " + samePubCount.size() );
        log.info( "Diff publication: " + diffPubCount.size() );
        log.info( "No initial publication: " + nullPubCount.size() );
        log.info( "No publications found: " + failedEe.size() );

        log.info( "\n\n========== Details ==========" );
        log.info( "Diff publication: " + Arrays.toString( diffPubCount.toArray() ) );
        log.info( "No initial publication: " + Arrays.toString( nullPubCount.toArray() ) );
        log.info( "No publications found: " + Arrays.toString( failedEe.toArray() ) );
    }

    /**
     * Reads pubmedID and experiment short name from the file and stores it in a HashMap<eeShortName, pubmedId>. E.g.
     * 22438826 GSE27715 22340501 GSE35802
     */
    private Map<String, Integer> parsePubmedIdFile( String filename ) throws IOException {
        Map<String, Integer> ids = new HashMap<>();
        final int COL_COUNT = 2;
        try ( BufferedReader br = new BufferedReader( new FileReader( filename ) ) ) {
            String row;
            while ( ( row = br.readLine() ) != null ) {
                String[] col = row.split( "\t" );
                if ( col.length < COL_COUNT ) {
                    log.error( "Line must contain " + COL_COUNT + " columns. Invalid line " + row );
                    continue;
                }
                ids.put( col[1].trim(), Integer.parseInt( col[0].trim() ) );
            }
        }
        log.info( "Read " + ids.size() + " entries from " + filename );

        return ids;
    }
}
