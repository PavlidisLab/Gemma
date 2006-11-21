/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
/**
 * 
 */
package ubic.gemma.apps;

import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;

/**
 * CLI for computing and persisting the
 * 
 * @author Paul
 * @version $Id$
 */
public class TwoChannelMissingValueCLI extends ExpressionExperimentManipulatingCli {

    /**
     * 
     */
    private static final double DEFAULT_SIGNAL_TO_NOISE_THRESHOLD = 2.0;
    private double s2n = DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;

    // private DesignElementDataVectorService designElementDataVectorService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option signal2noiseOption = OptionBuilder.hasArg().withArgName( "Signal-to-noise" ).withDescription(
                "Signal to noise ratio, below which values are considered missing; default="
                        + DEFAULT_SIGNAL_TO_NOISE_THRESHOLD ).withLongOpt( "signal2noise" ).create( 's' );

        addOption( signal2noiseOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Two-channel missing values", args );
        if ( err != null ) return err;

        ExpressionExperiment ee = locateExpressionExperiment( this.getExperimentShortName() );

        if ( ee == null ) {
            log.error( "No expression experiment with name " + this.getExperimentShortName() );
            bail( ErrorCode.INVALID_OPTION );
        }

        this.getExpressionExperimentService().thaw( ee );
        TwoChannelMissingValues tcmv = new TwoChannelMissingValues();

        Collection<DesignElementDataVector> vectors = tcmv.computeMissingValues( ee, s2n );
        
        PersisterHelper persisterHelper = this.getPersisterHelper();
        
        for ( DesignElementDataVector vector : vectors ) {
            vector.setQuantitationType( ( QuantitationType ) persisterHelper.persist(vector.getQuantitationType()) );
        }

        ee.getDesignElementDataVectors().addAll( vectors );
        this.getExpressionExperimentService().update( ee );

        return null;
    }

    public static void main( String[] args ) {
        TwoChannelMissingValueCLI p = new TwoChannelMissingValueCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 's' ) ) {
            this.s2n = this.getDoubleOptionValue( 's' );
        }
        // this.designElementDataVectorService = ( DesignElementDataVectorService ) getBean(
        // "designElementDataVectorService" );
    }

}
