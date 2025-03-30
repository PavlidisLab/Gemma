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
package ubic.gemma.apps;

import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Add batch information for RNA-seq experiments.
 *
 * @author tesar
 * @deprecated this should not be necessary and the regular batch population tool can be used instead.
 */
public class RNASeqBatchInfoCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private BatchInfoPopulationService batchService;

    @Value("${gemma.fastq.headers.dir}")
    private String fastqRootDir;

    @Override
    public String getCommandName() {
        return "rnaseqBatchInfo";
    }

    @Override
    public String getShortDesc() {
        return "Load RNASeq batch information; header files expected to be in structure like ${gemma.fastq.headers.dir}/GSExxx/GSMxxx/SRRxxx.fastq.header";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        super.addForceOption( options );
    }

    @Override
    protected Collection<BioAssaySet> preprocessBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        log.info( "Checking folders for existing experiments in " + fastqRootDir );
        return super.preprocessBioAssaySets( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        batchService.fillBatchInformation( ee, isForce() );
    }
}
