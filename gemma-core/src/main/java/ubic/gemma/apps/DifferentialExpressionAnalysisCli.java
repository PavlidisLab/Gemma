/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends AbstractGeneExpressionExperimentManipulatingCLI {
    private static Log log = LogFactory.getLog( DifferentialExpressionAnalysisCli.class );

    // private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private DesignElementDataVectorService designElementDataVectorService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: runing on one data set, running on list of data sets from a file,
         * running on all data sets.
         */
        super.buildOptions();

        /* Supports: runing on all data sets that have not been run since a given date. */
        super.addDateOption();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        DifferentialExpressionAnalysis analysis = new DifferentialExpressionAnalysis();

        designElementDataVectorService = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        // TODO add a DifferentialExpressionAnalysisService
        // differentialExpressionAnalysisService = ( DifferentialExpressionAnalysisService ) this
        // .getBean( "differentialExpressionAnalysisService" );

        if ( this.getExperimentShortName() != null ) {
            String[] shortNames = this.getExperimentShortName().split( "," );

            if ( shortNames.length > 1 )
                throw new RuntimeException( this.getClass().getName()
                        + " supports 1 expression experiment at this time." );

            for ( String shortName : shortNames ) {
                ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );

                if ( expressionExperiment == null ) continue;

                eeService.thaw( expressionExperiment );

                // Collection<QuantitationType> quantitationTypes = expressionExperiment.getQuantitationTypes();
                // quantitationTypeService.
                // for ( QuantitationType qt : quantitationTypes ) {
                // log.info( qt );
                // }

                Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
                designElementDataVectorService.thaw( vectors );

                // for ( DesignElementDataVector vector : vectors ) {
                // log.info( vector.getBioAssayDimension() );
                // }

                // analysis.analyze( expressionExperiment, quantitationType, bioAssayDimension );
            }

        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DifferentialExpressionAnalysisCli analysisCli = new DifferentialExpressionAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysisCli.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
