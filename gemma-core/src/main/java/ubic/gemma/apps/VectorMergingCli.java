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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class VectorMergingCli extends AbstractGeneExpressionExperimentManipulatingCLI {

    DesignElementDataVectorService vectorService;

    Long dimId = null;

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

        ExpressionExperiment expressionExperiment = locateExpressionExperiment( this.getExperimentShortName() );
        eeService.thawLite( expressionExperiment );

        vectorService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );

        VectorMergingService mergingService = ( VectorMergingService ) this.getBean( "vectorMergingService" );

        if ( this.dimId != null ) {
            mergingService.mergeVectors( expressionExperiment, dimId );
        } else {
            mergingService.mergeVectors( expressionExperiment );
        }

        log.info( "Finished processing " + expressionExperiment );

        return null;
    }

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

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'd' ) ) {
            this.dimId = new Long( this.getIntegerOptionValue( 'd' ) );
        }
    }
}
