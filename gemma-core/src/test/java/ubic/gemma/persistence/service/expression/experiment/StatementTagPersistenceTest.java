package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A statement-shaped experiment tag must survive the round trip as a {@link Statement}.
 * <p>
 * {@code Investigation.characteristics} is a {@code Set<Characteristic>} and {@link Statement} is a
 * subclass with {@code @DiscriminatorValue("Statement")}, so persisting one relies on Hibernate using the
 * runtime type rather than the collection's declared type. Nothing covered that: statements were only ever
 * written under a FactorValue, whose collection is typed {@code Set<Statement>} directly. Written while
 * investigating a report of tag statements being accepted and dropped (cab, 2026-08-31) — this layer was
 * exonerated, and the coverage is worth keeping either way.
 */
@ContextConfiguration
public class StatementTagPersistenceTest extends BaseDatabaseTest5 {

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

    @Test
    public void testAStatementTagPersistsAsAStatement() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.setTaxon( taxon );
        ee = expressionExperimentDao.create( ee );

        Statement st = Statement.Factory.newInstance();
        st.setCategory( "cell type" );
        st.setSubject( "Schwann cell" );
        st.setPredicate( "derives from part of" );
        st.setObject( "sciatic nerve" );
        ee.getCharacteristics().add( st );
        expressionExperimentDao.update( ee );
        sessionFactory.getCurrentSession().flush();
        Long eeId = ee.getId();
        sessionFactory.getCurrentSession().clear();

        ExpressionExperiment reloaded = expressionExperimentDao.load( eeId );
        assertThat( reloaded ).isNotNull();
        assertThat( reloaded.getCharacteristics() ).hasSize( 1 );
        Characteristic persisted = reloaded.getCharacteristics().iterator().next();
        // The subclass, not just the fields: a plain Characteristic here means the triple was silently lost.
        assertThat( persisted ).isInstanceOf( Statement.class );
        Statement out = ( Statement ) persisted;
        assertThat( out.getSubject() ).isEqualTo( "Schwann cell" );
        assertThat( out.getPredicate() ).isEqualTo( "derives from part of" );
        assertThat( out.getObject() ).isEqualTo( "sciatic nerve" );
    }
}
