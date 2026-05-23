package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.core.security.util.SecurityUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;

/**
 * Created by tesarst on 07/03/17.
 * Abstract curatable value object that provides variables and methods for data stored in CurationDetails objects on
 * curatable objects.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Slf4j
@Getter
@Setter
public abstract class AbstractCuratableValueObject<C extends Curatable> extends IdentifiableValueObject<C> {

    private static final String TROUBLE_DETAILS_NONE = "No trouble details provided.";

    private Date lastUpdated;
    private boolean troubled = false;
    private AuditEventValueObject lastTroubledEvent;
    private boolean needsAttention = false;
    private AuditEventValueObject lastNeedsAttentionEvent;
    private String curationNote;
    private AuditEventValueObject lastNoteUpdateEvent;

    /**
     * Required when using the implementing classes as a spring beans.
     */
    protected AbstractCuratableValueObject() {
        super();
    }

    protected AbstractCuratableValueObject( Long id ) {
        super( id );
    }

    protected AbstractCuratableValueObject( C curatable ) {
        this( curatable, false );
    }

    /**
     * Variant of {@link #AbstractCuratableValueObject(Curatable)} that skips reading the three
     * {@code last*Event} associations off {@code CurationDetails}. Use this when a caller is
     * batch-hydrating the events post-fetch (see {@code ExpressionExperimentDaoImpl}'s value-object
     * transformer): passing {@code skipEvents=true} keeps the three lazy-proxy associations
     * untouched at VO-construction time, avoiding ~3 SELECTs per row, and the caller fills in the
     * three {@link AuditEventValueObject} fields via the inherited setters once a batched
     * prefetch is available.
     */
    protected AbstractCuratableValueObject( C curatable, boolean skipEvents ) {
        super( curatable );
        this.lastUpdated = curatable.getCurationDetails().getLastUpdated();
        this.troubled = curatable.getCurationDetails().getTroubled();
        this.needsAttention = curatable.getCurationDetails().getNeedsAttention();
        if ( !skipEvents ) {
            this.lastTroubledEvent = curatable.getCurationDetails().getLastTroubledEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastTroubledEvent() ) : null;
            this.lastNeedsAttentionEvent = curatable.getCurationDetails().getLastNeedsAttentionEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastNeedsAttentionEvent() ) : null;
        }
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curatable.getCurationDetails().getCurationNote();
            if ( !skipEvents ) {
                this.lastNoteUpdateEvent = curatable.getCurationDetails().getLastNoteUpdateEvent() != null ? new AuditEventValueObject( curatable.getCurationDetails().getLastNoteUpdateEvent() ) : null;
            }
        }
    }

    /**
     * Apply a batched {@link LastEventTriple} onto this VO. Wraps non-null {@link AuditEvent}
     * references in {@link AuditEventValueObject} matching the per-row constructor's behaviour;
     * respects the same admin-only gate on {@code lastNoteUpdateEvent} that the per-row ctor uses.
     * Called by callers that constructed this VO with {@code skipEvents=true}.
     */
    public void applyLastEventTriple( @Nullable LastEventTriple triple ) {
        if ( triple == null ) {
            return;
        }
        this.lastTroubledEvent = triple.troubled() != null ? new AuditEventValueObject( triple.troubled() ) : null;
        this.lastNeedsAttentionEvent = triple.needsAttention() != null ? new AuditEventValueObject( triple.needsAttention() ) : null;
        if ( SecurityUtil.isUserAdmin() ) {
            this.lastNoteUpdateEvent = triple.noteUpdate() != null ? new AuditEventValueObject( triple.noteUpdate() ) : null;
        }
    }

    /**
     * Pre-fetched triple of "last X event" associations off a {@code CurationDetails}, batch-loaded
     * per page of curatable entities to avoid the N×3-SELECT proxy-init cost of letting each VO
     * constructor touch the three lazy proxies on its own. See
     * {@code ExpressionExperimentDaoImpl#loadLastEventsByExperimentIds(Collection)}.
     */
    public record LastEventTriple( @Nullable AuditEvent troubled, @Nullable AuditEvent needsAttention, @Nullable AuditEvent noteUpdate ) {
    }

    /**
     * Copy constructor.
     */
    protected AbstractCuratableValueObject( AbstractCuratableValueObject<C> curatable ) {
        super( curatable );
        this.lastUpdated = curatable.getLastUpdated();
        this.troubled = curatable.getTroubled();
        this.lastTroubledEvent = curatable.getLastTroubledEvent();
        this.needsAttention = curatable.getNeedsAttention();
        this.lastNeedsAttentionEvent = curatable.getLastNeedsAttentionEvent();
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curatable.getCurationNote();
            this.lastNoteUpdateEvent = curatable.getLastNoteUpdateEvent();
        }
    }

    public boolean getTroubled() {
        return troubled;
    }

    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public boolean getNeedsAttention() {
        return needsAttention;
    }

    public void setNeedsAttention( Boolean needsAttention ) {
        this.needsAttention = needsAttention;
    }

    /**
     * @return a string describing the current trouble of this object. In this case, only the trouble of the Expression
     * Experiment are described. If you also need to include the Array Design trouble info, use
     * {@link ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject}
     */
    @SuppressWarnings("unused") // Used in front end
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }

    public String getTroubleDetails( boolean htmlEscape ) {
        String details = AbstractCuratableValueObject.TROUBLE_DETAILS_NONE;
        if ( this.getTroubled() ) {
            if ( this.getLastTroubledEvent() == null ) {
                log.warn( "Curatable object is troubled, but has no trouble event! Id: " + this.getId() );
            } else {
                details = this.getLastTroubledEvent().toString();
            }
        }

        return htmlEscape ? StringEscapeUtils.escapeHtml4( details ) : details;
    }
}
