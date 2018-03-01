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
import org.apache.commons.cli.OptionBuilder;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Prepare the "processed" expression data vectors, and can also do batch correction.F
 *
 * @author xwan, paul
 * @see ProcessedExpressionDataVectorServiceImpl
 */
public class ProcessedDataComputeCLI extends ExpressionExperimentManipulatingCLI {

    private boolean batchCorrect = false;
    private PreprocessorService preprocessorService;
    private ExpressionExperimentService expressionExperimentService;

    public static void main( String[] args ) {
        ProcessedDataComputeCLI p = new ProcessedDataComputeCLI();
        AbstractCLIContextCLI.tryDoWorkLogTime( p, args );
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

        Option outputFileOption = OptionBuilder.withDescription( "Attempt to batch-correct the data" )
                .withLongOpt( "batchcorr" ).create( 'b' );
        this.addOption( outputFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        preprocessorService = this.getBean( PreprocessorService.class );
        expressionExperimentService = this.getBean( ExpressionExperimentService.class );
        this.auditTrailService = this.getBean( AuditTrailService.class );
        eeService = this.getBean( ExpressionExperimentService.class );

        if ( this.hasOption( 'b' ) ) {
            this.batchCorrect = true;
        }
    }

    @Override
    public String getCommandName() {
        return "makeProcessedData";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = this.processCommandLine( args );
        if ( err != null ) {
            return err;
        }

        if ( expressionExperiments.size() == 0 ) {
            AbstractCLI.log.error( "You did not select any usable expression experiments" );
            return null;
        }

        for ( BioAssaySet ee : expressionExperiments ) {
            this.processExperiment( ( ExpressionExperiment ) ee );
        }
        this.summarizeProcessing();
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Performs preprocessing and can do batch correction (ComBat)";
    }

    private void processExperiment( ExpressionExperiment ee ) {
        if ( expressionExperimentService.isTroubled( ee ) && !force ) {
            AbstractCLI.log.info( "Skipping troubled experiment " + ee.getShortName() );
            return;
        }
        try {
            ee = this.eeService.thawLite( ee );

            if ( this.batchCorrect ) {
                this.preprocessorService.batchCorrect( ee, this.force );
            } else {
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
