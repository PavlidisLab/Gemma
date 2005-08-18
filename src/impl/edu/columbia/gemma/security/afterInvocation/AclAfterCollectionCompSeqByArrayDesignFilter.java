/* Copyright 2004, 2005 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.columbia.gemma.security.afterInvocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import net.sf.acegisecurity.AccessDeniedException;
import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.AuthorizationServiceException;
import net.sf.acegisecurity.ConfigAttribute;
import net.sf.acegisecurity.ConfigAttributeDefinition;
import net.sf.acegisecurity.acl.AclEntry;
import net.sf.acegisecurity.acl.AclManager;
import net.sf.acegisecurity.acl.basic.AbstractBasicAclEntry;
import net.sf.acegisecurity.acl.basic.SimpleAclEntry;
import net.sf.acegisecurity.afterinvocation.AfterInvocationProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * For this particular AfterInvocationProvider, composite sequence authorization is determined based on the secured
 * array design acl. ie. composite sequence security is determined from an owning array desgin's security. Copyright (c)
 * 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author Ben Alex
 * @author Paulo Neves
 * @version $Id: BasicAclEntryAfterInvocationArrayDesignCollectionFilteringProvider.java,v 1.2 2005/08/17 21:46:32
 *          keshav Exp $
 * @see AfterInvocationProvider
 */
public class AclAfterCollectionCompSeqByArrayDesignFilter implements AfterInvocationProvider,
        InitializingBean {
    // ~ Static fields/initializers =============================================

    protected static final Log logger = LogFactory
            .getLog( AclAfterCollectionCompSeqByArrayDesignFilter.class );

    // ~ Instance fields ========================================================

    private AclManager aclManager;
    private String processConfigAttribute = "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ";
    private int[] requirePermission = { SimpleAclEntry.READ };

    // ~ Methods ================================================================

    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

    public AclManager getAclManager() {
        return aclManager;
    }

    public void setProcessConfigAttribute( String processConfigAttribute ) {
        this.processConfigAttribute = processConfigAttribute;
    }

    public String getProcessConfigAttribute() {
        return processConfigAttribute;
    }

    public void setRequirePermission( int[] requirePermission ) {
        this.requirePermission = requirePermission;
    }

    public int[] getRequirePermission() {
        return requirePermission;
    }

    /**
     * Invoked by the beanFactory after it has set all the bean properties supplied. This method allows this bean to be
     * initialized only when these preconditions (setting the properties) have been met.
     */
    public void afterPropertiesSet() throws Exception {
        assert processConfigAttribute != null : "A processConfigAttribute is mandatory";
        assert aclManager != null : "An aclManager is mandatory";

        if ( ( requirePermission == null ) || ( requirePermission.length == 0 ) ) {
            throw new IllegalArgumentException( "One or more requirePermission entries is mandatory" );
        }
    }

    /**
     * Decides whether user has access to object based on owning object (for composition relationships).
     * 
     * @param authentication
     * @param object
     * @param config
     * @param returnedObject
     * @return Object
     * @throws AccessDeniedException
     */
    public Object decide( Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object returnedObject ) throws AccessDeniedException {

        if ( logger.isDebugEnabled() ) {
            logger.debug( object );
            logger.debug( config );
            logger.debug( returnedObject );
        }

        Iterator iter = config.getConfigAttributes();

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

                if ( returnedObject instanceof Collection ) {
                    Collection collection = ( Collection ) returnedObject;
                    filterer = new CollectionFilterer( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    Object[] array = ( Object[] ) returnedObject;
                    filterer = new ArrayFilterer( array );
                } else {
                    throw new AuthorizationServiceException(
                            "A Collection or an array (or null) was required as the returnedObject, but the returnedObject was: "
                                    + returnedObject );
                }

                // Locate unauthorised Collection elements
                Iterator collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {
                    // Object domainObject = collectionIter.next();

                    // keshav - this is used to get compositeSequences based on arrayDesign (owner).
                    Object targetDomainObject = collectionIter.next(); // compositeSequence
                    Object domainObject = null; // arrayDesign

                    Method m = null;
                    try {
                        m = targetDomainObject.getClass().getMethod( "getArrayDesign", new Class[] {} );
                    } catch ( SecurityException e ) {
                        e.printStackTrace();
                    } catch ( NoSuchMethodException e ) {
                        e.printStackTrace();
                    }
                    try {
                        domainObject = m.invoke( targetDomainObject, new Object[] {} );
                    } catch ( IllegalArgumentException e ) {
                        e.printStackTrace();
                    } catch ( IllegalAccessException e ) {
                        e.printStackTrace();
                    } catch ( InvocationTargetException e ) {
                        e.printStackTrace();
                    }// end keshav

                    boolean hasPermission = false;

                    AclEntry[] acls = null;

                    if ( domainObject == null ) {
                        hasPermission = true;
                    } else {
                        acls = aclManager.getAcls( domainObject, authentication );
                    }

                    if ( ( acls != null ) && ( acls.length != 0 ) ) {
                        for ( int i = 0; i < acls.length; i++ ) {
                            // Locate processable AclEntrys
                            if ( acls[i] instanceof AbstractBasicAclEntry ) {
                                AbstractBasicAclEntry processableAcl = ( AbstractBasicAclEntry ) acls[i];

                                // See if principal has any of the required permissions
                                for ( int y = 0; y < requirePermission.length; y++ ) {
                                    if ( processableAcl.isPermitted( requirePermission[y] ) ) {
                                        hasPermission = true;

                                        if ( logger.isDebugEnabled() ) {
                                            // logger.debug( "Principal is authorised for element: " + domainObject
                                            logger.debug( "Principal is authorised for element: " + targetDomainObject
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
                            // logger.debug( "Principal is NOT authorised for element: " + domainObject );
                            logger.debug( "Principal is NOT authorised for element: " + targetDomainObject );
                        }
                    }
                }

                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }

    /**
     * Called by the AbstractSecurityInterceptor at startup time to determine of AfterInvocationManager can process the
     * ConfigAttribute.
     * 
     * @param attribute
     * @return boolean
     */
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
        if ( logger.isDebugEnabled() ) logger.debug( clazz );

        return true;
    }
}
