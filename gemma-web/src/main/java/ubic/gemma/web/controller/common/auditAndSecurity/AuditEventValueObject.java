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
package ubic.gemma.web.controller.common.auditAndSecurity;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * @author klc
 * @version $Id$
 */
public class AuditEventValueObject implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */

    public AuditEventValueObject() {
    }

    private String performer;

    public AuditEventValueObject( AuditEvent ae ) {
        this.setPerformer( ae.getPerformer().getName() );
        this.setAction( ae.getAction().getValue() );
        this.setEventType( ae.getEventType() );
        this.setNote( ae.getNote() );
        this.setDate( ae.getDate() );
        this.setDetail( ae.getDetail() );
    }

    public String getActionName() {
        if ( this.getAction().equals( AuditAction.CREATE ) ) {
            return "Create";
        } else {
            return "Update";
        }
    }

    public String getEventTypeName() {
        return this.getEventType() == null ? "" : this.getEventType().getClass().getSimpleName();
    }

    public void setPerformer( String name ) {
        this.performer = name;
    }

    public String getPerformer() {
        return this.performer;
    }

    private java.util.Date date;

    /**
     * 
     */
    public java.util.Date getDate() {
        return this.date;
    }

    public void setDate( java.util.Date date ) {
        this.date = date;
    }

    private java.lang.String action;

    /**
     * 
     */
    public java.lang.String getAction() {
        return this.action;
    }

    public void setAction( java.lang.String action ) {
        this.action = action;
    }

    private java.lang.String note;

    /**
     * <p>
     * <p>
     * An annotation about the action taken.
     * </p>
     * </p>
     */
    public java.lang.String getNote() {
        return this.note;
    }

    public void setNote( java.lang.String note ) {
        this.note = note;
    }

    private java.lang.String detail;

    /**
     * 
     */
    public java.lang.String getDetail() {
        return this.detail;
    }

    public void setDetail( java.lang.String detail ) {
        this.detail = detail;
    }

    private java.lang.Long id;

    /**
     * 
     */
    public java.lang.Long getId() {
        return this.id;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    private ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType eventType;

    /**
     * 
     */
    public ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType getEventType() {
        return this.eventType;
    }

    public void setEventType( ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType eventType ) {
        this.eventType = eventType;
    }

}
