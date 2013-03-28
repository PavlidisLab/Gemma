/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.security.authorization.acl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.AbstractAclVoter;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

/**
 * Customized voter that looks at collections to see if permissions are present for objects contained in the collection;
 * the processDomainObjectClass set refers to the types of objects in the collection. getInternalMethod refers to a
 * method that will be applied to the contained object -- not the collection. Other settings (sidRetrievalStrategy etc)
 * work exactly like the superclass.
 * <p>
 * Method invocation works like this: As for the AclEntryVoter, this only handles cases where there is a single matching
 * argument. Each argument is checked in order; if it's a collection, its contents are exampined; If the collection
 * contains objects matching the configured processDomainObjectClass, then it is used. If the collection is empty, it is
 * ignored. If no Collection of processDomainObjectClass is found, a AuthorizationServiceException is thrown. The
 * limitation that the collection be non-empty is so there is some way to tell what the intent is.
 * <p>
 * The voting works as follows: The Principal must have the required Permissions on <em>all</em> of the collection's
 * members; otherwise DENIED. ABSTAIN will be returned if it isn't a Collection in the first place. Null collection
 * members are ignored. As with the superclass, an exception will be thrown if the collection members are not of the set
 * processDomainObjectClass type.
 * 
 * @author paul
 * @version $Id$
 * @see org.springframework.security.acls.AclEntryVoter -- this is basically the same thing with a revised vote method;
 *      I would have subclassed it if there weren't so many private fields there.
 */
public class AclCollectionEntryVoter extends AbstractAclVoter {

    private static Log logger = LogFactory.getLog( AclCollectionEntryVoter.class );

    private AclService aclService;
    private String internalMethod;
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ValueObjectAwareIdentityRetrievalStrategyImpl();
    private String processConfigAttribute;
    private List<Permission> requirePermission;
    private SidRetrievalStrategy sidRetrievalStrategy = new SidRetrievalStrategyImpl();

    public AclCollectionEntryVoter( AclService aclService, String processConfigAttribute, Permission[] requirePermission ) {

        this.aclService = aclService;
        this.processConfigAttribute = processConfigAttribute;
        this.requirePermission = Arrays.asList( requirePermission );
    }

    /**
     * @return the aclService
     */
    public AclService getAclService() {
        return aclService;
    }

    /**
     * @return the internalMethod
     */
    public String getInternalMethod() {
        return internalMethod;
    }

    /**
     * @return the objectIdentityRetrievalStrategy
     */
    public ObjectIdentityRetrievalStrategy getObjectIdentityRetrievalStrategy() {
        return objectIdentityRetrievalStrategy;
    }

    /**
     * @return the processConfigAttribute
     */
    public String getProcessConfigAttribute() {
        return processConfigAttribute;
    }

    /**
     * @return the requirePermission
     */
    public List<Permission> getRequirePermission() {
        return requirePermission;
    }

    /**
     * @return the sidRetrievalStrategy
     */
    public SidRetrievalStrategy getSidRetrievalStrategy() {
        return sidRetrievalStrategy;
    }

    /**
     * @param objectIdentityRetrievalStrategy the objectIdentityRetrievalStrategy to set
     */
    public void setObjectIdentityRetrievalStrategy( ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy ) {
        this.objectIdentityRetrievalStrategy = objectIdentityRetrievalStrategy;
    }

    /**
     * @param sidRetrievalStrategy the sidRetrievalStrategy to set
     */
    public void setSidRetrievalStrategy( SidRetrievalStrategy sidRetrievalStrategy ) {
        this.sidRetrievalStrategy = sidRetrievalStrategy;
    }

    @Override
    public boolean supports( ConfigAttribute attribute ) {
        if ( ( attribute.getAttribute() != null ) && attribute.getAttribute().equals( processConfigAttribute ) ) {
            return true;
        }
        return false;

    }

