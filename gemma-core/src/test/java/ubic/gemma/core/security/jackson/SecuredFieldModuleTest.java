package ubic.gemma.core.security.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gemma.gsec.model.Securable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.model.annotations.SecuredField;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class SecuredFieldModuleTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class SecuredJsonSerializerTestContextConfiguration {

        @Bean
        public ObjectMapper objectMapper( AccessDecisionManager accessDecisionManager ) {
            return new ObjectMapper()
                    .registerModule( new SecuredFieldModule( accessDecisionManager ) );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccessDecisionManager accessDecisionManager;

    private AbstractCuratableValueObject<?> curatableVo;

    @Before
    public void setUp() {
        curatableVo = new ExpressionExperimentValueObject();
        curatableVo.setCurationNote( "Reserved for curators" );
    }

    @After
    public void tearDown() {
        reset( accessDecisionManager );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testCuratable() throws JsonProcessingException {
        //noinspection unchecked
        ArgumentCaptor<Collection<ConfigAttribute>> captor = ArgumentCaptor.forClass( Collection.class );
        doNothing()
                .when( accessDecisionManager )
                .decide( any(), any(), captor.capture() );
        assertThat( objectMapper.writeValueAsString( curatableVo ) ).contains( "Reserved for curators" );
        verify( accessDecisionManager, atLeastOnce() ).decide( any(), same( curatableVo ), anyCollection() );
        assertThat( captor.getValue() ).anySatisfy( ca -> assertThat( ca.getAttribute() ).isEqualTo( "GROUP_ADMIN" ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_USER")
    public void testCuratableAsNonAdmin() throws JsonProcessingException {
        doThrow( AccessDeniedException.class )
                .when( accessDecisionManager )
                .decide( any(), any(), anyCollection() );
        assertThat( objectMapper.writeValueAsString( curatableVo ) )
                .doesNotContain( "Reserved for curators" );
    }

    @Test
    @WithMockUser(authorities = "IS_AUTHENTICATED_ANONYMOUSLY")
    public void testCuratableAsAnonymous() throws JsonProcessingException {
        doThrow( AccessDeniedException.class )
                .when( accessDecisionManager )
                .decide( any(), any(), anyCollection() );
        assertThat( objectMapper.writeValueAsString( curatableVo ) )
                .doesNotContain( "Reserved for curators" );
    }

    static class Entity implements Securable {
        private Long id;
        @SecuredField({ "GROUP_ADMIN" })
        private String foo;
        private Entity nestedEntity;

        @Override
        public Long getId() {
            return id;
        }

        public String getFoo() {
            return foo;
        }

        public Entity getNestedEntity() {
            return nestedEntity;
        }
    }

    @Test
    public void testSecuredFieldInASecurableEntity() throws JsonProcessingException {
        Entity entity = new Entity();
        entity.id = 1L;
        entity.foo = "test";
        entity.nestedEntity = new Entity();
        entity.nestedEntity.id = 2L;
        entity.nestedEntity.foo = "test";
        objectMapper.writeValueAsString( entity );
        verify( accessDecisionManager ).decide( any(), same( entity ), anyCollection() );
        verify( accessDecisionManager ).decide( any(), same( entity.nestedEntity ), anyCollection() );
    }
}