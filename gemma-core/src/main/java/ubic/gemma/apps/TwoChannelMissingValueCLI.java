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
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * CLI for computing and persisting the 'present' calls for two-channel data -- AND creates the processed data vectors
 * (saving you a step!)
 * 
 * @author Paul
 * @version $Id$
 */
public class TwoChannelMissingValueCLI extends ExpressionExperimentManipulatingCLI {

    private static final String MISSING_VALUE_OPTION = "mvind";

    /**
     * 
     */

    /**
     * @param args
     */
    public static void main( String[] args ) {
        TwoChannelMissingValueCLI p = new TwoChannelMissingValueCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            log.error( e, e );
        }
    }

    private TwoChannelMissingValues tcmv;

    private DesignElementDataVectorService dedvs;

    private ProcessedExpressionDataVectorCreateService pedvs;

    private double s2n = TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;

    private Collection<Double> extraMissingValueIndicators = new HashSet<Double>();

    private QuantitationTypeService quantitationTypeService;

    @Override
    public String getShortDesc() {
        return "Computes missing value information and updates processed data vectors on two-channel microarray experiments";
    }

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
                        + TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD ).withLongOpt( "signal2noise" )
                .create( 's' );

        addOption( signal2noiseOption );

        Option extraMissingIndicators = OptionBuilder.hasArg().withArgName( "mv indicators" ).withDescription(
                "Additional numeric values (comma delimited) to be considered missing values." ).create(
                MISSING_VALUE_OPTION );

        addOption( extraMissingIndicators );

        super.addDateOption();
        super.addForceOption();
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
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException(
                        "Can't do two-channel missing values on non-expressionExperiment bioassaysets" );
            }
        }

        summarizeProcessing();
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 's' ) ) {
            this.s2n = this.getDoubleOptionValue( 's' );
        }

        if ( hasOption( "force" ) ) {
            this.force = true;
        }
        if ( hasOption( MISSING_VALUE_OPTION ) ) {
            String o = this.getOptionValue( MISSING_VALUE_OPTION );
            String[] vals = StringUtils.split( o, ',' );
            try {
                for ( String string : vals ) {
                    this.extraMissingValueIndicators.add( new Double( string ) );
                }
            } catch ( NumberFormatException e ) {
                log.error( "Arguments to mvind must be numbers" );
                this.bail( ErrorCode.INVALID_OPTION );
            }
        }
        tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
        dedvs = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        quantitationTypeService = ( QuantitationTypeService ) this.getBean( "quantitationTypeService" );
        this.pedvs = ( ProcessedExpressionDataVectorCreateService ) this
                .getBean( "processedExpressionDataVectorCreateService" );
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        boolean wasProcessed = processForMissingValues( ee );

        if ( !wasProcessed ) {
            errorObjects.add( ee.getShortName() );
        } else {
            // Aduit iting is done separately.
            successObjects.add( ee.toString() );
        }

    }

    /**
     * @param ee
     * @param ad
     */
    private boolean processExperiment( ExpressionExperiment ee, ArrayDesign ad ) {

        Collection<QuantitationType> types = eeService.getQuantitationTypes( ee );

        ee = eeService.thawLite( ee );

        if ( !force && !needToRun( ee, MissingValueAnalysisEvent.class ) ) return false;

        QuantitationType previousMissingValueQt = null;
        for ( QuantitationType qType : types ) {
            if ( qType.getType() == StandardQuantitationType.PRESENTABSENT ) {
                if ( previousMissingValueQt != null ) {
                    log.warn( "More than one present/absent quantitationtype!" );
                }
                previousMissingValueQt = qType;
            }
        }

        if ( previousMissingValueQt != null && !force ) {
            log.warn( ee + " already has missing value vectors, skipping" );
            return false;
        }

        if ( force && previousMissingValueQt != null ) {
            log.info( "Removing old present/absent data" );
            dedvs.removeDataForQuantitationType( previousMissingValueQt );
            quantitationTypeService.remove( previousMissingValueQt );
        }

        log.info( "Got " + ee + ", thawing..." );

        log.info( "Computing missing value data.." );

        Collection<RawExpressionDataVector> missingValueVectors = tcmv.computeMissingValues( ee, s2n,
                this.extraMissingValueIndicators );

        if ( missingValueVectors.size() == 0 ) {
            log.warn( "No missing value vectors computed" );
            return false;
        }

        log.info( "Saving processed data vectors" );

        pedvs.computeProcessedExpressionData( ee );

        return true;
    }

    private boolean processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );

        boolean wasProcessed = false;
        for ( ArrayDesign design : arrayDesignsUsed ) {
            TechnologyType tt = design.getTechnologyType();
            if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
                log.info( ee + " uses a two-color array design, processing..." );
                if ( arrayDesignsUsed.size() == 1 ) {
                    wasProcessed = processExperiment( ee, null ); // save the slower query.
                } else {
                    wasProcessed = processExperiment( ee, design );
                }

            }
        }
        return wasProcessed;
    }
}
