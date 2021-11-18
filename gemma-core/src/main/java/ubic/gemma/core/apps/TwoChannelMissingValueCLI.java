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

package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * CLI for computing and persisting the 'present' calls for two-channel data -- AND creates the processed data vectors
 * (saving you a step!)
 *
 * @author Paul
 */
public class TwoChannelMissingValueCLI extends ExpressionExperimentManipulatingCLI {

    private static final String MISSING_VALUE_OPTION = "mvind";
    private final Set<Double> extraMissingValueIndicators = new HashSet<>();
    private RawExpressionDataVectorService rawService;
    private ProcessedExpressionDataVectorService procService;
    private PreprocessorService preprocessorService;
    private QuantitationTypeService quantitationTypeService;
    private double s2n = TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD;
    private TwoChannelMissingValues tcmv;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        Option signal2noiseOption = Option.builder( "s" ).hasArg().argName( "Signal-to-noise" ).desc(
                "Signal to noise ratio, below which values are considered missing; default="
                        + TwoChannelMissingValues.DEFAULT_SIGNAL_TO_NOISE_THRESHOLD )
                .longOpt( "signal2noise" )
                .build();

        options.addOption( signal2noiseOption );

        Option extraMissingIndicators = Option.builder( TwoChannelMissingValueCLI.MISSING_VALUE_OPTION ).hasArg().argName( "mv indicators" )
                .desc( "Additional numeric values (comma delimited) to be considered missing values." )
                .build();

        options.addOption( extraMissingIndicators );

        super.addDateOption( options );
        super.addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 's' ) ) {
            this.s2n = this.getDoubleOptionValue( commandLine, 's' );
        }

        if ( commandLine.hasOption( "force" ) ) {
            this.force = true;
        }
        if ( commandLine.hasOption( TwoChannelMissingValueCLI.MISSING_VALUE_OPTION ) ) {
            String o = commandLine.getOptionValue( TwoChannelMissingValueCLI.MISSING_VALUE_OPTION );
            String[] vals = StringUtils.split( o, ',' );
            try {
                for ( String string : vals ) {
                    this.extraMissingValueIndicators.add( new Double( string ) );
                }
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Arguments to mvind must be numbers", e );
            }
        }
        tcmv = this.getBean( TwoChannelMissingValues.class );
        rawService = this.getBean( RawExpressionDataVectorService.class );
        procService = this.getBean( ProcessedExpressionDataVectorService.class );
        eeService = this.getBean( ExpressionExperimentService.class );
        quantitationTypeService = this.getBean( QuantitationTypeService.class );
        this.preprocessorService = this.getBean( PreprocessorService.class );
    }

    @Override
    public String getCommandName() {
        return "twoChannelMissingData";
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                this.processForMissingValues( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException(
                        "Can't do two-channel missing values on " + ee.getClass().getName() );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Computes missing value information and updates processed data vectors on two-channel microarray experiments";
    }

    private boolean processExperiment( ExpressionExperiment ee ) {

        Collection<QuantitationType> types = eeService.getQuantitationTypes( ee );

        ee = this.eeService.thawLite( ee );

        if ( !force && this.noNeedToRun( ee, MissingValueAnalysisEvent.class ) )
            return false;

        QuantitationType previousMissingValueQt = null;
        for ( QuantitationType qType : types ) {
            if ( qType.getType() == StandardQuantitationType.PRESENTABSENT ) {
                if ( previousMissingValueQt != null ) {
                    AbstractCLI.log.warn( "More than one present/absent quantitationtype!" );
                }
                previousMissingValueQt = qType;
            }
        }

        if ( previousMissingValueQt != null && !force ) {
            AbstractCLI.log.warn( ee + " already has missing value vectors, skipping" );
            return false;
        }

        if ( force && previousMissingValueQt != null ) {
            AbstractCLI.log.info( "Removing old present/absent data" );
            rawService.removeDataForQuantitationType( previousMissingValueQt );
            procService.removeDataForQuantitationType( previousMissingValueQt );
            quantitationTypeService.remove( previousMissingValueQt );
        }

        AbstractCLI.log.info( "Got " + ee + ", thawing..." );

        AbstractCLI.log.info( "Computing missing value data.." );

        Collection<RawExpressionDataVector> missingValueVectors = tcmv
                .computeMissingValues( ee, s2n, this.extraMissingValueIndicators );

        if ( missingValueVectors.size() == 0 ) {
            AbstractCLI.log.warn( "No missing value vectors computed" );
            return false;
        }

        try {
            preprocessorService.processLight( ee );
        } catch ( PreprocessingException e ) {
            AbstractCLI.log
                    .error( "Error during postprocessing of " + ee + " , make sure additional steps are completed", e );
        }

        return true;
    }

    private void processForMissingValues( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );

        boolean wasProcessed = false;
        for ( ArrayDesign design : arrayDesignsUsed ) {
            TechnologyType tt = design.getTechnologyType();
            if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
                AbstractCLI.log.info( ee + " uses a two-color array design, processing..." );
                if ( arrayDesignsUsed.size() == 1 ) {
                    wasProcessed = this.processExperiment( ee ); // save the slower query.
                } else {
                    wasProcessed = this.processExperiment( ee );
                }

            }
        }

        if ( wasProcessed ) {
            addSuccessObject( ee.toString(), "Was processed" );
        } else {
            addErrorObject( ee.getShortName(), "Was not processed" );
        }
    }
}
