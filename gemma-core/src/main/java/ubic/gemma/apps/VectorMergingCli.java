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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.VectorMergingService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * For experiments that used multiple array designs, merge the expression profiles
 * 
 * @author pavlidis
 * @version $Id$
 */
public class VectorMergingCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        VectorMergingCli v = new VectorMergingCli();
        Exception e = v.doWork( args );
        if ( e != null ) {
            log.fatal( e );
        }
    }

    private Long dimId = null;

    private VectorMergingService mergingService;

    @Override
    public String getShortDesc() {
        return "For experiments that used multiple array designs, merge the expression profiles";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option dimOption = OptionBuilder.hasArg().withArgName( "Dimension ID" ).withDescription(
                "ID of pre-existing BioAssayDimension to use (instead of creating a new one)" ).withLongOpt( "dim" )
                .create( 'd' );

        addOption( dimOption );

    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "Merge vectors across array designs", args );
        if ( e != null ) {
            return e;
        }

        mergingService = ( VectorMergingService ) this.getBean( "vectorMergingService" );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException(
                        "Can't do vector merging on non-expressionExperiment bioassaysets" );
            }
        }

        summarizeProcessing();
        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'd' ) ) {
            this.dimId = new Long( this.getIntegerOptionValue( 'd' ) );
        }
    }

    /**
     * @param expressionExperiment
     */
    private void processExperiment( ExpressionExperiment expressionExperiment ) {
        eeService.thawLite( expressionExperiment );
        if ( this.dimId != null ) {
            mergingService.mergeVectors( expressionExperiment, dimId );
        } else {
            mergingService.mergeVectors( expressionExperiment );
        }

        log.info( "Finished processing " + expressionExperiment );
    }
}
