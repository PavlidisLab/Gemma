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

import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.File;
import java.util.Map;

/**
 * Delete one or more experiments from the system.
 *
 * @author paul
 */
public class RNASeqBatchInfoCli extends ExpressionExperimentManipulatingCLI {

    @SuppressWarnings("FieldCanBeLocal")
    private BatchInfoPopulationService batchService;
    private String path = BatchInfoPopulationServiceImpl.FASTQ_HEADERS_ROOT;

    public static void main( String[] args ) {
        RNASeqBatchInfoCli d = new RNASeqBatchInfoCli();
        Exception e = d.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();
        addOption( "path", true, "Path to the root directory where the fastq header files are located."
                + " The expected structure in this root directory is then: ./GSExxx/GSMxxx/SRRxxx.fastq.header \n"
                + " If not provided, defaults to " + path );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( "path" ) ) {
            this.path = this.getOptionValue( "path" );
        }
    }

    @Override
    public String getCommandName() {
        return "rnaseqBatchInfo";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = super.processCommandLine( args );
        if ( e != null )
            return e;

        batchService = this.getBean( BatchInfoPopulationService.class );

        log.info( "Checking folders for existing experiments in " + path );
        Map<String, File> eeFolders = batchService.getFoldersForFolder( new File( BatchInfoPopulationServiceImpl.FASTQ_HEADERS_ROOT ) );
        for ( String eeName : eeFolders.keySet() ) {
            ExpressionExperiment ee = eeService.findByShortName( eeName );
            if ( ee == null ) {
                log.error( "Could not find experiment with short name " + eeName + ", skipping!" );
                continue;
            }
            if ( this.expressionExperiments.contains( ee ) ) {
                log.info( "Found folder for "+eeName+", starting data processing..." );
                if ( batchService.fillBatchInformation( ee, this.force, true ) ) {
                    successObjects.add( ee );
                } else {
                    errorObjects.add( ee );
                }
            } else {
                log.info( "The list of experiments to be processed does not contain " + eeName + ", skipping." );
            }
        }

        summarizeProcessing();

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Load RNASeq batch information";
    }

}
