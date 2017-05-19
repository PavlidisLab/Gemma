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
package ubic.gemma.model.common.auditAndSecurity;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Locale;

/**
 * @author klc (orginally generated by model)
 */
public class AuditEventValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TROUBLE_UNKNOWN_NAME = "Unknown performer";

    private String performer;

    private java.util.Date date;

    private String action;

    private String note;

    private String detail;

    private Long id;

    private AuditEventType eventType;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public AuditEventValueObject() {
    }

    public AuditEventValueObject( AuditEvent ae ) {
        if ( ae == null )
            throw new IllegalArgumentException( "Event cannot be null" );
        if ( ae.getPerformer() != null )
            this.setPerformer( ae.getPerformer().getUserName() );

        if ( ae.getAction() != null )
            this.setAction( ae.getAction().getValue() );
        this.setEventType( ae.getEventType() );
        this.setNote( ae.getNote() );
        this.setDate( ae.getDate() );
        this.setDetail( ae.getDetail() );
    }

    public String getAction() {
        return this.action;
    }

    public void setAction( String action ) {
        this.action = action;
    }

    public String getActionName() {
        if ( this.getAction().equals( "C" ) ) {
            return "Create";
        }
        return "Update";
    }

    public java.util.Date getDate() {
        return this.date;
    }

    public void setDate( java.util.Date date ) {
        this.date = date;
    }

    public String getDetail() {
        return this.detail;
    }

    public void setDetail( String detail ) {
        this.detail = detail;
    }

    public AuditEventType getEventType() {
        return this.eventType;
    }

    public void setEventType( AuditEventType eventType ) {
        this.eventType = eventType;
    }

    public String getEventTypeName() {
        return this.getEventType() == null ? "" : this.getEventType().getClass().getSimpleName();
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote( String note ) {
        this.note = note;
    }

    public String getPerformer() {
        return this.performer;
    }

    public void setPerformer( String name ) {
        this.performer = name;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if ( this.getPerformer() == null ) {
            buf.append( TROUBLE_UNKNOWN_NAME );
        } else {
            buf.append( this.getPerformer() );
        }

        try {
            buf.append( " on "
                    + DateFormat.getDateInstance( DateFormat.LONG, Locale.getDefault() ).format( this.getDate() ) );
        } catch ( Exception ex ) {
            System.err.println( "AuditEventImpl toString problem." );
            System.err.println( ex );
        }
        buf.append( ": " );

        boolean hasNote = false;

        if ( !StringUtils.isEmpty( this.getNote() ) ) {
            buf.append( this.getNote() );
            hasNote = true;
        }
        if ( !StringUtils.isEmpty( this.getDetail() ) ) {
            if ( hasNote ) {
                buf.append( " - " );
            }
            buf.append( this.getDetail() );
        }
        return buf.toString();
    }

}