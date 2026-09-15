package ubic.gemma.model.common.auditAndSecurity.curation;

import ubic.gemma.core.security.util.SecurityUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.LazyInitializationException;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

import java.util.Date;
import java.util.function.Supplier;

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
            this.lastTroubledEvent = lastEventVo( () -> curatable.getCurationDetails().getLastTroubledEvent(), curatable, "lastTroubledEvent" );
            this.lastNeedsAttentionEvent = lastEventVo( () -> curatable.getCurationDetails().getLastNeedsAttentionEvent(), curatable, "lastNeedsAttentionEvent" );
        }
        if ( SecurityUtil.isUserAdmin() ) {
            this.curationNote = curatable.getCurationDetails().getCurationNote();
            if ( !skipEvents ) {
                this.lastNoteUpdateEvent = lastEventVo( () -> curatable.getCurationDetails().getLastNoteUpdateEvent(), curatable, "lastNoteUpdateEvent" );
            }
        }
    }

    /**
     * Read one of the three {@code last*Event} associations off a {@link CurationDetails} and wrap it
     * in an {@link AuditEventValueObject}, tolerating a reference that can no longer be resolved
     * because the owning entity has left the session it was loaded in.
     * <p>
     * A value object built from a detached entity hits a dead {@link AuditEvent} proxy for any of the
     * three, because they are lazy and are not covered by simply initializing the
     * {@link CurationDetails} itself. That turned {@code GET /datasets/{id}/refresh} into a 500 on
     * every call. The thaw now covers them ({@code Thaws#thawCurationDetails}); this is the backstop
     * for a caller that builds the VO outside the transaction that thawed — the field comes back
     * {@code null}, matching how every other lazy association is treated in
     * {@code ExpressionExperimentValueObject}, and the warning names the entity so the missing thaw
     * is findable.
     * <p>
     * Note that testing {@code Hibernate.isInitialized} instead would be wrong here: an
     * uninitialized-but-still-attached proxy is the normal case for an in-session VO build, and
     * skipping on that would drop the events from every ordinary response.
     */
    @Nullable
    static AuditEventValueObject lastEventVo( Supplier<AuditEvent> ref, Object owner, String field ) {
        try {
            AuditEvent ae = ref.get();
            return ae != null ? new AuditEventValueObject( ae ) : null;
        } catch ( LazyInitializationException e ) {
            log.warn( "Could not read {} of {}; it was built outside the session that loaded it. "
                    + "Thaw the curation details (Thaws#thawCurationDetails) in the transaction that builds the value object.", field, owner, e );
            return null;
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
