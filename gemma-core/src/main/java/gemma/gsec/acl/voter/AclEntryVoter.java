package gemma.gsec.acl.voter;

import gemma.gsec.acl.ObjectTransientnessRetrievalStrategy;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * An extension of {@link org.springframework.security.acls.AclEntryVoter}.
 * <p>
 * This voter also supports granting access to transient objects if a {@link ObjectTransientnessRetrievalStrategy}
 * is set. By default, the voter will abstain, but you can set {@link #setGrantOnTransient(boolean)} to grant. Methods
 * that allows transient instance have to append {@link #IGNORE_TRANSIENT_SUFFIX}.
 * @author poirigui
 */
public class AclEntryVoter extends org.springframework.security.acls.AclEntryVoter {

    /**
     * Suffix used by config attributes to ignore transient entities.
     */
    public static final String IGNORE_TRANSIENT_SUFFIX = "_IGNORE_TRANSIENT";

    @Nullable
    private ObjectTransientnessRetrievalStrategy objectTransientnessRetrievalStrategy;
    private final String processConfigAttributeIgnoreTransient;
    private boolean grantOnTransient;

    public AclEntryVoter( AclService aclService, String processConfigAttribute, Permission[] requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
        this.processConfigAttributeIgnoreTransient = processConfigAttribute + IGNORE_TRANSIENT_SUFFIX;
    }

    @Override
    public int vote( Authentication authentication, MethodInvocation object, Collection<ConfigAttribute> attributes ) {
        if ( objectTransientnessRetrievalStrategy != null
            && attributes.stream().map( ConfigAttribute::getAttribute ).anyMatch( processConfigAttributeIgnoreTransient::equals ) ) {
            Object domainObject = getDomainObjectInstance( object );
            if ( domainObject != null && objectTransientnessRetrievalStrategy.isObjectTransient( domainObject ) ) {
                return grantOnTransient ? ACCESS_GRANTED : ACCESS_ABSTAIN;
            }
        }
        return super.vote( authentication, object, attributes );
    }

    @Override
    public boolean supports( ConfigAttribute attribute ) {
        return super.supports( attribute )
            || ( objectTransientnessRetrievalStrategy != null && processConfigAttributeIgnoreTransient.equals( attribute.getAttribute() ) );
    }

    /**
     * Set the strategy to retrieve the transientness of an object.
     */
    public void setObjectTransientnessRetrievalStrategy( @Nullable ObjectTransientnessRetrievalStrategy objectTransientnessRetrievalStrategy ) {
        this.objectTransientnessRetrievalStrategy = objectTransientnessRetrievalStrategy;
    }

    /**
     * Set whether to grant access to transient objects or simply abstain.
     */
    public void setGrantOnTransient( boolean grantOnTransient ) {
        this.grantOnTransient = grantOnTransient;
    }
}
