/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.core.analysis.preprocess.SplitExperimentService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Split an experiment into parts based on an experimental factor
 *
 * @author paul
 */
public class SplitExperimentCli extends ExpressionExperimentManipulatingCLI {

    private static final String FACTOR_OPTION = "factor",
            SKIP_POST_PROCESSING_OPTION = "nopost",
            DELETE_ORIGINAL_EXPERIMENT_OPTION = "deleteOriginalExperiment";

    @Autowired
    private SplitExperimentService serv;
    @Autowired
    private EntityLocator entityLocator;

    private String factorIdentifier;
    private boolean skipPostProcessing;
    private boolean deleteOriginalExperiment;

    public SplitExperimentCli() {
        super();
        setSingleExperimentMode();
    }

    @Override
    public String getCommandName() {
        return "splitExperiment";
    }

    @Override
    public String getShortDesc() {
        return "Split an experiment into parts based on an experimental factor";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addRequiredOption( FACTOR_OPTION, "factor", true, "ID numbers, categories or names of the factor to use, with spaces replaced by underscores (must not be 'batch')" );
        options.addOption( SKIP_POST_PROCESSING_OPTION, "no-post-processing", false, "Skip post-processing of resulting splits if applicable." );
        options.addOption( DELETE_ORIGINAL_EXPERIMENT_OPTION, "delete-original-experiment", false, "Delete the original experiment once the split succeeds." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        factorIdentifier = commandLine.getOptionValue( FACTOR_OPTION );
        skipPostProcessing = commandLine.hasOption( SKIP_POST_PROCESSING_OPTION );
        deleteOriginalExperiment = commandLine.hasOption( DELETE_ORIGINAL_EXPERIMENT_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = this.eeService.thawLite( ee );
        ExperimentalFactor splitOn = entityLocator.locateExperimentalFactor( ee, factorIdentifier );
        ExpressionExperimentSet eeSet = serv.split( ee, splitOn, !skipPostProcessing, deleteOriginalExperiment );
        addSuccessObject( ee, "Experiment was split on " + splitOn + " into " + eeSet.getExperiments().size() + " parts." );
    }
}
