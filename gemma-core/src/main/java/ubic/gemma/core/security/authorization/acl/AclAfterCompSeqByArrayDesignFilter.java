package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.List;

public class AclAfterCompSeqByArrayDesignFilter extends AclEntryAfterInvocationByAssociationFilteringProvider {

    public AclAfterCompSeqByArrayDesignFilter( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_ARRAYDESIGN_READ", requirePermission );
    }

    @Override
    protected Class<?> getProcessDomainObjectClass() {
        return CompositeSequence.class;
    }

    @Override
    protected Object getActualDomainObject( Object targetDomainObject ) {
        if ( targetDomainObject instanceof CompositeSequence ) {
            return ( ( CompositeSequence ) targetDomainObject ).getArrayDesign();
        }
        throw new IllegalArgumentException(
                "Don't know how to filter a " + targetDomainObject.getClass().getSimpleName() );
    }
}
