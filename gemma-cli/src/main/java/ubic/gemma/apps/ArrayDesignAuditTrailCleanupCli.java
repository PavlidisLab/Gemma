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

package ubic.gemma.apps;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.*;

/**
 * work in progress
 *
 * @author paul
 */
public class ArrayDesignAuditTrailCleanupCli extends ArrayDesignSequenceManipulatingCli {

    @Override
    public String getCommandName() {
        return "adATcleanup";
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesignsToProcess ) {
        for ( ArrayDesign arrayDesign : arrayDesignsToProcess ) {
            arrayDesign = arrayDesignService.thawLite( arrayDesign );

            List<AuditEvent> allEvents = arrayDesign.getAuditTrail().getEvents();

            Map<Object, List<AuditEvent>> eventsByType = new HashMap<>();

            /*
             * Rules: delete all but the most recent event of each type.
             */
            for ( AuditEvent ae : allEvents ) {
                if ( ae.getEventType() == null ) {
                    if ( ae.getAction().equals( AuditAction.UPDATE ) ) {
                        if ( !eventsByType.containsKey( AuditAction.UPDATE ) ) {
                            eventsByType.put( AuditAction.UPDATE, new ArrayList<AuditEvent>() );
                        }
                        eventsByType.get( AuditAction.UPDATE ).add( ae );
                    }
                } else {
                    AuditEventType type = ae.getEventType();

                    Class<? extends AuditEventType> typeClass = type.getClass();
                    if ( !eventsByType.containsKey( typeClass ) ) {
                        eventsByType.put( typeClass, new ArrayList<AuditEvent>() );
                    }
                    eventsByType.get( typeClass ).add( ae );
                }
            }

            for ( Object k : eventsByType.keySet() ) {

                List<AuditEvent> evs = eventsByType.get( k );

                getCliContext().getErrorStream().println( "------------------------" );
                getCliContext().getErrorStream().println( k );
                for ( AuditEvent ae : evs ) {
                    getCliContext().getErrorStream().println( ae.getDate() + " " + ae.getNote() );
                }

                // possibly keep subsumption and merge events no matter what. ArrayDesignSubsumeCheckEvent ArrayDesignMergeEvent

                // keep only last AlignmentBasedGeneMappingEvent, ArrayDesignRepeatAnalysisEvent, ArrayDesignGeneMappingEvent, ArrayDesignSequenceAnalysisEvent 
            }

            addSuccessObject( arrayDesign );
        }
    }

}
