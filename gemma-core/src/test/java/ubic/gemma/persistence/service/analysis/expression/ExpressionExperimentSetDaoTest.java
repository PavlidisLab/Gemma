package ubic.gemma.persistence.service.analysis.expression;

import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ContextConfiguration
public class ExpressionExperimentSetDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class CC extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentSetDao expressionExperimentSetDao( SessionFactory sessionFactory, ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentSetDaoImpl( sessionFactory, expressionExperimentDao );
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        // EE DAO now field-injects ArrayDesignDao for batched platform loads (round-2 probe #8).
        @Bean
        public ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl( sessionFactory );
        }

        // PERF_PROBE_REPORT_ROUND4 B1: EE DAO field-injects SingleCellDimensionExperimentDao.
        @Bean
        public ubic.gemma.persistence.service.expression.experiment.SingleCellDimensionExperimentDao singleCellDimensionExperimentDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.experiment.SingleCellDimensionExperimentDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentSetDao expressionExperimentSetDao;

    /**
     * This is an example of modeling a GEO super-series.
     */
    @Test
    public void testCreateSetWithAccession() {
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        sessionFactory.getCurrentSession().persist( geo );
        ExpressionExperimentSet expressionExperimentSet = new ExpressionExperimentSet();
        expressionExperimentSet.setAccession( DatabaseEntry.Factory.newInstance( "GSE000123", geo ) );
        expressionExperimentSet = expressionExperimentSetDao.create( expressionExperimentSet );
        assertNotNull( expressionExperimentSet.getId() );
        assertNotNull( expressionExperimentSet.getAccession() );
        assertNotNull( expressionExperimentSet.getAccession().getId() );
        assertThat( expressionExperimentSetDao.findByAccession( "GSE000123" ) )
                .contains( expressionExperimentSet );
        assertThat( expressionExperimentSetDao.findByAccession( "GSE000123", geo ) )
                .contains( expressionExperimentSet );
        sessionFactory.getCurrentSession().flush();

        // violates the unique key on the accession
        ExpressionExperimentSet set2 = new ExpressionExperimentSet();
        set2.setAccession( expressionExperimentSet.getAccession() );
        assertThatThrownBy( () -> expressionExperimentSetDao.create( set2 ) )
                .isInstanceOf( ConstraintViolationException.class );
        sessionFactory.getCurrentSession().clear();
    }

    /**
     * A set whose members disagree on taxon is stored with no taxon at all, which is what lets a set
     * span them. Its value object must still load: the VO query joined the taxon with an INNER JOIN,
     * so such a set produced no row and {@code loadValueObject} returned null. That is what
     * {@code POST /experiment-sets} reported as a 404 — the create had succeeded, and the read-back
     * composing the 201 body found nothing.
     */
    @Test
    public void testLoadValueObjectOfSetWithoutTaxon() {
        ExpressionExperiment ee1 = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee1 );
        ExpressionExperiment ee2 = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee2 );

        ExpressionExperimentSet mixed = new ExpressionExperimentSet();
        mixed.setName( "Reference cohort spanning taxa" );
        mixed.setDescription( "members disagree, so the set carries no taxon" );
        mixed.getExperiments().add( ee1 );
        mixed.getExperiments().add( ee2 );
        mixed.setTaxon( null );
        mixed = expressionExperimentSetDao.create( mixed );
        sessionFactory.getCurrentSession().flush();

        ExpressionExperimentSetValueObject vo = expressionExperimentSetDao.loadValueObject( mixed.getId(), true );
        assertNotNull( vo, "a set with no taxon must still load as a value object" );
        assertNull( vo.getTaxonId() );
        assertNull( vo.getTaxonName() );
        assertEquals( 2, vo.getSize().intValue() );
        assertThat( vo.getExpressionExperimentIds() ).containsExactlyInAnyOrder( ee1.getId(), ee2.getId() );

        assertThat( expressionExperimentSetDao.loadAllValueObjects( false ) )
                .as( "and it must be visible when listing sets" )
                .extracting( ExpressionExperimentSetValueObject::getId )
                .contains( mixed.getId() );
    }

    /**
     * Control: a set that DOES declare a taxon still reports it.
     */
    @Test
    public void testLoadValueObjectOfSetWithTaxon() {
        Taxon taxon = new Taxon();
        taxon.setCommonName( "human" );
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( ee );

        ExpressionExperimentSet scoped = new ExpressionExperimentSet();
        scoped.setName( "Human only" );
        scoped.setDescription( "one taxon" );
        scoped.getExperiments().add( ee );
        scoped.setTaxon( taxon );
        scoped = expressionExperimentSetDao.create( scoped );
        sessionFactory.getCurrentSession().flush();

        ExpressionExperimentSetValueObject vo = expressionExperimentSetDao.loadValueObject( scoped.getId(), true );
        assertNotNull( vo );
        assertEquals( taxon.getId(), vo.getTaxonId() );
        assertEquals( "human", vo.getTaxonName() );
        assertEquals( 1, vo.getSize().intValue() );
    }

}