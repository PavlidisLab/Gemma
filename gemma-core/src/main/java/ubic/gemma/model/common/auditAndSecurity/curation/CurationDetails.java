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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.lang.Nullable;
import java.util.Date;

/**
 * Class encapsulating all the curation information for Curatable objects. This includes a flag to indicate whether
 * the entity is "troubled". For ExpressionExperiments as of 2019, this troubled flag will reflect whether an associated
 * ArrayDesign is troubled.
 *
 * @author tesarst
 */
@Entity
@Table(name = "CURATION_DETAILS", indexes = @Index(name = "TROUBLED_IX", columnList = "TROUBLED"))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CurationDetails extends AbstractIdentifiable {

    @Nullable
    @Column(name = "LAST_UPDATED", columnDefinition = "DATETIME(3)")
    private Date lastUpdated;
    // cascade=all on these events causes problems with EE deletion; however, making them 'none' exposed other issues.
    // lastXEvent associations are loaded lazily; eager-join made every EE hydration pay the cost regardless of whether the caller reads the events.
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ATTENTION_AUDIT_EVENT_FK", unique = true, columnDefinition = "BIGINT")
    private AuditEvent lastNeedsAttentionEvent;
    @Column(name = "NEEDS_ATTENTION", nullable = false, columnDefinition = "TINYINT")
    private boolean needsAttention;
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "TROUBLE_AUDIT_EVENT_FK", unique = true, columnDefinition = "BIGINT")
    private AuditEvent lastTroubledEvent;
    @Column(name = "TROUBLED", nullable = false, columnDefinition = "TINYINT")
    private boolean troubled;
    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "NOTE_AUDIT_EVENT_FK", unique = true, columnDefinition = "BIGINT")
    private AuditEvent lastNoteUpdateEvent;
    @Nullable
    @Column(name = "NOTE", columnDefinition = "VARCHAR(255)")
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