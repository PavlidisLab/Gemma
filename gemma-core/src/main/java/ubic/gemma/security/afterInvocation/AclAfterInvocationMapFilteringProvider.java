package ubic.gemma.security.afterInvocation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.AuthorizationServiceException;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.acl.AclEntry;
import org.springframework.security.acl.AclManager;
import org.springframework.security.acl.basic.BasicAclEntry;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.afterinvocation.AfterInvocationProvider;
import org.springframework.util.Assert;

public class AclAfterInvocationMapFilteringProvider implements AfterInvocationProvider, InitializingBean {
    // ~ Static fields/initializers
    // =====================================================================================

    protected static final Log logger = LogFactory.getLog( AclAfterInvocationMapFilteringProvider.class );

    // ~ Instance fields
    // ================================================================================================

    private AclManager aclManager;
    private Class processDomainObjectClass = Object.class;
    private String processConfigAttribute = "AFTER_ACL_MAP_READ";
    private int[] requirePermission = { SimpleAclEntry.READ };

    // ~ Methods
    // ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull( processConfigAttribute, "A processConfigAttribute is mandatory" );
        Assert.notNull( aclManager, "An aclManager is mandatory" );

        if ( ( requirePermission == null ) || ( requirePermission.length == 0 ) ) {
            throw new IllegalArgumentException( "One or more requirePermission entries is mandatory" );
        }
    }

    public Object decide( Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object returnedObject ) throws AccessDeniedException {
        Iterator iter = config.getConfigAttributes().iterator();

        while ( iter.hasNext() ) {
            ConfigAttribute attr = ( ConfigAttribute ) iter.next();

            if ( this.supports( attr ) ) {
                // Need to process the Collection for this invocation
                if ( returnedObject == null ) {
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Return object is null, skipping" );
                    }

                    return null;
                }

                Filterer filterer = null;

                if ( returnedObject instanceof Map ) {
                    Map map = ( Map ) returnedObject;
                    filterer = new MapFilterer( map );
                } else {
                    throw new AuthorizationServiceException( "A Map was required as the "
                            + "returnedObject, but the returnedObject was: " + returnedObject );
                }

                // Locate unauthorised Collection elements
                Iterator collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {
                    Object domainObject = collectionIter.next();

                    boolean hasPermission = false;

                    if ( domainObject == null ) {
                        hasPermission = true;
                    } else if ( !processDomainObjectClass.isAssignableFrom( domainObject.getClass() ) ) {
                        hasPermission = true;
                    } else {
                        AclEntry[] acls = aclManager.getAcls( domainObject, authentication );

                        if ( ( acls != null ) && ( acls.length != 0 ) ) {
                            for ( int i = 0; i < acls.length; i++ ) {
                                // Locate processable AclEntrys
                                if ( acls[i] instanceof BasicAclEntry ) {
                                    BasicAclEntry processableAcl = ( BasicAclEntry ) acls[i];

                                    // See if principal has any of the required permissions
                                    for ( int y = 0; y < requirePermission.length; y++ ) {
                                        if ( processableAcl.isPermitted( requirePermission[y] ) ) {
                                            hasPermission = true;

                                            if ( logger.isDebugEnabled() ) {
                                                logger.debug( "Principal is authorised for element: " + domainObject
                                                        + " due to ACL: " + processableAcl.toString() );
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if ( !hasPermission ) {
                            filterer.remove( domainObject );

                            if ( logger.isDebugEnabled() ) {
                                logger.debug( "Principal is NOT authorised for element: " + domainObject );
                            }
                        }
                    }
                }

                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }

    public AclManager getAclManager() {
        return aclManager;
    }

    public String getProcessConfigAttribute() {
        return processConfigAttribute;
    }

    public int[] getRequirePermission() {
        return requirePermission;
    }

    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

    public void setProcessConfigAttribute( String processConfigAttribute ) {
        this.processConfigAttribute = processConfigAttribute;
    }

    public void setProcessDomainObjectClass( Class processDomainObjectClass ) {
        Assert.notNull( processDomainObjectClass, "processDomainObjectClass cannot be set to null" );
        this.processDomainObjectClass = processDomainObjectClass;
    }

    public void setRequirePermission( int[] requirePermission ) {
        this.requirePermission = requirePermission;
    }

    /**
     * Allow setting permissions with String literals instead of integers as {@link #setRequirePermission(int[])}
     * 
     * @param requiredPermissions permission literals
     * @see SimpleAclEntry#parsePermissions(String[]) for valid values
     */
    public void setRequirePermissionFromString( String[] requiredPermissions ) {
        setRequirePermission( SimpleAclEntry.parsePermissions( requiredPermissions ) );
    }

    public boolean supports( ConfigAttribute attribute ) {
        if ( ( attribute.getAttribute() != null ) && attribute.getAttribute().equals( getProcessConfigAttribute() ) ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     * 
     * @param clazz the secure object
     * @return always <code>true</code>
     */
    public boolean supports( Class clazz ) {
        return true;
    }
}
