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
package edu.columbia.common.auditAndSecurity;

/**
 * @see edu.columbia.common.auditAndSecurity.AuditTrail
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailImpl extends edu.columbia.common.auditAndSecurity.AuditTrail {
   /**
    * @see edu.columbia.common.auditAndSecurity.AuditTrail#updateOrCreate(edu.columbia.common.auditAndSecurity.AuditEvent)
    */
   public void addEvent( edu.columbia.common.auditAndSecurity.AuditEvent event ) {

      if ( this.getEvents().size() == 0 && event.getAction() == edu.columbia.common.auditAndSecurity.AuditAction.UPDATE )
            throw new IllegalStateException( "Attempt to add non-create Audit Event to the beginning of the trail" );

      this.getEvents().add( event );
   }

   /**
    * @see edu.columbia.common.auditAndSecurity.AuditTrail#getLast()
    */
   public edu.columbia.common.auditAndSecurity.AuditEvent getLast() {
      return ( edu.columbia.common.auditAndSecurity.AuditEvent ) this.getEvents().get( this.getEvents().size() - 1 );
   }

   /**
    * @see edu.columbia.common.auditAndSecurity.AuditTrail#getCreate()
    */
   public edu.columbia.common.auditAndSecurity.AuditEvent getCreationEvent() {
      return ( edu.columbia.common.auditAndSecurity.AuditEvent ) this.getEvents().get( 0 );
   }

}