    /*
     * Most of this is modified from the superclass vote method code.
     * 
     * @see org.springframework.security.acls.AclEntryVoter#vote(org.springframework.security.core.Authentication,
     * java.lang.Object, java.util.Collection)
     */
    @Override
    public int vote( Authentication authentication, Object object, Collection<ConfigAttribute> attributes ) {

        for ( ConfigAttribute attr : attributes ) {

            if ( !this.supports( attr ) ) {
                continue;
            }

            /*
             * This is what makes the decision on the invocation
             */
            Collection<?> coll = getCollectionInstance( object );

            if ( coll == null ) {
                continue;
            }

            for ( Object domainObject : coll ) {

                // If domain object is null, vote to abstain
                if ( domainObject == null ) {
                    continue;
                }

                // Evaluate if we are required to use an inner domain object
                if ( StringUtils.hasText( getInternalMethod() ) ) {
                    try {
                        Class<?> clazz = domainObject.getClass();
                        Method method = clazz.getMethod( getInternalMethod(), new Class[0] );
                        domainObject = method.invoke( domainObject, new Object[0] );
                    } catch ( NoSuchMethodException nsme ) {
                        throw new AuthorizationServiceException( "Object of class '" + domainObject.getClass()
                                + "' does not provide the requested internalMethod: " + getInternalMethod() );
                    } catch ( IllegalAccessException iae ) {
                        logger.debug( "IllegalAccessException", iae );

                        throw new AuthorizationServiceException( "Problem invoking internalMethod: "
                                + getInternalMethod() + " for object: " + domainObject );
                    } catch ( InvocationTargetException ite ) {
                        logger.debug( "InvocationTargetException", ite );

                        throw new AuthorizationServiceException( "Problem invoking internalMethod: "
                                + getInternalMethod() + " for object: " + domainObject );
                    }
                }

                // Obtain the OID applicable to the domain object
                ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

                // Obtain the SIDs applicable to the principal
                List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

                Acl acl;

                try {
                    // Lookup only ACLs for SIDs we're interested in
                    acl = aclService.readAclById( objectIdentity, sids );
                } catch ( NotFoundException nfe ) {
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Voting to deny access - no ACLs apply for this principal: " + domainObject );
                    }

                    return ACCESS_DENIED;
                }

                try {
                    if ( !acl.isGranted( requirePermission, sids, false ) ) {
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( "Voting to deny access - ACLs returned, but insufficient permissions for this principal" );
                        }

                        return ACCESS_DENIED;
                    }

                } catch ( NotFoundException nfe ) {
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Voting to deny access - no ACLs apply for this principal" );
                    }

                    return ACCESS_DENIED;
                }
            }

            // No denials, so we're allowing access.
            if ( logger.isDebugEnabled() ) {
                logger.debug( "Voting to grant access: " + coll );
            }
            return ACCESS_GRANTED;

        }

        // No configuration attribute matched, so abstain
        return ACCESS_ABSTAIN;

    }

    /**
     * Get the collection from the invocation.
     * 
     * @param secureObject
     * @return
     */
    private Collection<?> getCollectionInstance( Object secureObject ) {

        Object[] args;
        Class<?>[] params;

        if ( secureObject instanceof MethodInvocation ) {
            MethodInvocation invocation = ( MethodInvocation ) secureObject;
            params = invocation.getMethod().getParameterTypes();
            args = invocation.getArguments();
        } else {
            JoinPoint jp = ( JoinPoint ) secureObject;
            params = ( ( CodeSignature ) jp.getStaticPart().getSignature() ).getParameterTypes();
            args = jp.getArgs();
        }

        for ( int i = 0; i < params.length; i++ ) {
            if ( Collection.class.isAssignableFrom( params[i] ) ) {

                Collection<?> coll = ( Collection<?> ) args[i];

                /*
                 * Inspect the collection: does it contain the right kind of objects. Note: AFAIK there is no way to do
                 * this using reflection, thanks to erasure.
                 */

                if ( coll.isEmpty() ) {
                    continue; // no way to know that it needs to be checked...
                }

                Object o = coll.iterator().next();

                if ( this.getProcessDomainObjectClass().isAssignableFrom( o.getClass() ) ) {
                    return coll;
                }

            }
        }

        throw new AuthorizationServiceException( "Secure object: " + secureObject
                + " did not provide a non-empty Collection of " + this.getProcessDomainObjectClass() + "'s" );

    }

}
