package ubic.gemma.persistence.service.common.description;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.common.description.PublicationAssociationStatus;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the refusal message {@link PublicationAssociationServiceImpl} raises when a standing
 * rejection blocks an acceptance.
 * <p>
 * The proxy case is the one that matters. Every entry point on this service takes an
 * {@link Investigation} — the mapped root of the hierarchy — and both associations that hold one
 * ({@code PublicationAssociation.investigation}, {@code AnnotationSet.investigation}) are
 * {@code @ManyToOne(LAZY)}. A caller that reached the experiment through either hands the service an
 * {@code Investigation$HibernateProxy}, which is an instance of no subclass, so the {@code instanceof}
 * that recovers the short name fell through and the message named the dataset by database id.
 *
 * @author claude
 */
public class PublicationAssociationServiceImplTest {

    private PublicationAssociationDao dao;
    private PublicationAssociationServiceImpl service;

    /**
     * Stands in for what Hibernate hands back for a lazy {@link Investigation} association: an
     * {@link Investigation} that is an instance of no concrete subclass and resolves to the target
     * only through its {@link LazyInitializer}.
     * <p>
     * A real class rather than a Mockito mock on purpose. {@code Hibernate.unproxy} does not test
     * {@code instanceof HibernateProxy}; it goes through {@code asHibernateProxy()}, a default method
     * that returns {@code this}. A mock stubs that to {@code null} and the proxy silently passes
     * through unresolved — which looks exactly like the bug this test guards.
     */
    private static class LazyProxy extends Investigation implements HibernateProxy {

        private final LazyInitializer li;

        private LazyProxy( Investigation target ) {
            this.li = mock( LazyInitializer.class );
            when( li.getImplementation() ).thenReturn( target );
            setId( target.getId() );
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return li;
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        // AbstractDescribable declares equals abstract; identity is the right answer for a stand-in.
        @Override
        public boolean equals( Object obj ) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode( this );
        }
    }

    private static ExpressionExperiment ee( Long id, String shortName ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        ee.setShortName( shortName );
        return ee;
    }

    private static BibliographicReference publication( Long id ) {
        BibliographicReference ref = new BibliographicReference();
        ref.setId( id );
        return ref;
    }

    @BeforeEach
    public void setUp() {
        dao = mock( PublicationAssociationDao.class );
        service = new PublicationAssociationServiceImpl( dao );
    }

    /**
     * A curator rejection standing at rank 40 refuses an agent acceptance at rank 20.
     */
    private void standingCuratorRejection( BibliographicReference pub ) {
        PublicationAssociation held = new PublicationAssociation();
        held.setId( 5L );
        held.setPublication( pub );
        held.setStatus( PublicationAssociationStatus.REJECTED );
        held.setSource( PublicationAssociationSource.CURATOR );
        when( dao.findByInvestigationAndPublication( any(), any() ) ).thenReturn( held );
    }

    @Test
    public void conflictMessage_namesTheExperimentByShortName() {
        BibliographicReference pub = publication( 9L );
        standingCuratorRejection( pub );

        assertThatThrownBy( () -> service.assertAccepted( ee( 27929L, "GSE227854" ),
                new PublicationAssertion( pub, PublicationAssociationSource.AGENT ),
                PublicationAssociationRole.PRIMARY ) )
                .isInstanceOf( PublicationAssociationConflictException.class )
                .hasMessageContaining( "GSE227854 (id=27929)" );
    }

    @Test
    public void conflictMessage_namesTheExperimentByShortName_evenThroughALazyProxy() {
        BibliographicReference pub = publication( 9L );
        standingCuratorRejection( pub );
        Investigation proxy = new LazyProxy( ee( 27929L, "GSE227854" ) );
        // guard: the proxy really is opaque, otherwise this test proves nothing
        assertThat( proxy ).isNotInstanceOf( ExpressionExperiment.class );

        assertThatThrownBy( () -> service.assertAccepted( proxy,
                new PublicationAssertion( pub, PublicationAssociationSource.AGENT ),
                PublicationAssociationRole.PRIMARY ) )
                .isInstanceOf( PublicationAssociationConflictException.class )
                .hasMessageContaining( "GSE227854 (id=27929)" );
    }

    /**
     * An investigation that is not an experiment, or one with no short name yet, still has to produce a
     * usable message rather than blowing up.
     */
    @Test
    public void conflictMessage_fallsBackToTheIdWhenThereIsNoShortName() {
        BibliographicReference pub = publication( 9L );
        standingCuratorRejection( pub );

        assertThatThrownBy( () -> service.assertAccepted( ee( 27929L, null ),
                new PublicationAssertion( pub, PublicationAssociationSource.AGENT ),
                PublicationAssociationRole.PRIMARY ) )
                .isInstanceOf( PublicationAssociationConflictException.class )
                .hasMessageContaining( "experiment id=27929" );
    }
}
