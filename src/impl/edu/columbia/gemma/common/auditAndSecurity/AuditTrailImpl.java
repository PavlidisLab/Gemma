/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.common.auditAndSecurity;

import java.util.ArrayList;
import java.util.Date;

/**
 * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailImpl extends edu.columbia.gemma.common.auditAndSecurity.AuditTrail {

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#addEvent(edu.columbia.gemma.common.auditAndSecurity.AuditEvent)
    */
   public void addEvent( AuditEvent event ) {
      if ( event == null ) throw new IllegalArgumentException( "AuditEvent cannot be null" );
      assert this.getEvents() != null;
      this.getEvents().add( event );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#getLast()
    */
   public AuditEvent getLast() {
      assert this.getEvents() != null;
      return ( AuditEvent ) this.getEvents().get( this.getEvents().size() - 1 );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#getCreationEvent()
    */
   public AuditEvent getCreationEvent() {
      assert this.getEvents() != null;
      return ( AuditEvent ) this.getEvents().get( 0 );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#start()
    */
   public void start() {
      this.start( null );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#start(java.lang.String)
    */
   public void start( String note ) {
      this.init(); // ensure that this is the first event.
      AuditEvent newEvent = AuditEvent.Factory.newInstance();
      newEvent.setAction( AuditAction.CREATE );
      newEvent.setDate( new Date() );
      if ( note != null ) newEvent.setNote( note );

      this.addEvent( newEvent );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#update()
    */
   public void update() {
      this.update( null );
   }

   /**
    * @see edu.columbia.gemma.common.auditAndSecurity.AuditTrail#update(java.lang.String)
    */
   public void update( String note ) {
      assert this.getEvents() != null;
      assert this.getEvents().size() > 0; // can't have update as the first
      // event.

      AuditEvent newEvent = AuditEvent.Factory.newInstance();
      newEvent.setAction( AuditAction.UPDATE );
      newEvent.setDate( new Date() );
      if ( note != null ) newEvent.setNote( note );

      this.addEvent( newEvent );
   }

   /**
    * If this AudiTrail is empty or null, initialize it. Otherwise, clear any events.
    */
   private void init() {
      if ( trailIsNull() ) {
         this.setEvents( new ArrayList() );
      } else {
         this.getEvents().clear();
      }
   }

   /**
    * Check whether the list of events doesn't exist yet.
    * 
    * @return
    */
   private boolean trailIsNull() {
      return this.getEvents() == null;
   }

}