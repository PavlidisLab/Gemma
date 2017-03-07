/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

/**
 * Represents the basic status of an AbstractAuditable, with possible information about state in workflows etc.
 */
public class CurationDetails implements java.io.Serializable {

    private static final long serialVersionUID = -3418540112052921387L;

    private Long id;

    private AuditEvent lastNeedsAttentionEvent;

    private Boolean needsAttention;

    private AuditEvent lastTroubledEvent;

    private Boolean troubled;

    private AuditEvent lastNoteUpdateEvent;

    private String curationNote;

    public CurationDetails() {
    }

    public CurationDetails( AuditEvent lastNeedsAttentionEvent, Boolean needsAttention, AuditEvent lastTroubledEvent,
            Boolean troubled, AuditEvent lastNoteUpdateEvent, String curationNote ) {
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
        this.needsAttention = needsAttention;
        this.lastTroubledEvent = lastTroubledEvent;
        this.troubled = troubled;
        this.lastNoteUpdateEvent = lastNoteUpdateEvent;
        this.curationNote = curationNote;
    }

    /**
     * Returns <code>true</code> if the argument is an Status instance and all identifiers for this entity equal the
     * identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof CurationDetails ) ) {
            return false;
        }
        final CurationDetails that = ( CurationDetails ) object;
        return !( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) );
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public AuditEvent getLastNeedsAttentionEvent() {
        return lastNeedsAttentionEvent;
    }

    public void setLastNeedsAttentionEvent( AuditEvent lastNeedsAttentionEvent ) {
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
    }

    public Boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    public AuditEvent getLastTroubledEvent() {
        return lastTroubledEvent;
    }

    public void setLastTroubledEvent( AuditEvent lastTroubledEvent ) {
        this.lastTroubledEvent = lastTroubledEvent;
    }

    public Boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public AuditEvent getLastNoteUpdateEvent() {
        return lastNoteUpdateEvent;
    }

    public void setLastNoteUpdateEvent( AuditEvent lastNoteUpdateEvent ) {
        this.lastNoteUpdateEvent = lastNoteUpdateEvent;
    }

    public String getCurationNote() {
        return curationNote;
    }

    public void setCurationNote( String curationNote ) {
        this.curationNote = curationNote;
    }
}