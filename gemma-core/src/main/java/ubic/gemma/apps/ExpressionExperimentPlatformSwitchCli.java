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

import ubic.gemma.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Switch the array design used to the merged one.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentPlatformSwitchCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExpressionExperimentPlatformSwitchCli p = new ExpressionExperimentPlatformSwitchCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            log.fatal( e, e );
        }
    }

    ArrayDesignService arrayDesignService;
    String arrayDesignName = null;

    ExpressionExperimentPlatformSwitchService serv;

    @Override
    public String getShortDesc() {
        return "Switch an experiment to a different array design (usually a merged one)";
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option arrayDesignOption = OptionBuilder
                .hasArg()
                .withArgName( "Array design" )
                .withDescription(
                        "Array design name (or short name) - no need to specifiy if the platforms used by the EE are merged" )
                .withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );
        this.addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( "Switch EE to merged or selected Array Design", args );
        if ( exp != null ) {
            return exp;
        }

        serv = ( ExpressionExperimentPlatformSwitchService ) this.getBean( "expressionExperimentPlatformSwitchService" );

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

    /**
     * code copied from
     * 
     * @param name of the array design to find.
     * @return
     */
    protected ArrayDesign locateArrayDesign( String name ) {

        ArrayDesign arrayDesign = arrayDesignService.findByName( name.trim().toUpperCase() );

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
    }

    private void processExperiment( ExpressionExperiment ee ) {

        try {
            ee = this.eeService.thawLite( ee );

            AuditTrailService ats = ( AuditTrailService ) this.getBean( "auditTrailService" );
            AuditEventType type = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            if ( this.arrayDesignName != null ) {
                ArrayDesign ad = locateArrayDesign( this.arrayDesignName );
                if ( ad == null ) {
                    log.error( "Unknown array design" );
                    bail( ErrorCode.INVALID_OPTION );
                }
                ad = arrayDesignService.thaw( ad );
                serv.switchExperimentToArrayDesign( ee, ad );

                ats.addUpdateEvent( ee, type, "Switched to use " + ad );

            } else {
                serv.switchExperimentToMergedPlatform( ee );
                ats.addUpdateEvent( ee, type, "Switched to use merged array Design " );
            }

            super.successObjects.add( ee.toString() );
        } catch ( Exception e ) {
            log.error( e, e );
            super.errorObjects.add( ee + ": " + e.getMessage() );
        }
    }
}
