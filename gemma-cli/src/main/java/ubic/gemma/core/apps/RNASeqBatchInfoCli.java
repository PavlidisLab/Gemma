/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.config.Settings;

/**
 * Add batch information for RNA-seq experiments.
 *
 * @author tesar
 * @deprecated this should not be necessary and the regular batch population tool can be used instead.
 */
public class RNASeqBatchInfoCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private BatchInfoPopulationService batchService;

    private String fastqRootDir = Settings.getString( "gemma.fastq.headers.dir" );

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        super.addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );

    }

    @Override
    public String getCommandName() {
        return "rnaseqBatchInfo";
    }

    @Override
    protected void doWork() throws Exception {
        log.info( "Checking folders for existing experiments in " + fastqRootDir );
        for ( BioAssaySet ee : this.expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                try {
                    batchService.fillBatchInformation( ( ExpressionExperiment ) ee, this.force );
                    addSuccessObject( ee, "Added batch information" );
                } catch ( Exception e ) {
                    addErrorObject( ee, "Failed to add batch information.", e );
                }
            } else {
                addErrorObject( ee, "This is not an ExpressionExperiment" );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Load RNASeq batch information; header files expected to be in structure like ${gemma.fastq.headers.dir}/GSExxx/GSMxxx/SRRxxx.fastq.header";
    }

}
