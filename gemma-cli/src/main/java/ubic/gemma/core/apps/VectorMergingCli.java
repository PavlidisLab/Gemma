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

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.VectorMergingService;
import ubic.gemma.core.util.CLI;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For experiments that used multiple array designs, merge the expression profiles
 *
 * @author pavlidis
 */
public class VectorMergingCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private VectorMergingService mergingService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        super.addForceOption( options );
    }

    @Override
    public String getCommandName() {
        return "vectorMerge";
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                this.processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException(
                        "Can't do vector merging on non-expressionExperiment bioassaysets" );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "For experiments that used multiple array designs, merge the expression profiles";
    }

    private void processExperiment( ExpressionExperiment expressionExperiment ) {
        try {
            mergingService.mergeVectors( expressionExperiment );
            preprocessorService.process( expressionExperiment );
            addSuccessObject( expressionExperiment );
        } catch ( Exception e ) {
            addErrorObject( expressionExperiment, e );
        }
    }
}
