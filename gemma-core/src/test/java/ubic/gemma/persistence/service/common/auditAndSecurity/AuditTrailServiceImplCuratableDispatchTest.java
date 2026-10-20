package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.GenericCuratableDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Does {@code doAddUpdateEvent} recognise a curatable entity when it arrives as a proxy?
 * <p>
 * {@link Curatable} is introduced at {@link ExpressionExperiment}, below {@link Investigation} and
 * {@code BioAssaySet}. A Hibernate proxy is generated from the DECLARED type, so a proxy typed to
 * either supertype never implements {@code Curatable} — not even after initialization. The
 * {@code instanceof Curatable} gate therefore missed, and the curation details were silently left
 * stale while the audit row was written anyway.
 * <p>
 * Reached in production through {@code @AuditedConditional} methods that declare an
 * {@code Investigation} parameter ({@code AnnotationSetServiceImpl.attach},
 * {@code WorkflowServiceImpl.advance}): {@code AuditedAspect.findAuditable} hands the aspect
 * whatever {@code args[0]} is, and {@code DatasetsWebService} passes
 * {@code proposal.getInvestigation()} — a lazy proxy.
 *
 * @author claude
 */
public class AuditTrailServiceImplCuratableDispatchTest {

    private AuditTrailDao auditTrailDao;
    private GenericCuratableDao curatableDao;
    private AuditTrailServiceImpl service;

    @BeforeEach
    public void setUp() {
        auditTrailDao = mock( AuditTrailDao.class );
        // ensureInSession builds its requireNonNull message eagerly, so this is consulted on
        // every call regardless of whether the load succeeds.
        //noinspection unchecked,rawtypes
        when( (Class) auditTrailDao.getElementClass() ).thenReturn( AuditTrail.class );
        curatableDao = mock( GenericCuratableDao.class );
        service = new AuditTrailServiceImpl( auditTrailDao, curatableDao,
                mock( UserManager.class ), mock( SessionFactory.class ) );
    }

    /**
     * A stand-in for what Hibernate hands back for a lazy {@code Investigation} association: typed
     * to the declared supertype, resolving to the real entity only through its
     * {@link LazyInitializer}. Deliberately not a {@link Curatable}.
     */
    private static class InvestigationProxy extends Investigation implements HibernateProxy {

        private final LazyInitializer li;

        private InvestigationProxy( Object target ) {
            this.li = mock( LazyInitializer.class );
            when( li.getImplementation() ).thenReturn( target );
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return li;
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        // AbstractDescribable declares equals abstract; identity is right for a stand-in.
        @Override
        public boolean equals( Object obj ) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode( this );
        }
    }

    private ExpressionExperiment experiment() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        ee.setAuditTrail( trail() );
        return ee;
    }

    private AuditTrail trail() {
        AuditTrail t = new AuditTrail();
        t.setId( 2L );
        when( auditTrailDao.load( 2L ) ).thenReturn( t );
        return t;
    }

    @Test
    public void plainExperiment_updatesCurationDetails() {
        ExpressionExperiment ee = experiment();

        service.addUpdateEvent( ee, "note" );

        verify( curatableDao ).updateCurationDetailsFromAuditEvent( same( ee ), any( AuditEvent.class ) );
    }

    @Test
    public void experimentBehindAnInvestigationProxy_stillUpdatesCurationDetails() {
        ExpressionExperiment ee = experiment();
        InvestigationProxy proxy = new InvestigationProxy( ee );
        proxy.setId( ee.getId() );
        proxy.setAuditTrail( ee.getAuditTrail() );

        // guard: the proxy really is opaque, or this test proves nothing
        org.assertj.core.api.Assertions.assertThat( proxy ).isNotInstanceOf( Curatable.class );

        service.addUpdateEvent( proxy, "note" );

        verify( curatableDao ).updateCurationDetailsFromAuditEvent( same( ee ), any( AuditEvent.class ) );
    }

    @Test
    public void nonCuratableInvestigation_doesNotUpdateCurationDetails() {
        InvestigationProxy plain = new InvestigationProxy( null );
        plain.setId( 1L );
        plain.setAuditTrail( trail() );
        when( plain.getHibernateLazyInitializer().getImplementation() ).thenReturn( plain );

        service.addUpdateEvent( plain, "note" );

        verify( curatableDao, never() ).updateCurationDetailsFromAuditEvent( any(), any() );
    }
}
