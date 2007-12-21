/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Base class for CLIs that need an expression experiment as an input.
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ExpressionExperimentManipulatingCLI extends AbstractSpringAwareCLI {

    protected ExpressionExperimentService eeService;

    protected GeneService geneService;

    protected SearchService searchService;

    protected TaxonService taxonService;

    private String experimentShortName = null;

    private String excludeEeFileName;

    protected String experimentListFile = null;

    protected Taxon taxon = null;

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option expOption = OptionBuilder.hasArg().withArgName( "Expression experiment name" ).withDescription(
                "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                        + "and if this option is omitted, the tool will be applied to all expression experiments." )
                .withLongOpt( "experiment" ).create( 'e' );

        addOption( expOption );

        Option eeFileListOption = OptionBuilder.hasArg().withArgName( "Expression experiment list file" )
                .withDescription(
                        "File with list of short names of expression experiments (one per line; use instead of '-e')" )
                .withLongOpt( "eeListfile" ).create( 'f' );
        addOption( eeFileListOption );

        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" ).withDescription(
                "taxon of the expression experiments and genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );

        Option excludeEeOption = OptionBuilder.hasArg().withArgName( "Expression experiment list file" )
                .withDescription( "File containing list of expression experiments to exclude" ).withLongOpt(
                        "excludeEEFile" ).create( 'x' );
        addOption( excludeEeOption );

        Option eeSearchOption = OptionBuilder.hasArg().withArgName( "expressionQuerry" ).withDescription(
                "Use a query string for defining which expression experiments to use" ).withLongOpt( "expressionQuery" )
                .create( 'q' );
        addOption( eeSearchOption );
    }

    /**
     * @param short name of the experiment to find.
     * @return
     */
    protected ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            errorObjects.add( "Expression experiment short name must be provided" );
            return null;
        }

        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            log.error( "No experiment " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'e' ) ) {
            this.experimentShortName = this.getOptionValue( 'e' );
        }
        if ( hasOption( 'f' ) ) {
            this.experimentListFile = getOptionValue( 'f' );
        }
        if ( hasOption( 'x' ) ) {
            excludeEeFileName = getOptionValue( 'x' );
        }
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        taxonService = ( TaxonService ) getBean( "taxonService" );
        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }

    }

    protected String getExperimentShortName() {
        return experimentShortName;
    }

    @SuppressWarnings("unchecked")
    protected Collection<ExpressionExperiment> getExpressionExperiments( Taxon taxon ) throws IOException {
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        if ( experimentShortName != null ) {
            ExpressionExperiment ee = eeService.findByShortName( experimentShortName );
            if ( ee == null )
                log.error( "No experiment " + experimentShortName + " found" );
            else
                ees.add( ee );
        }
        if ( experimentListFile != null ) {
            log.info( "Reading list of expression experiments from " + experimentListFile );
            ees.addAll( readExpressionExperimentListFile( experimentListFile ) );
        }
        if ( ees.isEmpty() ) {
            if ( taxon != null ) {
                log.info( "Loading expression experiments for " + taxon.getCommonName() );
                ees.addAll( eeService.findByTaxon( taxon ) );
            } else {
                log.info( "Loading all expression experiments" );
                ees.addAll( eeService.loadAll() );
            }
        }
        if ( excludeEeFileName != null ) {
            Collection<String> excludedEeNames = readExpressionExperimentListFileToStrings( excludeEeFileName );
            int count = 0;
            for ( Iterator<ExpressionExperiment> it = ees.iterator(); it.hasNext(); ) {
                ExpressionExperiment ee = it.next();
                if ( excludedEeNames.contains( ee.getShortName() ) ) {
                    it.remove();
                    count++;
                }
            }
            log.info( "Excluded " + count + " expression experiments" );
        }
        return ees;
    }

    private Collection<String> readExpressionExperimentListFileToStrings( String fileName ) throws IOException {
        Collection<String> eeNames = new HashSet<String>();
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        while ( in.ready() ) {
            String eeName = in.readLine().trim();
            if ( eeName.startsWith( "#" ) ) {
                continue;
            }
            eeNames.add( eeName );
        }
        return eeNames;
    }

    /**
     * Load expression experiments based on a list of short names in a file.
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    protected Collection<ExpressionExperiment> readExpressionExperimentListFile( String fileName ) throws IOException {
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        for ( String eeName : readExpressionExperimentListFileToStrings( fileName ) ) {
            ExpressionExperiment ee = eeService.findByShortName( eeName );
            if ( ee == null ) {
                log.error( "No experiment " + eeName + " found" );
                continue;
            }
            ees.add( ee );
        }
        return ees;
    }

    /**
     * Use the search engine to locate expression experiments.
     * 
     * @param query
     */
    protected Collection<ExpressionExperiment> findExpressionExperimentsByQuery( String query, Taxon taxon ) {
        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        Collection<SearchResult> eeSearchResults = searchService.search(
                SearchSettings.ExpressionExperimentSearch( query ) ).get( ExpressionExperiment.class );

        log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult sr : eeSearchResults ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) sr.getResultObject();
            Taxon t = eeService.getTaxon( ee.getId() );
            if ( t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) ) {
                ees.add( ee );
            }
        }
        return ees;

    }

    /**
     * Read in a list of genes
     * 
     * @param inFile - file name to read
     * @param taxon
     * @return collection of genes
     * @throws IOException
     */
    protected Collection<Gene> readGeneListFile( String inFile, Taxon taxon ) throws IOException {
        log.info( "Reading " + inFile );

        Collection<Gene> genes = new ArrayList<Gene>();
        BufferedReader in = new BufferedReader( new FileReader( inFile ) );
        String line;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) continue;
            String s = line.trim();
            Gene gene = findGeneByOfficialSymbol( s, taxon );
            if ( gene == null ) {
                log.error( "ERROR: Cannot find genes for " + s );
                continue;
            }
            genes.add( gene );
        }
        return genes;
    }

    @SuppressWarnings("unchecked")
    protected Gene findGeneByOfficialSymbol( String symbol, Taxon taxon ) {
        Collection<Gene> genes = geneService.findByOfficialSymbolInexact( symbol );
        for ( Gene gene : genes ) {
            if ( taxon.equals( gene.getTaxon() ) ) return gene;
        }
        return null;
    }

}
