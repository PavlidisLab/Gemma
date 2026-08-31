package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExpressionExperimentService#isSingleCell(ExpressionExperiment)} must not require its argument
 * to be attached to a session.
 * <p>
 * 🛑 This exists because a mocked test cannot catch the failure. {@code isSingleCell} reads the lazy
 * {@code characteristics} collection, and every REST-layer test mocks the service, so the transaction
 * boundary is never crossed. It shipped and 500ed every call to
 * {@code GET /datasets/{id}/sample-correlation} on production — including datasets the caller's gate was
 * never meant to touch — with "failed to lazily initialize a collection of role:
 * Investigation.characteristics: could not initialize proxy - no Session" (2026-08-31, fixed in the same
 * commit as this test).
 * <p>
 * ⚠️ {@link BaseDatabaseTest5} is {@code @Transactional} at class level, so an entity created here stays
 * attached and the test would pass with or without the fix. The {@code evict} below is what makes it a
 * real test: without it this guard cannot fail.
 */
@ContextConfiguration
public class ExpressionExperimentServiceDetachedEntityTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class Config extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }

        @Bean
        public SingleCellDimensionExperimentDao singleCellDimensionExperimentDao( SessionFactory sessionFactory ) {
            return new SingleCellDimensionExperimentDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private SessionFactory sessionFactory;

    private ExpressionExperiment ee;

    /**
     * Constructed directly rather than wired as a bean: the real impl field-injects security
     * infrastructure (AccessDecisionManager and its chain) that this behaviour does not touch, and
     * declaring all of it would be context scaffolding around a two-line invariant. isSingleCell needs
     * only the DAO — the same one ensureInSession reloads through.
     */
    private ExpressionExperimentService expressionExperimentService;

    @BeforeEach
    public void setUp() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.setTaxon( taxon );
        ee = expressionExperimentDao.create( ee );
        expressionExperimentService = new ExpressionExperimentServiceImpl( expressionExperimentDao );
    }

    @Test
    public void testIsSingleCellOnADetachedExperiment() {
        Long id = ee.getId();
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        // Reload so the collections are genuine lazy proxies. The instance built in setUp() carries an
        // already-initialized empty set, so evicting THAT one proves nothing — this test passed with and
        // without the fix until the reload was added.
        ExpressionExperiment reloaded = expressionExperimentDao.load( id );
        assertThat( reloaded ).isNotNull();
        sessionFactory.getCurrentSession().evict( reloaded );

        // Prove the precondition rather than assume it: this is the state the REST layer hands the
        // service, and if this stops throwing the test below can no longer fail.
        assertThat( Hibernate.isInitialized( reloaded.getCharacteristics() ) ).isFalse();
        assertThatThrownBy( () -> reloaded.getCharacteristics().size() )
                .isInstanceOf( LazyInitializationException.class );

        assertThatNoException()
                .isThrownBy( () -> expressionExperimentService.isSingleCell( reloaded ) );
    }
}
