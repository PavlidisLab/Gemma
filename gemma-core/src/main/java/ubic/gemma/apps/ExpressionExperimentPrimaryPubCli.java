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

package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.entrez.pubmed.ExpressionExperimentBibRefFinder;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.Persister;
import cern.colt.Arrays;

/**
 * Update the primary publication for experiments.
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionExperimentPrimaryPubCli extends ExpressionExperimentManipulatingCLI {

    private String pubmedIdFilename;
    private Map<String, Integer> pubmedIds = new HashMap<>();

    public static void main( String[] args ) {
        ExpressionExperimentPrimaryPubCli p = new ExpressionExperimentPrimaryPubCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option pubmedOption = OptionBuilder
                .hasArg()
                .withArgName( "pubmedIDFile" )
                .withDescription(
                        "A text file which contains the list of pubmed IDs associated with each experiment ID. If the pubmed ID is not found, it will try to use the existing pubmed ID associated with the experiment. Each row has two columns: pubmedId and experiment shortName, e.g. 22438826<TAB>GSE27715" )
                .withLongOpt( "pubmedIDFile" ).create( "pmidFile" );

        addOption( pubmedOption );
        this.addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Expression experiment bibref finder ", args );
        if ( err != null ) return err;
        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );

        Persister ph = this.getPersisterHelper();
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
            // if ( experiment.getPrimaryPublication() != null ) continue;
            if ( experiment.getPrimaryPublication() == null ) {
                log.warn( experiment + " has no existing primary publication" );
            }
            experiment = ees.thawLite( experiment );

            // get from GEO or get from a file
            BibliographicReference ref = fetcher.retrieveByHTTP( pubmedIds.get( experiment.getShortName() ) );

            if ( ref == null ) {
                if ( this.pubmedIdFilename != null ) {
                    log.warn( "Pubmed ID for " + experiment.getShortName() + " was not found in "
                            + this.pubmedIdFilename );
                }
                ref = finder.locatePrimaryReference( experiment );

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

            try {
                log.info( "Found pubAccession " + ref.getPubAccession().getAccession() + " for " + experiment );
                ref = ( BibliographicReference ) ph.persist( ref );
                experiment.setPrimaryPublication( ref );
                ees.update( experiment );
            } catch ( Exception e ) {
                log.error( experiment.getShortName() + " (id=" + experiment.getId() + ") update failed." );
                e.printStackTrace();
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

        return null;
    }

    /**
     * Reads pubmedID and experiment short name from the file and stores it in a HashMap<eeShortName, pubmedId>. E.g.
     * 22438826 GSE27715 22340501 GSE35802
     * 
     * @param filename
     * @throws IOException
     * @throws FileNotFoundException
     */
    private Map<String, Integer> parsePubmedIdFile( String filename ) throws FileNotFoundException, IOException {
        Map<String, Integer> ids = new HashMap<>();
        final int COL_COUNT = 2;
        try (BufferedReader br = new BufferedReader( new FileReader( filename ) )) {
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

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( "pmidFile" ) ) {
            this.pubmedIdFilename = this.getOptionValue( "pmidFile" );
            try {
                this.pubmedIds = parsePubmedIdFile( this.pubmedIdFilename );
            } catch ( FileNotFoundException e ) {
                log.error( e.getMessage() );
                e.printStackTrace();
            } catch ( IOException e ) {
                log.error( e.getMessage() );
                e.printStackTrace();
            }
        }
    }
}
