/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author pavlidis
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public class AuditTrailImpl extends AuditTrail {

    private static final long serialVersionUID = 5316032533526337630L;

    @Override
    public void addEvent( AuditEvent event ) {
        if ( event == null )
            throw new IllegalArgumentException( "AuditEvent cannot be null" );
        assert this.getEvents() != null;
        this.getEvents().add( event );
    }

    @Override
    public AuditEvent getCreationEvent() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        AuditEvent auditEvent = ( ( List<AuditEvent> ) this.getEvents() ).get( 0 );

        assert auditEvent.getAction().equals( AuditAction.CREATE );

        return auditEvent;
    }

    @Override
    public AuditEvent getLast() {
        assert this.getEvents() != null;
        if ( this.getEvents().size() == 0 ) {
            return null;
        }
        return ( ( List<AuditEvent> ) this.getEvents() ).get( this.getEvents().size() - 1 );
    }

    @Override
    public void read() {
        this.read( null );

    }

    @Override
    public void read( String note ) {
        this.read( note, null );

    }

    @Override
    public void read( String note, User actor ) {
        assert this.getEvents() != null;
        assert this.getEvents().size() > 0; // can't have read as the first
        // event.

        AuditEvent newEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.READ, note, null, actor, null );

        this.addEvent( newEvent );

    }

    @Override
    public void start() {
        this.start( null );
    }

    @Override
    public void start( String note ) {
        this.start( note, null );
    }

    @Override
    public void start( String note, User actor ) {
        this.initialize(); // ensure that this is the first event.
        AuditEvent newEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.CREATE, note, null, actor, null );

        this.addEvent( newEvent );
    }

    @Override
    public void update() {
        this.update( null );
    }

    @Override
    public void update( String note ) {
        this.update( note, null );
    }

    @Override
    public void update( String note, User actor ) {
        assert this.getEvents() != null;
        assert this.getEvents().size() > 0; // can't have update as the first
        // event.

        AuditEvent newEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, note, null, actor, null );

        this.addEvent( newEvent );
    }

    /**
     * If this AuditTrail's list is empty or null, initialize it. Otherwise, clear any events.
     */
    private void initialize() {
        if ( trailIsNull() ) {
            this.setEvents( new ArrayList<AuditEvent>() );
        } else {
            this.getEvents().clear();
        }
    }

    /**
     * Check whether the list of events doesn't exist yet.
     */
    private boolean trailIsNull() {
        return this.getEvents() == null;
    }

}