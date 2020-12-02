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

import org.apache.commons.cli.Option;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.ProcessedExpressionDataVectorCreateHelperService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Prepare the "processed" expression data vectors, and can also do batch correction.
 *
 * @author xwan, paul
 * @see ProcessedExpressionDataVectorServiceImpl
 */
public class ProcessedDataComputeCLI extends ExpressionExperimentManipulatingCLI {

    private boolean batchCorrect = false;
    private PreprocessorService preprocessorService;
    private ProcessedExpressionDataVectorCreateHelperService proccessedVectorService;
    private ExpressionExperimentService expressionExperimentService;
    private boolean updateRanks = false;
    private boolean updateDiagnostics = false;

    public static void main( String[] args ) {
        ProcessedDataComputeCLI p = new ProcessedDataComputeCLI();
        executeCommand( p, args );
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        super.buildOptions();

        super.addForceOption();
        this.addDateOption();

        Option outputFileOption = Option.builder( "b" )
                .desc( "Attempt to batch-correct the data without recomputing data  (may be combined with other options)" ).longOpt( "batchcorr" )
                .build();
        this.addOption( "diagupdate",
                "Only update the diagnostics without recomputing data (PCA, M-V, sample correlation, GEEQ; may be combined with other options)" );
        this.addOption( "rankupdate", "Only update the expression intensity rank information (may be combined with other options)" );

        this.addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        preprocessorService = this.getBean( PreprocessorService.class );
        expressionExperimentService = this.getBean( ExpressionExperimentService.class );
        proccessedVectorService = this.getBean( ProcessedExpressionDataVectorCreateHelperService.class );
        this.auditTrailService = this.getBean( AuditTrailService.class );
        eeService = this.getBean( ExpressionExperimentService.class );

        if ( this.hasOption( "diagupdate" ) ) {
            this.updateDiagnostics = true;
        }

        if ( this.hasOption( "rankupdate" ) ) {

            this.updateRanks = true;
        }

        if ( this.hasOption( 'b' ) ) {
            this.batchCorrect = true;
        }
    }

    @Override
    public String getCommandName() {
        return "makeProcessedData";
    }

    @Override
    protected void doWork( String[] args ) throws Exception {
        this.processCommandLine( args );

        if ( expressionExperiments.size() == 0 ) {
            AbstractCLI.log.error( "You did not select any usable expression experiments" );
            return;
        }

        for ( BioAssaySet ee : expressionExperiments ) {
            this.processExperiment( ( ExpressionExperiment ) ee );
        }
        this.summarizeProcessing();
    }

    @Override
    public String getShortDesc() {
        return "Performs preprocessing. Optionally can do only selected processing steps including batch correction, rank computation and diagnostic computation";
    }

    private void processExperiment( ExpressionExperiment ee ) {
        if ( expressionExperimentService.isTroubled( ee ) && !force ) {
            AbstractCLI.log.info( "Skipping troubled experiment " + ee.getShortName() );
            return;
        }
        try {
            ee = this.eeService.thawLite( ee );

            if ( this.batchCorrect || this.updateDiagnostics || this.updateRanks ) {
                log.info( "Skipping processed data vector creation; only doing selected postprocessing steps" );

                // this ordering is kind of important
                if ( this.batchCorrect ) {
                    log.info( "Batch correcting " + ee );
                    this.preprocessorService.batchCorrect( ee, this.force );
                }

                if ( this.updateRanks ) {
                    log.info( "Updating ranks: " + ee );
                    this.proccessedVectorService.updateRanks( ee );
                }

                if ( this.updateDiagnostics ) {
                    log.info( "Updating diagnostics: " + ee );
                    this.preprocessorService.processDiagnostics( ee );
                }
            } else {
                // this does all of the steps.
                this.preprocessorService.process( ee );
            }

            // Note the auditing is done by the service.
            successObjects.add( ee );
            AbstractCLI.log.info( "Successfully processed: " + ee );
        } catch ( PreprocessingException | Exception e ) {
            errorObjects.add( ee + ": " + e.getMessage() );
            AbstractCLI.log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********", e );
        }
    }
}
