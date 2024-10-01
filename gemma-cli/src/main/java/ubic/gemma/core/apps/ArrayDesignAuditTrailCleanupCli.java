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

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * work in progress
 *
 * @author paul
 */
public class ArrayDesignAuditTrailCleanupCli extends ArrayDesignSequenceManipulatingCli {

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "adATcleanup";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected void doWork() throws Exception {
        for ( ArrayDesign arrayDesign : this.getArrayDesignsToProcess() ) {
            arrayDesign = getArrayDesignService().thawLite( arrayDesign );

            List<AuditEvent> allEvents = ( List<AuditEvent> ) arrayDesign.getAuditTrail().getEvents();

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

                System.err.println( "------------------------" );
                System.err.println( k );
                for ( AuditEvent ae : evs ) {
                    System.err.println( ae.getDate() + " " + ae.getNote() );
                }

                // possibly keep subsumption and merge events no matter what. ArrayDesignSubsumeCheckEvent ArrayDesignMergeEvent

                // keep only last AlignmentBasedGeneMappingEvent, ArrayDesignRepeatAnalysisEvent, ArrayDesignGeneMappingEvent, ArrayDesignSequenceAnalysisEvent 
            }

            addSuccessObject( arrayDesign );
        }
    }

}
