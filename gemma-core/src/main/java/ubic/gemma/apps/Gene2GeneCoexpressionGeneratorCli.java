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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.coexpression.GeneLinkCoexpressionAnalyzer;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
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

    private GeneLinkCoexpressionAnalyzer geneVoteAnalyzer;
    private Collection<Gene> toUseGenes;
    private int toUseStringency;

    private String toUseAnalysisName;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option geneFileOption = OptionBuilder.hasArg().withArgName( "Gene List File Name" ).withDescription(
                "A text file that contains a list of gene symbols, with one gene symbol on each line" ).withLongOpt(
                "geneFile" ).create( 'g' );

        Option stringencyOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "The stringency value: Defaults to " + DEFAULT_STRINGINCY ).withLongOpt( "stringency" ).create( 's' );

        Option analysisNameOption = OptionBuilder.hasArg().isRequired().withArgName( "name" ).withDescription(
                "The name of the analysis to create" ).withLongOpt( "name" ).create( 'a' );

        addOption( geneFileOption );
        addOption( stringencyOption );
        addOption( analysisNameOption );

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

        log.info( "Using " + expressionExperiments.size() + " Expression Experiments." );
        log.info( displayEEs() );

        geneVoteAnalyzer.analyze( expressionExperiments, toUseGenes, toUseStringency, toUseAnalysisName );

        return null;
    }

    /**
     * 
     */

    @SuppressWarnings("unchecked")
    @Override
    protected void processOptions() {
        super.processOptions();
        initSpringBeans();

        if ( this.hasOption( 'g' ) ) {
            if ( !this.hasOption( 't' ) ) {
                log.info( "You must provide the taxon if you provide a gene file" );
                bail( ErrorCode.MISSING_ARGUMENT );
                return;
            }
            assert taxon != null;
            try {
                super.readGeneListFile( this.getOptionValue( 'g' ), taxon );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else {
            toUseGenes = super.geneService.loadKnownGenes( taxon );
        }

        toUseStringency = DEFAULT_STRINGINCY;
        if ( this.hasOption( 's' ) ) {
            toUseStringency = Integer.parseInt( this.getOptionValue( 's' ) );
        }

        if ( this.hasOption( 'a' ) ) {
            toUseAnalysisName = this.getOptionValue( 'a' );
        }

    }

    private String displayEEs() {
        String results = " ";
        for ( ExpressionExperiment ee : expressionExperiments ) {
            results += ee.getShortName() + "  ";
        }
        return results;
    }

    private void initSpringBeans() {
        geneVoteAnalyzer = ( GeneLinkCoexpressionAnalyzer ) this.getBean( "geneLinkCoexpressionAnalyzer" );

    }

}