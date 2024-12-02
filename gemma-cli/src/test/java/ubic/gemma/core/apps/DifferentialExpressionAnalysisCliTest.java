package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

import static org.mockito.Mockito.mock;

@ContextConfiguration
public class DifferentialExpressionAnalysisCliTest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public DifferentialExpressionAnalysisCli differentialExpressionAnalysisCli() {
            return new DifferentialExpressionAnalysisCli();
        }

        @Bean
        public DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock();
        }

        @Bean
        public ExperimentalFactorService efs() {
            return mock();
        }

        @Bean
        public MessageSource messageSource() {
            return mock();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }
    }

    @Autowired
    private DifferentialExpressionAnalysisCli differentialExpressionAnalysisCli;

    @Test
    public void test2() {

    }
}