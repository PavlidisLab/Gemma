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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.security.principal.UserDetailsServiceImpl;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.DateUtil;

/**
 * Aggregates functionality useful when writing CLIs that need to get an array design from the database and do something
 * with it.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractSpringAwareCLI {

    ArrayDesignService arrayDesignService;
    String arrayDesignName = null;
    AuditTrailService auditTrailService;

    String mDate = null;

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" ).withDescription(
                "Array design name (or short name)" ).withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );

        Option dateOption = OptionBuilder
                .hasArg()
                .withArgName( "mdate" )
                .withDescription(
                        "Constrain to run only on array designs with analyses older than the given date. "
                                + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                                + "If there is no record of when the analysis was last run, it will be run." ).create(
                        "mdate" );

        addOption( dateOption );

    }

    protected void unlazifyArrayDesign( ArrayDesign arrayDesign ) {
        arrayDesignService.thawLite( arrayDesign );
    }

    /**
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

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'a' ) ) {
            this.arrayDesignName = this.getOptionValue( 'a' );
        }
        if ( hasOption( "mdate" ) ) {
            this.mDate = this.getOptionValue( "mdate" );
        }

        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
    }

    protected void updateAudit( String note ) {
        ArrayDesign ad = this.locateArrayDesign( arrayDesignName );
        AuditEvent ae = AuditEvent.Factory.newInstance();
        ae.setNote( note );
        ae.setAction( AuditAction.UPDATE );
        ae.setPerformer( UserDetailsServiceImpl.getCurrentUser() );
        ad.getAuditTrail().addEvent( ae );
        arrayDesignService.update( ad );
    }

    /**
     * @return
     */
    protected Date getLimitingDate() {
        Date skipIfLastRunLaterThan = null;
        if ( StringUtils.isNotBlank( mDate ) ) {
            skipIfLastRunLaterThan = DateUtil.getRelativeDate( new Date(), mDate );
            log.info( "Analyses will be run only if last was older than " + skipIfLastRunLaterThan );
        }
        return skipIfLastRunLaterThan;
    }

    /**
     * @param skipIfLastRunLaterThan
     * @param arrayDesign
     * @param eventClass e.g., ArrayDesignSequenceAnalysisEvent.class
     * @return true if skipIfLastRunLaterThan is null, or there is no record of a previous analysis, or if the last
     *         analysis was run before skipIfLastRunLaterThan. false otherwise.
     */
    protected boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        if ( skipIfLastRunLaterThan == null ) return true;
        auditTrailService.thaw( arrayDesign );

        ArrayDesign subsumingArrayDesign = arrayDesign.getSubsumingArrayDesign();

        if ( subsumingArrayDesign != null ) {
            boolean needToRunSubsumer = needToRun( skipIfLastRunLaterThan, subsumingArrayDesign, eventClass );
            if ( !needToRunSubsumer ) {
                log
                        .info( "Subsumer  " + subsumingArrayDesign + " was run more recently than "
                                + skipIfLastRunLaterThan );
                return false;
            }
        }

        List<AuditEvent> sequenceAnalysisEvents = getEvents( arrayDesign, eventClass );

        if ( sequenceAnalysisEvents.size() == 0 ) {
            return true; // always do it
        } else {
            // return true if the last time was older than the limit time.
            AuditEvent lastEvent = sequenceAnalysisEvents.get( sequenceAnalysisEvents.size() - 1 );
            return lastEvent.getDate().before( skipIfLastRunLaterThan );
        }
    }

    /**
     * @param arrayDesign
     * @param eventClass
     * @return
     */
    private List<AuditEvent> getEvents( ArrayDesign arrayDesign, Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        List<AuditEvent> sequenceAnalysisEvents = new ArrayList<AuditEvent>();

        for ( AuditEvent event : arrayDesign.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && eventClass.isAssignableFrom( event.getEventType().getClass() ) ) {
                sequenceAnalysisEvents.add( event );
            }
        }
        return sequenceAnalysisEvents;
    }

}
