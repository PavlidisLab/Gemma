package gemma.gsec.acl.afterinvocation;

import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.Securable;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AclEntryAfterInvocationProviderTest {

    private AclService aclService;
    private AclEntryAfterInvocationProvider voter;
    private Authentication auth;

    @Before
    public void setUp() {
        aclService = mock( AclService.class );
        voter = new AclEntryAfterInvocationProvider( aclService, Collections.singletonList( BasePermission.READ ) );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        voter.setProcessDomainObjectClass( Securable.class );
        auth = new TestingAuthenticationToken( "bob", "1234" );
    }

    @Test
    public void test() {
        Securable securableObject = mock( Securable.class );
        when( securableObject.getId() ).thenReturn( 1L );
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( true );
        when( aclService.readAclById( eq( new ObjectIdentityImpl( securableObject ) ), anyList() ) ).thenReturn( acl );
        Object result = voter.decide( auth, null, Collections.singletonList( new SecurityConfig( "AFTER_ACL_READ" ) ), securableObject );
        assertThat( result ).isSameAs( securableObject );
    }


    @Test
    public void testWhenNotGranted() {
        Securable securableObject = mock( Securable.class );
        when( securableObject.getId() ).thenReturn( 1L );
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( false );
        when( aclService.readAclById( eq( new ObjectIdentityImpl( securableObject ) ), anyList() ) ).thenReturn( acl );
        assertThatThrownBy( () -> voter.decide( auth, null, Collections.singletonList( new SecurityConfig( "AFTER_ACL_READ" ) ), securableObject ) )
            .isInstanceOf( AccessDeniedException.class );
    }

    @Test
    public void testQuiet() {
        voter.setQuiet( true );
        Securable securableObject = mock( Securable.class );
        when( securableObject.getId() ).thenReturn( 1L );
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( false );
        when( aclService.readAclById( eq( new ObjectIdentityImpl( securableObject ) ), anyList() ) ).thenReturn( acl );
        Object result = voter.decide( auth, null, Collections.singletonList( new SecurityConfig( "AFTER_ACL_READ_QUIET" ) ), securableObject );
        assertNull( result );
    }
}