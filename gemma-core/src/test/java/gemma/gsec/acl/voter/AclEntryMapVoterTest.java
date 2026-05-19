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

public class AclEntryMapVoterTest {

    static class Service {

        public void testGeneric( Map<? extends Securable, Object> map ) {

        }
    }

    @Test
    public void testGeneric() throws NoSuchMethodException {
        AclEntryMapVoter voter = new AclEntryMapVoter( mock(), "ACL_SECURABLE_MAP_READ", new Permission[] { BasePermission.READ } );
        voter.setProcessDomainObjectClass( Securable.class );
        voter.setObjectIdentityRetrievalStrategy( new ObjectIdentityRetrievalStrategyImpl() );
        voter.setSidRetrievalStrategy( new SidRetrievalStrategyImpl() );
        TestingAuthenticationToken auth = new TestingAuthenticationToken( "bob", "1234" );
        Service service = new Service();
        Map<Securable, Object> map = new HashMap<>();
        map.put( mock( Securable.class ), new Object() );
        SimpleMethodInvocation invocation = new SimpleMethodInvocation( service, Service.class.getMethod( "testGeneric", Map.class ), map );
        assertEquals( map.keySet(), voter.getCollectionInstance( invocation ) );
    }
}