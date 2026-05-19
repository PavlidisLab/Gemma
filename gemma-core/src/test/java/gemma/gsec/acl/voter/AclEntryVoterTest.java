package gemma.gsec.acl.voter;

import gemma.gsec.acl.ObjectTransientnessRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.Securable;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class AclEntryVoterTest {

    private AclService aclService;
    private AclEntryVoter voter;
    private ConfigAttribute ca, caIgnoreTransient;
    private Authentication auth;

    public static class Model implements Securable {

        @Nullable
        private final Long id;

        private Model( @Nullable Long id ) {
            this.id = id;
        }

        @Nullable
        @Override
        public Long getId() {
            return id;
        }
    }

    private static class MyService {

        public void test( Model model ) {
        }
    }

    @Before
    public void setUp() {
        aclService = mock( AclService.class );
        voter = new AclEntryVoter( aclService, "ACL_SECURABLE_READ", new Permission[] { BasePermission.READ } );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        voter.setProcessDomainObjectClass( Securable.class );
        ca = new SecurityConfig( "ACL_SECURABLE_READ" );
        caIgnoreTransient = new SecurityConfig( "ACL_SECURABLE_READ_IGNORE_TRANSIENT" );
        auth = new TestingAuthenticationToken( "bob", "1234" );
    }

    @Test
    public void testGrantOnTransientObject() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( true );
        Method method = MyService.class.getMethod( "test", Model.class );
        assertEquals( AclEntryVoter.ACCESS_GRANTED, voter.vote( auth, new SimpleMethodInvocation( null, method, new Model( null ) ), Collections.singleton( caIgnoreTransient ) ) );
    }

    @Test
    public void testErrorOnTransientObjectWhenAttributeIsMissing() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( true );
        Method method = MyService.class.getMethod( "test", Model.class );
        Assertions.assertThatThrownBy( () -> voter.vote( auth, new SimpleMethodInvocation( null, method, new Model( null ) ), Collections.singleton( ca ) ) )
            .isInstanceOf( IllegalArgumentException.class )
            .hasMessageContaining( "getId() is required to return a non-null value" );
    }

    @Test
    public void testAbstainOnTransientObject() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( false );
        Method method = MyService.class.getMethod( "test", Model.class );
        assertEquals( AclEntryVoter.ACCESS_ABSTAIN, voter.vote( auth, new SimpleMethodInvocation( null, method, new Model( null ) ), Collections.singleton( caIgnoreTransient ) ) );
    }

    @Test
    public void testNoSupportForIgnoringTransientIfNoStrategyIsSet() {
        voter.setObjectTransientnessRetrievalStrategy( null );
        assertFalse( voter.supports( caIgnoreTransient ) );
    }
}
