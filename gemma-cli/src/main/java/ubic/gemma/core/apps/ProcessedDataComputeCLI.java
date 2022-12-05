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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
 * @see    ProcessedExpressionDataVectorServiceImpl
 */
public class ProcessedDataComputeCLI extends ExpressionExperimentManipulatingCLI {

    //   private boolean batchCorrect = false;
    private PreprocessorService preprocessorService;
    private ProcessedExpressionDataVectorCreateHelperService proccessedVectorService;
    private ExpressionExperimentService expressionExperimentService;
    private boolean updateRanks = false;
    private boolean updateDiagnostics = false;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {

        super.buildOptions( options );

        super.addForceOption( options );
        this.addDateOption( options );

        options.addOption( "diagupdate",
                "Only update the diagnostics without recomputing data (PCA, M-V, sample correlation, GEEQ; may be combined with other options)" );
        options.addOption( "rankupdate", "Only update the expression intensity rank information (may be combined with other options)" );

        //  options.addOption( outputFileOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );
        preprocessorService = this.getBean( PreprocessorService.class );
        expressionExperimentService = this.getBean( ExpressionExperimentService.class );
        proccessedVectorService = this.getBean( ProcessedExpressionDataVectorCreateHelperService.class );
        this.auditTrailService = this.getBean( AuditTrailService.class );
        eeService = this.getBean( ExpressionExperimentService.class );

        if ( commandLine.hasOption( "diagupdate" ) ) {
            this.updateDiagnostics = true;
        }

        if ( commandLine.hasOption( "rankupdate" ) ) {

            this.updateRanks = true;
        }
    }

    @Override
    public String getCommandName() {
        return "makeProcessedData";
    }

    @Override
    protected void doWork() throws Exception {
        if ( expressionExperiments.size() == 0 ) {
            AbstractCLI.log.error( "You did not select any usable expression experiments" );
            return;
        }

        for ( BioAssaySet ee : expressionExperiments ) {
            this.processExperiment( ( ExpressionExperiment ) ee );
        }
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

            if ( this.updateDiagnostics || this.updateRanks ) {
                log.info( "Skipping processed data vector creation; only doing selected postprocessing steps" );

                if ( this.updateRanks ) {
                    log.info( "Updating ranks: " + ee );
                    this.proccessedVectorService.updateRanks( ee );
                }

                if ( this.updateDiagnostics ) {
                    log.info( "Updating diagnostics: " + ee );
                    this.preprocessorService.processDiagnostics( ee );
                }
            } else {
                // this does all of the steps including batch correction
                this.preprocessorService.process( ee );
            }

            // Note the auditing is done by the service.
            addSuccessObject( ee, "Successfully processed: " + ee );
        } catch ( Exception e ) {
            addErrorObject( ee, e.getMessage(), e );
        }
    }
}
