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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.QuantitationTypeDetectionRelatedPreprocessingException;
import ubic.gemma.core.analysis.preprocess.detect.SuspiciousValuesForQuantitationException;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
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
    protected void buildExperimentOptions( Options options ) {
        addForceOption( options );
        addLimitingDateOption( options );
        options.addOption( UPDATE_DIAGNOSTICS_OPTION, false,
                "Only update the diagnostics without recomputing data (PCA, M-V, sample correlation, GEEQ; may be combined with other options)" );
        options.addOption( UPDATE_RANKS_OPTION, false, "Only update the expression intensity rank information (may be combined with other options)" );
        options.addOption( IGNORE_QUANTITATION_MISMATCH_OPTION, false, "Ignore mismatch between raw quantitations and that inferred from data" );
        //  options.addOption( outputFileOption );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        this.updateDiagnostics = commandLine.hasOption( UPDATE_DIAGNOSTICS_OPTION );
        this.updateRanks = commandLine.hasOption( UPDATE_RANKS_OPTION );
        this.ignoreQuantitationMismatch = commandLine.hasOption( IGNORE_QUANTITATION_MISMATCH_OPTION );
    }

    @Override
    public String getCommandName() {
        return "makeProcessedData";
    }

    @Override
    public String getShortDesc() {
        return "Performs preprocessing. Optionally can do only selected processing steps including batch correction, rank computation and diagnostic computation";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( expressionExperimentService.isTroubled( ee ) && !isForce() ) {
            addErrorObject( ee, "Skipping troubled experiment " + ee.getShortName() + ", use -" + FORCE_OPTION + " to process." );
            return;
        }
        try {
            ee = this.eeService.thaw( ee );

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

            try {
                refreshExpressionExperimentFromGemmaWeb( ee, true, false );
            } catch ( Exception e ) {
                addWarningObject( ee, "Failed to refresh experiment from Gemma Web.", e );
            }

            // Note the auditing is done by the service.
            addSuccessObject( ee );
        } catch ( QuantitationTypeDetectionRelatedPreprocessingException e ) {
            if ( e.getCause() instanceof SuspiciousValuesForQuantitationException ) {
                SuspiciousValuesForQuantitationException actual = ( SuspiciousValuesForQuantitationException ) e.getCause();
                QuantitationType qt = actual.getQuantitationType();
                addErrorObject( String.format( "%s:\n%s", ee, qt ), String.format( "The following issues were found in expression data:\n\n - %s\n\nYou may ignore this by setting the -%s option.",
                        actual
                                .getSuspiciousValues().stream()
                                .map( SuspiciousValuesForQuantitationException.SuspiciousValueResult::toString )
                                .collect( Collectors.joining( "\n - " ) ), IGNORE_QUANTITATION_MISMATCH_OPTION ) );
            } else {
                addErrorObject( ee, e );
            }
        } catch ( Exception e ) {
            addErrorObject( ee, e );
        }
    }
}
