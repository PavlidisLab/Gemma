package gemma.gsec.acl.voter;

import gemma.gsec.model.Securable;
import org.junit.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.util.SimpleMethodInvocation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class AclEntryMapValueVoterTest {

    static class Service {

        public void testGeneric( Map<? extends Securable, Object> map ) {

        }
    }

    @Test
    public void testGeneric() throws NoSuchMethodException {
        AclEntryMapValueVoter voter = new AclEntryMapValueVoter( mock(), "ACL_SECURABLE_MAP_VALUE_READ", new Permission[] { BasePermission.READ } );
        voter.setProcessDomainObjectClass( Securable.class );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        TestingAuthenticationToken auth = new TestingAuthenticationToken( "bob", "1234" );
        AclEntryMapVoterTest.Service service = new AclEntryMapVoterTest.Service();
        Map<Object, Securable> map = new HashMap<>();
        map.put( new Object(), mock( Securable.class ) );
        SimpleMethodInvocation invocation = new SimpleMethodInvocation( service, Service.class.getMethod( "testGeneric", Map.class ), map );
        assertEquals( map.values(), voter.getCollectionInstance( invocation ) );
    }
}