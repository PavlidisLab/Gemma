package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.List;

/**
 * Filter {@link CompositeSequence} based on the permissions of the associated {@link ubic.gemma.model.expression.arrayDesign.ArrayDesign}.
 */
public class AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider extends AclEntryAfterInvocationByAssociationFilteringProvider {

    public AclEntryAfterInvocationCompositeSequenceByArrayDesignFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
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
