package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.core.context.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class QuantitationTypeDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class QuantitationTypeDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public QuantitationTypeDao quantitationTypeDao( SessionFactory sessionFactory ) {
            return new QuantitationTypeDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Test
    public void testLoadValueObjects() {
        Filters filters = Filters.by( Filter.parse( null, "name", String.class, Filter.Operator.eq, "FPKM" ) );
        assertThat( quantitationTypeDao.loadValueObjects( filters, null ) ).isEmpty();
    }

    @Test
    public void testExistsByExpressionExperimentAndVectorType() {
        QuantitationType qt = new QuantitationType();
        qt.setId( 1L );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        assertThat( quantitationTypeDao.existsByExpressionExperimentAndVectorType( qt, ee, RawExpressionDataVector.class ) )
                .isFalse();
    }
}