package ubic.gemma.web.controller.expression.experiment;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.BaseWebTest;
import ubic.gemma.web.util.MessageUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ExpressionExperimentEditControllerTest extends BaseWebTest {


    @Configuration
    @TestComponent
    static class ExpressionExperimentEditControllerTestContextConfiguration {

        @Bean
        public ExpressionExperimentEditController expressionExperimentFormController() {
            return new ExpressionExperimentEditController();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public BioAssayService bioAssayService() {
            return mock();
        }

        @Bean
        public BioMaterialService bioMaterialService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }

        @Bean
        public Persister persisterHelper() {
            return mock();
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public MessageUtil messageUtil() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Test
    @WithMockUser
    public void test() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ExpressionExperimentValueObject eeVo = new ExpressionExperimentValueObject();
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 2L ), any(), any() ) ).thenReturn( ee );
        when( expressionExperimentService.loadValueObject( ee ) ).thenReturn( eeVo );
        perform( get( "/expressionExperiment/editExpressionExperiment.html?id={id}", 2L ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "expressionExperiment.edit" ) );
    }
}