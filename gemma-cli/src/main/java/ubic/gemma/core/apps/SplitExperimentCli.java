/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.SplitExperimentService;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

import java.util.Collection;

/**
 * Split an experiment into parts based on an experimental factor
 *
 * @author paul
 */
public class SplitExperimentCli extends ExpressionExperimentManipulatingCLI {

    /**
     *
     */
    private static final String FACTOR_OPTION = "factor";

    @Autowired
    private SplitExperimentService serv;
    @Autowired
    private ExperimentalFactorService efs;

    private Long factorId;
    private String factorName;

    public SplitExperimentCli() {
        setSingleExperimentMode();
    }

    @Override
    public String getCommandName() {
        return "splitExperiment";
    }

    @Override
    public String getShortDesc() {
        return "Split an experiment into parts based on an experimental factor";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( Option.builder( FACTOR_OPTION ).hasArg()
                .desc( "ID numbers, categories or names of the factor to use, with spaces replaced by underscores (must not be 'batch')" )
                .build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( !commandLine.hasOption( FACTOR_OPTION ) ) {
            throw new IllegalArgumentException( "Please specify the factor" );
        }
        String rawFactor = commandLine.getOptionValue( FACTOR_OPTION );
        try {
            this.factorId = Long.parseLong( rawFactor );
        } catch ( NumberFormatException e ) {
            this.factorName = rawFactor;
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = this.eeService.thawLite( ee );
        ExperimentalFactor splitOn = this.guessFactor( ee );
        serv.split( ee, splitOn, true );
    }

    /**
     * Adapted from code in DifferentialExpressionAnalysisCli
     */
    private ExperimentalFactor guessFactor( ExpressionExperiment ee ) {
        if ( ee.getExperimentalDesign() == null ) {
            throw new IllegalStateException( ee + " does not have an experimental design, it cannot be split on a factor." );
        }

        if ( this.factorName != null ) {

            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {

                // has already implemented way of figuring out human-friendly name of factor value.
                ExperimentalFactorValueObject fvo = new ExperimentalFactorValueObject( experimentalFactor );

                // do not attempt to switch on 'batch'
                if ( BatchInfoPopulationServiceImpl.isBatchFactor( experimentalFactor ) ) {
                    continue;
                }

                if ( factorName.contains( experimentalFactor.getName().replaceAll( " ", "_" ) ) ) {
                    return experimentalFactor;
                } else if ( fvo.getCategory() != null && factorName
                        .contains( fvo.getCategory().replaceAll( " ", "_" ) ) ) {
                    return experimentalFactor;
                }
            }

            throw new IllegalArgumentException( "Didn't find factor the provided factor name " );

        }

        ExperimentalFactor factor = efs.loadOrFail( factorId );
        factor = efs.thaw( factor );
        if ( factor == null ) {
            throw new IllegalArgumentException( "No factor for id=" + factorId );
        }
        if ( !factor.getExperimentalDesign().equals( ee.getExperimentalDesign() ) ) {
            throw new IllegalArgumentException( "Factor with id=" + factorId + " does not belong to " + ee );
        }

        if ( BatchInfoPopulationServiceImpl.isBatchFactor( factor ) ) {
            throw new IllegalArgumentException( "Selected factor looks like batch, split not allowed, choose another factor instead" );
        }

        return factor;

    }

}
