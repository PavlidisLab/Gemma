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
import org.apache.commons.cli.ParseException;
import org.springframework.security.core.context.SecurityContext;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author paul
 */
public class ExpressionExperimentDataFileGeneratorCli extends ExpressionExperimentManipulatingCLI {

    private static final String DESCRIPTION = "Generate analysis text files (diff expression)";
    private ExpressionDataFileService expressionDataFileService;
    private boolean force_write = false;

    @Override
    public String getCommandName() {
        return "generateDataFile";
    }

    @Override
    protected void doWork() throws Exception {
        BlockingQueue<BioAssaySet> queue = new ArrayBlockingQueue<>( expressionExperiments.size() );

        // Add the Experiments to the queue for processing
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                try {
                    queue.put( ee );
                } catch ( InterruptedException ie ) {
                    AbstractCLI.log.info( ie );
                }
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }

        Collection<Runnable> tasks = new ArrayList<>( queue.size() );
        for ( BioAssaySet ee : queue ) {
            tasks.add( new ProcessBioAssaySet( ee ) );
        }
        executeBatchTasks( tasks );
    }

    @Override
    public String getShortDesc() {
        return ExpressionExperimentDataFileGeneratorCli.DESCRIPTION;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        Option forceWriteOption = Option.builder( "w" )
                .desc( "Overwrites exsiting files if this option is set" ).longOpt( "forceWrite" )
                .build();

        options.addOption( forceWriteOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );

        if ( commandLine.hasOption( 'w' ) ) {
            this.force_write = true;
        }

        expressionDataFileService = this.getBean( ExpressionDataFileService.class );
    }

    private void processExperiment( ExpressionExperiment ee ) {

        try {
            ee = this.eeService.thawLite( ee );

            AuditTrailService ats = this.getBean( AuditTrailService.class );

            //expressionDataFileService.writeOrLocateCoexpressionDataFile( ee, force_write );
            expressionDataFileService.writeOrLocateDiffExpressionDataFiles( ee, force_write );

            ats.addUpdateEvent( ee, CommentedEvent.class, "Generated Flat data files for downloading" );
            addSuccessObject( ee, "Success:  generated data file for " + ee.getShortName() + " ID=" + ee.getId() );

        } catch ( Exception e ) {
            addErrorObject( ee, e );
        }
    }

    // Inner class for processing the experiments
    private class ProcessBioAssaySet implements Runnable {
        private SecurityContext context;
        private BioAssaySet bioAssaySet;

        private ProcessBioAssaySet( BioAssaySet bioAssaySet ) {
            this.bioAssaySet = bioAssaySet;
        }

        @Override
        public void run() {
            BioAssaySet ee = bioAssaySet;
            if ( ee == null ) {
                return;
            }
            AbstractCLI.log.info( "Processing Experiment: " + ee.getName() );
            ExpressionExperimentDataFileGeneratorCli.this.processExperiment( ( ExpressionExperiment ) ee );
        }
    }
}
