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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Class encapsulating all the curation information for Curatable objects. This includes a flag to indicate whether
 * the entity is "troubled". For ExpressionExperiments as of 2019, this troubled flag will reflect whether an associated
 * ArrayDesign is troubled.
 *
 * @author tesarst
 */
public class CurationDetails extends AbstractIdentifiable {

    @Nullable
    private Date lastUpdated;
    @Nullable
    private AuditEvent lastNeedsAttentionEvent;
    private boolean needsAttention;
    @Nullable
    private AuditEvent lastTroubledEvent;
    private boolean troubled;
    @Nullable
    private AuditEvent lastNoteUpdateEvent;
    @Nullable
    private String curationNote;

    /**
     * Compares the objects type and IDs.
     *
     * @param object the object to compare this instance to.
     * @return true, if this instance and the given object are the same type and have the same ID. False otherwise.
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
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Nullable
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated( @Nullable Date lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    @Nullable
    public AuditEvent getLastNeedsAttentionEvent() {
        return lastNeedsAttentionEvent;
    }

    public void setLastNeedsAttentionEvent( @Nullable AuditEvent lastNeedsAttentionEvent ) {
        this.lastNeedsAttentionEvent = lastNeedsAttentionEvent;
    }

    public boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    @Nullable
    public AuditEvent getLastTroubledEvent() {
        return lastTroubledEvent;
    }

    public void setLastTroubledEvent( @Nullable AuditEvent lastTroubledEvent ) {
        this.lastTroubledEvent = lastTroubledEvent;
    }

    /**
     * If you are trying to check for trouble of an expression experiment, you might consider using the method
     * {@link ExpressionExperimentService#isTroubled(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     * which also checks the parenting array designs
     *
     * @return true only if these curation details trouble flag is set to true.
     */
    public boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( boolean troubled ) {
        this.troubled = troubled;
    }

    @Nullable
    public AuditEvent getLastNoteUpdateEvent() {
        return lastNoteUpdateEvent;
    }

    public void setLastNoteUpdateEvent( @Nullable AuditEvent lastNoteUpdateEvent ) {
        this.lastNoteUpdateEvent = lastNoteUpdateEvent;
    }

    @Nullable
    public String getCurationNote() {
        return curationNote;
    }

    public void setCurationNote( @Nullable String curationNote ) {
        this.curationNote = curationNote;
    }
}