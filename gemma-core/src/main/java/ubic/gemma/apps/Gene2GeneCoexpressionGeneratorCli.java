/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.expression.coexpression.Gene2GenePopulationService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentSetService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionCache;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Run the gene link analysis, which converts probe links to gene links stored in the system.
 * 
 * @author klc
 * @author paul (refactoring)
 * @version $Id$
 */
public class Gene2GeneCoexpressionGeneratorCli extends ExpressionExperimentManipulatingCLI {

    private static final int DEFAULT_STRINGINCY = 2;

    public static void main( String[] args ) {
        Gene2GeneCoexpressionGeneratorCli p = new Gene2GeneCoexpressionGeneratorCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private Gene2GenePopulationService geneVoteAnalyzer;
    private Collection<Gene> toUseGenes;
    private int toUseStringency = DEFAULT_STRINGINCY;

    private boolean useDB = true;
    private boolean nodeDegreeOnly = false;
    private boolean updateExperimentSetsOnly = false;

    ExpressionExperimentSetService experimentSetService;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option geneFileOption = OptionBuilder
                .hasArg()
                .withArgName( "Gene List File Name" )
                .withDescription( "A text file that contains a list of gene symbols, with one gene symbol on each line" )
                .withLongOpt( "geneFile" ).create( 'g' );

        Option stringencyOption = OptionBuilder.hasArg().withArgName( "stringency" )
                .withDescription( "The stringency value: Defaults to " + DEFAULT_STRINGINCY )
                .withLongOpt( "stringency" ).create( 's' );

        Option noDBOption = OptionBuilder.withDescription( "Do not persist (print to stdout)" ).create( "nodb" );

        Option nodeDegreeOnlyOption = OptionBuilder.withDescription( "Only populate the node degree information" )
                .create( "nodes" );

        Option updateExperimentSetsOnlyOption = OptionBuilder.withDescription(
                "Only populate the 'master' Experiment Sets" ).create( "sets" );

        addOption( noDBOption );
        addOption( geneFileOption );
        addOption( stringencyOption );

        addOption( nodeDegreeOnlyOption );
        addOption( updateExperimentSetsOnlyOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Gene 2 Gene Coexpression Caching tool ", args );
        if ( err != null ) return err;

        log.debug( displayEEs() );
        if ( this.expressionExperimentSet != null ) {
            throw new UnsupportedOperationException(
                    "Don't use an EEset; Use all EEs instead please, by specifying a Taxon" );
        }
        assert this.taxon != null : "Please provide a taxon.";

        String analysisName = null;

        if ( this.hasOption( 'g' ) ) {
            analysisName = this.toUseGenes.size() + " genes read from " + this.getOptionValue( 'g' ) + " for "
                    + this.taxon.getCommonName();
        } else {
            analysisName = "All " + taxon.getCommonName();
        }

        Collection<ExpressionExperiment> eesToUse = new HashSet<ExpressionExperiment>();
        for ( BioAssaySet ba : expressionExperiments ) {
            if ( ba instanceof ExpressionExperiment ) {
                eesToUse.add( ( ExpressionExperiment ) ba );
            }
        }

        log.info( "Using " + this.expressionExperiments.size() + " Expression Experiments." );
        this.getBean( Probe2ProbeCoexpressionCache.class ).setEnabled( false );

        if ( this.nodeDegreeOnly ) {
            log.info( "CLI configured to only update 'node degrees'" );
            geneVoteAnalyzer.nodeDegreeAnalysis( eesToUse, toUseGenes, useDB );
        } else if ( this.updateExperimentSetsOnly ) {
            throw new UnsupportedOperationException( "Disabled until we figure out a good way to do this" );
            // log.info( "CLI configured to only update the 'master' Experiment Set" );
            // /*
            // * OK, this might not be a good idea.
            // *
            // * The problem is we can't (easily) separate the set that is associated with the
            // */
            // experimentSetService.initAutomaticallyGeneratedExperimentSet( eesToUse, taxon );
        } else if ( this.taxon != null ) {
            assert !toUseGenes.isEmpty();
            geneVoteAnalyzer.analyze( taxon, toUseGenes, toUseStringency, analysisName, useDB );
        } else {
            // analyze just the specific ones asked for. Not usual.
            geneVoteAnalyzer.analyze( eesToUse, toUseGenes, toUseStringency, analysisName, useDB );
        }
        return null;
    }

    /**
     * 
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        initSpringBeans();

        if ( this.hasOption( "nodb" ) ) {
            log.info( "Skipping database persisting of results" );
            this.useDB = false;
        }

        // arbitrary precedence, no big deal.
        if ( this.hasOption( "nodes" ) ) {
            this.nodeDegreeOnly = true;
        } else if ( this.hasOption( "sets" ) ) {
            this.updateExperimentSetsOnly = true;
        }

        if ( this.hasOption( 'g' ) ) {
            if ( !this.hasOption( 't' ) ) {
                log.info( "You must provide the taxon if you provide a gene file" );
                bail( ErrorCode.MISSING_ARGUMENT );
                return;
            }
            assert taxon != null;
            try {
                toUseGenes = super.readGeneListFile( this.getOptionValue( 'g' ), taxon );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else {
            toUseGenes = super.geneService.loadAll( taxon );
        }

        if ( toUseGenes.size() < 2 ) {
            throw new IllegalArgumentException( "You must specify at least two genes" );
        }

        toUseStringency = DEFAULT_STRINGINCY;
        if ( this.hasOption( 's' ) ) {
            toUseStringency = Integer.parseInt( this.getOptionValue( 's' ) );
        }

    }

    /**
     * Debugging.
     * 
     * @return
     */
    private String displayEEs() {
        String results = " ";
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                results += ( ( ExpressionExperiment ) ee ).getShortName() + "  ";
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }
        return results;
    }

    private void initSpringBeans() {
        experimentSetService = this.getBean( ExpressionExperimentSetService.class );
        geneVoteAnalyzer = this.getBean( Gene2GenePopulationService.class );

    }
}