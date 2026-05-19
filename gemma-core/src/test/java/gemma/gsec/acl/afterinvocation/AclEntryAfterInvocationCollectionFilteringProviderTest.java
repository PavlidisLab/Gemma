package gemma.gsec.acl.afterinvocation;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.Securable;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class AclEntryAfterInvocationCollectionFilteringProviderTest {

    private AclService aclService;
    private AclEntryAfterInvocationCollectionFilteringProvider voter;
    private ConfigAttribute ca;
    private Authentication auth;

    @Before
    public void setUp() {
        aclService = mock( AclService.class );
        voter = new AclEntryAfterInvocationCollectionFilteringProvider( aclService, "ACL_SECURABLE_COLLECTION_READ", Collections.singletonList( BasePermission.READ ) );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        voter.setProcessDomainObjectClass( Securable.class );
        ca = mock( ConfigAttribute.class );
        when( ca.getAttribute() ).thenReturn( "ACL_SECURABLE_COLLECTION_READ" );
        auth = new TestingAuthenticationToken( "bob", "1234" );
    }

    @Test
    public void testWhenCollectionContainNullOrIncompatibleEntries() {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( true );
        when( aclService.readAclsById( any(), any() ) )
            .thenAnswer( a -> a.getArgument( 0, Collection.class ).stream()
                .distinct()
                .collect( Collectors.toMap( o -> ( ObjectIdentity ) o, o -> acl ) ) );
        Collection<?> collection = Arrays.asList( mock( Securable.class ), null, mock( Securable.class ) );
        Collection<?> result = ( Collection<?> ) voter.decide( auth, null, Collections.singletonList( ca ), collection );
        assertSame( collection, result );
        assertEquals( 3, result.size() );
        verify( acl, times( 2 ) ).isGranted( any(), any(), eq( false ) );
        verifyNoMoreInteractions( acl );
    }

    public static class MyModel implements Securable {

        @Override
        public Long getId() {
            return 1L;
        }

        @Override
        public int hashCode() {
            throw new RuntimeException( "Should not be called!" );
        }
    }

    /**
     * Hashing an element triggers a proxy initialization with Hibernate, which is not desirable.
     * <p>
     * This is an issue in Spring Security where a HashSet is used for removing proxies.
     * See <a href="https://github.com/PavlidisLab/gsec/issues/25">#25</a> for details.
     */
    @Test
    public void testRemovedCollectionElementsAreNotHashed() {
        Securable a = new MyModel();
        AclObjectIdentity oid = new AclObjectIdentity( a );
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( false );
        when( aclService.readAclsById( eq( Collections.singletonList( oid ) ), any() ) )
            .thenReturn( Collections.singletonMap( oid, acl ) );
        Collection<?> collection = new ArrayList<>( Collections.singletonList( a ) );
        assertThatThrownBy( () -> voter.decide( auth, null, Collections.singletonList( ca ), collection ) )
            .isInstanceOf( RuntimeException.class )
            .hasMessageContaining( "Should not be called!" )
            .hasStackTraceContaining( "org.springframework.security.acls.afterinvocation.CollectionFilterer.remove" );
    }
}