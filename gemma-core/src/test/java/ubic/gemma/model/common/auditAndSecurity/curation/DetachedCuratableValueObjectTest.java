package ubic.gemma.model.common.auditAndSecurity.curation;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.util.Thaws;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract between thawing a curatable and building its value object once the entity has
 * left the session it was loaded in.
 * <p>
 * {@code GET /rest/v2/datasets/{id}/refresh} answered 500 on every call with
 * {@code Could not initialize proxy [AuditEvent#…] - no session}: the endpoint thaws through one
 * {@code @Transactional} service method and builds the value object through another, which is two
 * sessions, and the three {@code last*Event} associations on {@link CurationDetails} are lazy and
 * were not covered by the thaw. The CLI's post-write cache refresh therefore never ran.
 * <p>
 * The two halves of the fix are checked here: {@code Thaws#thawCurationDetails} covers the three
 * associations, and the value-object constructors survive a reference that can no longer be
 * resolved. Both sides are needed — the thaw is what makes the data correct, the constructor guard
 * is what keeps the next caller who forgets it from taking the endpoint down.
 * <p>
 * The session boundary itself needs a database, so it is modelled here with a stand-in proxy whose
 * getters throw {@link LazyInitializationException} until {@link LazyInitializer#initialize()} runs
 * — the behaviour of a real proxy, in-session and out. A run against a live session would be an
 * integration test.
 */
public class DetachedCuratableValueObjectTest {

    private ExpressionExperiment ee;
    private LazyAuditEvent troubled, needsAttention, noteUpdate;

    @BeforeEach
    public void setUp() {
        // AbstractCuratableValueObject consults SecurityUtil.isUserAdmin(), which throws outright
        // on an empty SecurityContext.
        SecurityContextHolder.getContext().setAuthentication( new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList( "GROUP_ANONYMOUS" ) ) );

        troubled = new LazyAuditEvent( 24936513L, new Date( 1_700_000_000_000L ) );
        needsAttention = new LazyAuditEvent( 24936514L, new Date( 1_700_000_001_000L ) );
        noteUpdate = new LazyAuditEvent( 24936515L, new Date( 1_700_000_002_000L ) );

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1658L );
        ee.setShortName( "GSE11630" );
        ee.setName( "a dataset that has just been written and is being refreshed" );
        ee.getCurationDetails().setLastTroubledEvent( troubled );
        ee.getCurationDetails().setLastNeedsAttentionEvent( needsAttention );
        ee.getCurationDetails().setLastNoteUpdateEvent( noteUpdate );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void thawCurationDetailsInitializesEveryLastEventAssociation() {
        assertThat( Hibernate.isInitialized( ee.getCurationDetails().getLastTroubledEvent() ) ).isFalse();

        Thaws.thawCurationDetails( ee );

        assertThat( Hibernate.isInitialized( ee.getCurationDetails().getLastTroubledEvent() ) ).isTrue();
        assertThat( Hibernate.isInitialized( ee.getCurationDetails().getLastNeedsAttentionEvent() ) ).isTrue();
        assertThat( Hibernate.isInitialized( ee.getCurationDetails().getLastNoteUpdateEvent() ) ).isTrue();
    }

    @Test
    public void thawCurationDetailsToleratesAbsentEvents() {
        ee.getCurationDetails().setLastTroubledEvent( null );
        ee.getCurationDetails().setLastNeedsAttentionEvent( null );
        ee.getCurationDetails().setLastNoteUpdateEvent( null );

        assertThatCode( () -> Thaws.thawCurationDetails( ee ) ).doesNotThrowAnyException();
    }

    /**
     * The refresh endpoint's shape: thaw in one transaction, build the value object in the next.
     */
    @Test
    public void valueObjectBuiltAfterAThawCarriesTheEvents() {
        Thaws.thawCurationDetails( ee );

        ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject( ee );

        assertThat( vo.getLastTroubledEvent() ).isNotNull();
        assertThat( vo.getLastTroubledEvent().getDate() ).isEqualTo( troubled.eventDate );
        assertThat( vo.getLastNeedsAttentionEvent() ).isNotNull();
        assertThat( vo.getLastNeedsAttentionEvent().getDate() ).isEqualTo( needsAttention.eventDate );
    }

    /**
     * Same shape with the thaw missing, which is what production was doing. A dead proxy must cost
     * the field, not the response.
     */
    @Test
    public void valueObjectBuiltWithoutAThawDropsTheEventsRatherThanThrowing() {
        ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject( ee );

        assertThat( vo.getLastTroubledEvent() ).isNull();
        assertThat( vo.getLastNeedsAttentionEvent() ).isNull();
        assertThat( vo.getLastNoteUpdateEvent() ).isNull();
        // everything that does not depend on the dead proxies is still there
        assertThat( vo.getShortName() ).isEqualTo( "GSE11630" );
    }

    @Test
    public void curationDetailsValueObjectBuiltWithoutAThawDropsTheEventsRatherThanThrowing() {
        CurationDetailsValueObject vo = new CurationDetailsValueObject( ee.getCurationDetails() );

        assertThat( vo.getLastTroubledEvent() ).isNull();
        assertThat( vo.getLastNeedsAttentionEvent() ).isNull();
    }

    @Test
    public void curationDetailsValueObjectBuiltAfterAThawCarriesTheEvents() {
        Thaws.thawCurationDetails( ee );

        CurationDetailsValueObject vo = new CurationDetailsValueObject( ee.getCurationDetails() );

        assertThat( vo.getLastTroubledEvent() ).isNotNull();
        assertThat( vo.getLastTroubledEvent().getDate() ).isEqualTo( troubled.eventDate );
    }

    /**
     * An {@link AuditEvent} reference that behaves like a Hibernate proxy: readable only once
     * {@link LazyInitializer#initialize()} has run, which is what the thaw does while a session is
     * still open. Reading it before that raises the same {@link LazyInitializationException} a
     * detached proxy raises. {@code getId()} answers without initializing, as a real proxy does.
     */
    private static final class LazyAuditEvent extends AuditEvent implements HibernateProxy {

        private final AtomicBoolean initialized = new AtomicBoolean( false );
        private final LazyInitializer lazyInitializer;
        private final Date eventDate;

        private LazyAuditEvent( long id, Date eventDate ) {
            this.eventDate = eventDate;
            setId( id );
            this.lazyInitializer = mock( LazyInitializer.class );
            when( lazyInitializer.isUninitialized() ).thenAnswer( i -> !initialized.get() );
            doAnswer( i -> {
                initialized.set( true );
                return null;
            } ).when( lazyInitializer ).initialize();
        }

        private void requireInitialized() {
            if ( !initialized.get() ) {
                throw new LazyInitializationException( "Could not initialize proxy ["
                        + AuditEvent.class.getName() + "#" + getId() + "] - no session" );
            }
        }

        @Override
        public Date getDate() {
            requireInitialized();
            return eventDate;
        }

        @Override
        public AuditAction getAction() {
            requireInitialized();
            return AuditAction.UPDATE;
        }

        @Override
        public String getNote() {
            requireInitialized();
            return null;
        }

        @Override
        public String getDetail() {
            requireInitialized();
            return null;
        }

        @Override
        public User getPerformer() {
            requireInitialized();
            return null;
        }

        @Override
        public AuditEventType getEventType() {
            requireInitialized();
            return null;
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return lazyInitializer;
        }
    }
}
