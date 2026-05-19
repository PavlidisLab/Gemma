package gemma.gsec.acl.voter;

import gemma.gsec.acl.ObjectTransientnessRetrievalStrategyImpl;
import gemma.gsec.model.Securable;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AclEntryCollectionVoterTest {


    public static class Model implements Securable {

        @Nullable
        private final Long id;

        public Model( @Nullable Long id ) {
            this.id = id;
        }

        public Model() {
            this( 1L );
        }

        @Override
        @Nullable
        public Long getId() {
            return id;
        }
    }

    public static class MyService {

        public void test( Collection<Securable> securables ) {
        }

        public void testGeneric( Collection<? extends Securable> securables ) {
        }

        public <T extends Securable> void testGeneric2( Collection<T> securables ) {
        }
    }

    private AclService aclService;
    private AclEntryCollectionVoter voter;
    private ConfigAttribute ca, caIgnoreTransient;
    private Authentication auth;

    @Before
    public void setUp() {
        aclService = mock( AclService.class );
        voter = new AclEntryCollectionVoter( aclService, "ACL_SECURABLE_COLLECTION_READ", new Permission[] { BasePermission.READ } );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        voter.setProcessDomainObjectClass( Securable.class );
        ca = new SecurityConfig( "ACL_SECURABLE_COLLECTION_READ" );
        caIgnoreTransient = new SecurityConfig( "ACL_SECURABLE_COLLECTION_READ_IGNORE_TRANSIENT" );
        auth = new TestingAuthenticationToken( "bob", "1234" );
    }

    @Test
    public void testCollection() throws NoSuchMethodException {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( true );
        when( aclService.readAclsById( any() ) )
            .thenAnswer( a -> a.getArgument( 0, Collection.class ).stream()
                .collect( Collectors.toMap( o -> ( ObjectIdentity ) o, o -> acl ) ) );
        Collection<?> collection = Collections.singleton( mock( Securable.class ) );
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( ca ) );
        assertEquals( AccessDecisionVoter.ACCESS_GRANTED, result );
    }

    @Test
    public void testGenericCollection() throws NoSuchMethodException {
        Collection<?> collection = Collections.singleton( mock( Securable.class ) );
        Method method = MyService.class.getMethod( "testGeneric", Collection.class );
        assertSame( collection, voter.getCollectionInstance( new SimpleMethodInvocation( null, method, collection ) ) );
    }

    @Test
    public void testGenericCollection2() throws NoSuchMethodException {
        Collection<?> collection = Collections.singleton( mock( Securable.class ) );
        Method method = MyService.class.getMethod( "testGeneric2", Collection.class );
        assertSame( collection, voter.getCollectionInstance( new SimpleMethodInvocation( null, method, collection ) ) );
    }

    @Test
    public void testNullCollection() throws NoSuchMethodException {
        Collection<?> collection = null;
        Method method = MyService.class.getMethod( "testGeneric2", Collection.class );
        assertNull( voter.getCollectionInstance( new SimpleMethodInvocation( null, method, collection ) ) );
    }

    @Test
    public void testCollectionDenyAccessWhenOneEntryDoesNotGrant() throws NoSuchMethodException {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( false );
        when( aclService.readAclsById( any() ) )
            .thenAnswer( a -> a.getArgument( 0, Collection.class ).stream()
                .collect( Collectors.toMap( o -> ( ObjectIdentity ) o, o -> acl ) ) );
        Collection<?> collection = Collections.singleton( mock( Securable.class ) );
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( ca ) );
        assertEquals( AccessDecisionVoter.ACCESS_DENIED, result );
    }

    @Test
    public void testCollectionAbstainWhenOneEntryIsNull() throws NoSuchMethodException {
        Acl acl = mock( Acl.class );
        when( acl.isGranted( any(), any(), anyBoolean() ) ).thenReturn( true );
        when( aclService.readAclsById( any() ) )
            .thenAnswer( a -> a.getArgument( 0, Collection.class ).stream()
                .collect( Collectors.toMap( o -> ( ObjectIdentity ) o, o -> acl ) ) );
        Collection<?> collection = Arrays.asList( mock( Securable.class ), null );
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( ca ) );
        assertEquals( AccessDecisionVoter.ACCESS_ABSTAIN, result );
    }

    @Test
    public void testEmptyCollectionShouldGrant() throws NoSuchMethodException {
        Collection<?> collection = Collections.emptyList();
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( ca ) );
        assertEquals( AccessDecisionVoter.ACCESS_GRANTED, result );
    }

    @Test
    public void testGrantOnCollectionContainingTransientObject() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( true );
        Collection<?> collection = Collections.singletonList( new Model( null ) );
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( caIgnoreTransient ) );
        assertEquals( AccessDecisionVoter.ACCESS_GRANTED, result );
    }

    @Test
    public void testErrorOnCollectionContainingTransientObjectWhenAttributeIsMissing() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( true );
        Collection<?> collection = Collections.singletonList( new Model( null ) );
        Method method = MyService.class.getMethod( "test", Collection.class );
        Assertions.assertThatThrownBy( () -> voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singleton( ca ) ) )
            .isInstanceOf( IllegalArgumentException.class )
            .hasMessageContaining( "getId() is required to return a non-null value" );
    }


    @Test
    public void testAbstainOnCollectionContainingTransientObject() throws NoSuchMethodException {
        voter.setObjectTransientnessRetrievalStrategy( new ObjectTransientnessRetrievalStrategyImpl() );
        voter.setGrantOnTransient( false );
        Collection<?> collection = Collections.singletonList( new Model( null ) );
        Method method = MyService.class.getMethod( "test", Collection.class );
        int result = voter.vote( auth, new SimpleMethodInvocation( null, method, collection ), Collections.singletonList( caIgnoreTransient ) );
        assertEquals( AccessDecisionVoter.ACCESS_ABSTAIN, result );
    }

    @Test
    public void testNoSupportForIgnoringTransientIfNoStrategyIsSet() {
        voter.setObjectTransientnessRetrievalStrategy( null );
        assertFalse( voter.supports( caIgnoreTransient ) );
    }
}