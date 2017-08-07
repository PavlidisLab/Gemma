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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Switch the array design used to the merged one.
 *
 * @author pavlidis
 */
public class ExpressionExperimentPlatformSwitchCli extends ExpressionExperimentManipulatingCLI {

    private String arrayDesignName = null;
    private ArrayDesignService arrayDesignService;
    private ExpressionExperimentPlatformSwitchService serv;

    public static void main( String[] args ) {
        ExpressionExperimentPlatformSwitchCli p = new ExpressionExperimentPlatformSwitchCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }
    }

    @Override
    public String getCommandName() {
        return "switchExperimentPlatform";
    }

    @Override
    public String getShortDesc() {
        return "Switch an experiment to a different array design (usually a merged one)";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" ).withDescription(
                "Array design name (or short name) - no need to specifiy if the platforms used by the EE are merged" )
                .withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );
        this.addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( args );
        if ( exp != null ) {
            return exp;
        }

        serv = this.getBean( ExpressionExperimentPlatformSwitchService.class );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }

        }
        summarizeProcessing();
        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        arrayDesignService = this.getBean( ArrayDesignService.class );
    }

    private void processExperiment( ExpressionExperiment ee ) {

        try {
            ee = this.eeService.thawLite( ee );

            AuditTrailService ats = this.getBean( AuditTrailService.class );
            AuditEventType type = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            if ( this.arrayDesignName != null ) {
                ArrayDesign ad = locateArrayDesign( this.arrayDesignName, arrayDesignService );
                if ( ad == null ) {
                    log.error( "Unknown array design" );
                    bail( ErrorCode.INVALID_OPTION );
                }
                ad = arrayDesignService.thaw( ad );
                ee = serv.switchExperimentToArrayDesign( ee, ad );

                ats.addUpdateEvent( ee, type, "Switched to use " + ad );

            } else {
                // Identify merged platform automatically; not really recommended as it might not make the optimal choice.
                ee = serv.switchExperimentToMergedPlatform( ee );
                ats.addUpdateEvent( ee, type, "Switched to use merged array Design " );
            }

            super.successObjects.add( ee.toString() );
        } catch ( Exception e ) {
            log.error( e, e );
            super.errorObjects.add( ee + ": " + e.getMessage() );
        }
    }
}
