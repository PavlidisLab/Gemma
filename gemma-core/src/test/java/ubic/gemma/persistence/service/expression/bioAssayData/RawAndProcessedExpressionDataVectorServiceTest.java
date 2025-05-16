package ubic.gemma.persistence.service.expression.bioAssayData;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.core.context.TestComponent;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ContextConfiguration
public class RawAndProcessedExpressionDataVectorServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class RawAndProcessedExpressionDataVectorServiceTestContextConfiguration {

        @Bean
        public RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService() {
            return new RawAndProcessedExpressionDataVectorServiceImpl( mock( RawAndProcessedExpressionDataVectorDao.class ) );
        }

        @Bean
        public RawExpressionDataVectorService rawExpressionDataVectorService() {
            return mock( RawExpressionDataVectorService.class );
        }

        @Bean
        public ProcessedExpressionDataVectorService processedExpressionDataVectorService() {
            return mock( ProcessedExpressionDataVectorService.class );
        }
    }

    @Autowired
    private RawAndProcessedExpressionDataVectorService rawAndProcessedExpressionDataVectorService;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Test
    public void testThawCollection() {
        RawExpressionDataVector ev = new RawExpressionDataVector();
        ProcessedExpressionDataVector pv = new ProcessedExpressionDataVector();
        rawAndProcessedExpressionDataVectorService.thaw( Arrays.asList( ev, pv ) );
        verify( rawExpressionDataVectorService ).thaw( Collections.singleton( ev ) );
        verify( processedExpressionDataVectorService ).thaw( Collections.singleton( pv ) );
    }

}