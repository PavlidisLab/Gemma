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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.QuantitationMismatchPreprocessingException;
import ubic.gemma.core.datastructure.matrix.SuspiciousValuesForQuantitationException;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorServiceImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.stream.Collectors;

/**
 * Prepare the "processed" expression data vectors, and can also do batch correction.
 *
 * @author xwan, paul
 * @see    ProcessedExpressionDataVectorServiceImpl
 */
public class ProcessedDataComputeCLI extends ExpressionExperimentManipulatingCLI {

    private static final String
            UPDATE_DIAGNOSTICS_OPTION = "diagupdate",
            UPDATE_RANKS_OPTION = "rankupdate",
            IGNORE_QUANTITATION_MISMATCH_OPTION = "ignoreqm";

    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private ProcessedExpressionDataVectorService processedVectorService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    //   private boolean batchCorrect = false;
    private boolean updateRanks = false;
    private boolean updateDiagnostics = false;
    private boolean ignoreQuantitationMismatch = false;

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        addForceOption( options );
        addDateOption( options );
        options.addOption( UPDATE_DIAGNOSTICS_OPTION, false,
                "Only update the diagnostics without recomputing data (PCA, M-V, sample correlation, GEEQ; may be combined with other options)" );
        options.addOption( UPDATE_RANKS_OPTION, false, "Only update the expression intensity rank information (may be combined with other options)" );
        options.addOption( IGNORE_QUANTITATION_MISMATCH_OPTION, false, "Ignore mismatch between raw quantitations and that inferred from data" );
        //  options.addOption( outputFileOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        this.updateDiagnostics = commandLine.hasOption( UPDATE_DIAGNOSTICS_OPTION );
        this.updateRanks = commandLine.hasOption( UPDATE_RANKS_OPTION );
        this.ignoreQuantitationMismatch = commandLine.hasOption( IGNORE_QUANTITATION_MISMATCH_OPTION );
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
                    this.processedVectorService.updateRanks( ee );
                }

                if ( this.updateDiagnostics ) {
                    log.info( "Updating diagnostics: " + ee );
                    this.preprocessorService.processDiagnostics( ee );
                }
            } else {
                // this does all of the steps including batch correction
                this.preprocessorService.process( ee, ignoreQuantitationMismatch );
            }

            // Note the auditing is done by the service.
            addSuccessObject( ee );
        } catch ( QuantitationMismatchPreprocessingException e ) {
            // TODO: e.getCause().getQuantitationType();
            QuantitationType qt = e.getCause().getQuantitationType();
            if ( e.getCause() instanceof SuspiciousValuesForQuantitationException ) {
                addErrorObject( String.format( "%s:\n%s", ee, qt ), String.format( "The following issues were found in expression data:\n\n - %s\n\nYou may ignore this by setting the -%s option.",
                        ( ( SuspiciousValuesForQuantitationException ) e.getCause() )
                                .getSuspiciousValues().stream()
                                .map( SuspiciousValuesForQuantitationException.SuspiciousValueResult::toString )
                                .collect( Collectors.joining( "\n - " ) ), IGNORE_QUANTITATION_MISMATCH_OPTION ) );
            } else {
                addErrorObject( String.format( "%s:\n%s", ee, qt ), e.getCause().getMessage() );
            }
        } catch ( Exception e ) {
            addErrorObject( ee, e );
        }
    }
}
