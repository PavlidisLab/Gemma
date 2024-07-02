package ubic.gemma.web.controller.expression.arrayDesign;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.web.util.BaseWebTest;
import ubic.gemma.web.util.MessageUtil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration
public class ArrayDesignEditControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class ArrayDesignEditControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public ArrayDesignEditController arrayDesignFormController() {
            return new ArrayDesignEditController();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public MessageUtil messageUtil() {
            return mock();
        }
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Test
    public void test() throws Exception {
        ArrayDesignValueObject ad = new ArrayDesignValueObject();
        when( arrayDesignService.loadValueObjectById( 2L ) ).thenReturn( ad );
        perform( get( "/arrayDesign/editArrayDesign.html?id={id}", 2L ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "arrayDesign.edit" ) )
                .andExpect( model().attribute( "arrayDesign", ad ) )
                .andExpect( model().attributeExists( "technologyTypes" ) );
    }
}