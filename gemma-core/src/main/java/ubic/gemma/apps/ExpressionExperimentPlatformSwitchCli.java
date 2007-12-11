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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Switch the array design used to the merged one.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentPlatformSwitchCli extends ExpressionExperimentManipulatingCLI {

    ArrayDesignService arrayDesignService;
    String arrayDesignName = null;

    @Override
    protected Exception doWork( String[] args ) {

        Exception exp = processCommandLine( "Switch EE to merged or selected Array Design", args );
        if ( exp != null ) {
            return exp;
        }

        ExpressionExperimentPlatformSwitchService serv = ( ExpressionExperimentPlatformSwitchService ) this
                .getBean( "expressionExperimentPlatformSwitchService" );

        ExpressionExperiment ee = this.locateExpressionExperiment( this.getExperimentShortName() );

        if ( ee == null ) {
            log.error( "Missing or unknown expression experiment" );
            bail( ErrorCode.INVALID_OPTION );
        }

        this.eeService.thawLite( ee );

        AuditTrailService auditEventService = ( AuditTrailService ) this.getBean( "auditTrailService" );
        AuditEventType type = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
        if ( this.arrayDesignName != null ) {
            ArrayDesign ad = locateArrayDesign( this.arrayDesignName );
            if ( ad == null ) {
                log.error( "Unknown array design" );
                bail( ErrorCode.INVALID_OPTION );
            }
            arrayDesignService.thawLite( ad );
            serv.switchExperimentToArrayDesign( ee, ad );
            auditEventService.addUpdateEvent( ee, type, "Switched to use " + ad );

        } else {
            serv.switchExperimentToMergedPlatform( ee );
            auditEventService.addUpdateEvent( ee, type, "Switched to use merged array Design " );
        }
        log.info( "Processing done!" );
        return null;
    }

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" ).withDescription(
                "Array design name (or short name) - no need to specifiy if the platforms used by the EE are merged" )
                .withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );
    }

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

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
    }

    /**
     * code copied from
     * 
     * @param name of the array design to find.
     * @return
     */
    protected ArrayDesign locateArrayDesign( String name ) {

        ArrayDesign arrayDesign = arrayDesignService.findArrayDesignByName( name.trim().toUpperCase() );

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }
}
