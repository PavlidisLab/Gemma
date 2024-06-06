package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.core.context.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class RawExpressionDataVectorDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class RawExpressionDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public RawExpressionDataVectorDao rawExpressionDataVectorDao( SessionFactory sessionFactory ) {
            return new RawExpressionDataVectorDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Test
    public void testFind() {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 1L );
        QuantitationType qt = new QuantitationType();
        qt.setId( 1L );
        assertThat( rawExpressionDataVectorDao.find( ad, qt ) ).isEmpty();
    }
}