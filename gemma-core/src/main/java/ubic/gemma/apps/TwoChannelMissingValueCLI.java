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
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * CLI for computing and persisting the 'present' calls for two-channel data
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
    private boolean doAll = false;
    private boolean force = false;

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

        Option doAllOption = OptionBuilder.withDescription( "Process all two-color experiments" ).create( "all" );

        addOption( doAllOption );

        Option force = OptionBuilder.withDescription(
                "Replace existing missing value data (two-color experiments only)" ).create( "force" );

        addOption( force );

        addDateOption();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Two-channel missing values", args );
        if ( err != null ) return err;

        if ( doAll ) {

            Collection<ExpressionExperiment> ees = this.getExpressionExperimentService().loadAll();
            for ( ExpressionExperiment ee : ees ) {
                try {
                    processExperiment( ee );
                } catch ( Exception e ) {
                    errorObjects.add( ee + ": " + e.getMessage() );
                    log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                }
            }

            summarizeProcessing();

        } else {
            ExpressionExperiment ee = locateExpressionExperiment( this.getExperimentShortName() );

            if ( ee == null ) {
                log.error( "No expression experiment with name " + this.getExperimentShortName() );
                bail( ErrorCode.INVALID_OPTION );
            }

            processExperiment( ee );

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void processExperiment( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.getExpressionExperimentService().getArrayDesignsUsed( ee );

        for ( ArrayDesign design : arrayDesignsUsed ) {
            TechnologyType tt = design.getTechnologyType();
            if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
                log.info( ee + " uses a two-color array design, processing..." );
                if ( arrayDesignsUsed.size() == 1 ) {
                    processExperiment( ee, null ); // save the slower query.
                } else {
                    processExperiment( ee, design );
                }
                successObjects.add( ee.toString() );
            }
        }

        AuditTrailService auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
        AuditEventType type = MissingValueAnalysisEvent.Factory.newInstance();
        auditTrailService
                .addUpdateEvent( ee, type, "Computed missing value data on array designs: " + arrayDesignsUsed );

    }

    @SuppressWarnings("unchecked")
    private void processExperiment( ExpressionExperiment ee, ArrayDesign ad ) {

        Collection<QuantitationType> types = this.getExpressionExperimentService().getQuantitationTypes( ee );

        if ( !needToRun( ee, MissingValueAnalysisEvent.class ) ) return;

        QuantitationType previousMissingValueQt = null;
        for ( QuantitationType qType : types ) {
            if ( qType.getType() == StandardQuantitationType.PRESENTABSENT ) {
                previousMissingValueQt = qType;
            }
        }

        if ( previousMissingValueQt != null && !force ) {
            log.warn( ee + " already has missing value vectors, skipping" );
            return;
        }

        if ( force && previousMissingValueQt != null ) {
            log.info( "Removing old present/absent data" );
            dedvs.removeDataForQuantitationType( ee, previousMissingValueQt );
        }

        log.info( "Got " + ee + ", thawing..." );

        log.info( "Computing missing value data.." );

        Collection<DesignElementDataVector> vectors = tcmv.computeMissingValues( ee, ad, s2n );

        PersisterHelper persisterHelper = this.getPersisterHelper();

        log.info( "Persisting " + vectors.size() + " vectors ... " );
        for ( DesignElementDataVector vector : vectors ) {
            vector.setQuantitationType( ( QuantitationType ) persisterHelper.persist( vector.getQuantitationType() ) );
        }
        dedvs.create( vectors );
        eeService.update( ee );
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

    TwoChannelMissingValues tcmv;
    DesignElementDataVectorService dedvs;
    ExpressionExperimentService eeService;

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 's' ) ) {
            this.s2n = this.getDoubleOptionValue( 's' );
        }
        if ( this.hasOption( "all" ) ) {
            this.doAll = true;
        }
        if ( hasOption( "force" ) ) {
            this.force = true;
        }
        tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
        dedvs = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }
}
