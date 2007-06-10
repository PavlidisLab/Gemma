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

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.security.principal.UserDetailsServiceImpl;
import ubic.gemma.util.AbstractSpringAwareCLI;

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

    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option arrayDesignOption = OptionBuilder.hasArg().withArgName( "Array design" ).withDescription(
                "Array design name (or short name)" ).withLongOpt( "array" ).create( 'a' );

        addOption( arrayDesignOption );

        addDateOption();

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

        List<AuditEvent> events = getEvents( arrayDesign, eventClass );

        if ( events.size() == 0 ) {
            return true; // always do it
        } else {
            // return true if the last time was older than the limit time.
            AuditEvent lastEvent = events.get( events.size() - 1 );
            return lastEvent.getDate().before( skipIfLastRunLaterThan );
        }
    }

    /**
     * @param arrayDesign
     * @return true if the sequences on the given array design would be equivalently treated by analyzing another array
     *         design. In the case of subsumption, this only works if the array design has been either analyzed for
     *         subsuming status. (the analysis is not done as part of this call).
     */
    protected boolean isSubsumedOrMerged( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getSubsumingArrayDesign() != null ) {
            log.info( arrayDesign + " is subsumed by " + arrayDesign.getSubsumingArrayDesign() );
            return true;
        }

        if ( arrayDesign.getMergedInto() != null ) {
            log.info( arrayDesign + " is merged into " + arrayDesign.getMergedInto() );
            return true;
        }
        return false;
    }

    /**
     * @param arrayDesign
     * @param eventClass
     * @return
     */
    private List<AuditEvent> getEvents( ArrayDesign arrayDesign, Class<? extends ArrayDesignAnalysisEvent> eventClass ) {
        List<AuditEvent> events = new ArrayList<AuditEvent>();

        for ( AuditEvent event : arrayDesign.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && eventClass.isAssignableFrom( event.getEventType().getClass() ) ) {
                events.add( event );
            }
        }
        return events;
    }

}
