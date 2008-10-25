/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.security.afterInvocation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.AccessDeniedException;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttribute;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.acl.AclEntry;
import org.springframework.security.acl.AclManager;
import org.springframework.security.acl.basic.AbstractBasicAclEntry;
import org.springframework.security.acl.basic.AclObjectIdentity;
import org.springframework.security.acl.basic.NamedEntityObjectIdentity;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.afterinvocation.AfterInvocationProvider;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Filter public {@link ExpressionExperiment}s.
 * 
 * @author keshav
 * @version $Id$
 * @see AfterInvocationProvider
 */
@SuppressWarnings("deprecation")
public class AclAfterCollectionPublicExpressionExperimentFilter implements AfterInvocationProvider {

    private Log log = LogFactory.getLog( this.getClass() );

    private AclManager aclManager;

    private int[] requirePermission = { SimpleAclEntry.READ }; // default.

    private static final AclObjectIdentity publicObjectIdentity = new NamedEntityObjectIdentity( "publicControlNode",
            "2" );// TODO move me

    public String getProcessConfigAttribute() {
        return "AFTER_ACL_FILTER_PUBLIC_EXPRESSION_EXPERIMENT_FROM_COLLECTION";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.afterinvocation.AfterInvocationProvider#decide(org.springframework.security.Authentication,
     *      java.lang.Object, org.springframework.security.ConfigAttributeDefinition, java.lang.Object)
     */
    public final Object decide( Authentication authentication, Object object, ConfigAttributeDefinition config,
            Object returnedObject ) throws AccessDeniedException {
        Collection<ConfigAttribute> configAttribs = config.getConfigAttributes();
        Iterator<ConfigAttribute> iter = configAttribs.iterator();

        while ( iter.hasNext() ) {
            ConfigAttribute attr = iter.next();

            if ( this.supports( attr ) ) {
                // Need to process the Collection for this invocation
                if ( returnedObject == null ) {
                    log.debug( "Return object is null, skipping" );

                    return null;
                }

                Filterer filterer = null;

                boolean wasSingleton = false;
                if ( returnedObject instanceof Collection ) {
                    Collection collection = ( Collection ) returnedObject;
                    filterer = new CollectionFilterer( collection );
                } else if ( returnedObject.getClass().isArray() ) {
                    Object[] array = ( Object[] ) returnedObject;
                    filterer = new ArrayFilterer( array );
                } else {
                    // shortcut, just put the object in a collection. (PP)
                    wasSingleton = true;
                    Collection<Object> coll = new HashSet<Object>();
                    coll.add( returnedObject );
                    filterer = new CollectionFilterer( coll );
                }

                // Locate unauthorised Collection elements
                Iterator collectionIter = filterer.iterator();

                while ( collectionIter.hasNext() ) {

                    Object domainObject = collectionIter.next();

                    if ( !( domainObject instanceof ExpressionExperiment ) ) continue;

                    boolean hasPermission = false;

                    AclEntry[] acls = null;

                    if ( domainObject == null ) {
                        hasPermission = true;
                    } else {
                        // get acl for domainObject that has been granted to the
                        // user.
                        acls = aclManager.getAcls( domainObject, authentication );
                    }

                    if ( ( acls != null ) && ( acls.length != 0 ) ) {
                        for ( int i = 0; i < acls.length; i++ ) {
                            // Locate processable AclEntrys
                            if ( acls[i] instanceof AbstractBasicAclEntry ) {
                                AbstractBasicAclEntry processableAcl = ( AbstractBasicAclEntry ) acls[i];

                                /* See if the object identity equals the publicControlNode. */
                                for ( int y = 0; y < requirePermission.length; y++ ) {
                                    if ( processableAcl.isPermitted( requirePermission[y] ) ) {

                                        AclObjectIdentity processableEntryObjectIdentity = processableAcl
                                                .getAclObjectIdentity();

                                        if ( !processableEntryObjectIdentity.equals( this.publicObjectIdentity ) ) {
                                            hasPermission = true;

                                            log.debug( "Filtering out public object." );

                                        }

                                    }
                                }
                            }
                        }
                    }

                    if ( !hasPermission ) {
                        filterer.remove( domainObject );

                    }
                }
                if ( wasSingleton ) {
                    if ( ( ( Collection ) filterer.getFilteredObject() ).size() == 1 ) {
                        return ( ( Collection ) filterer.getFilteredObject() ).iterator().next();
                    } else {
                        return null;
                    }

                }
                return filterer.getFilteredObject();
            }
        }

        return returnedObject;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.afterinvocation.AfterInvocationProvider#supports(org.springframework.security.ConfigAttribute)
     */
    public boolean supports( ConfigAttribute attribute ) {
        if ( ( attribute.getAttribute() != null ) && attribute.getAttribute().equals( getProcessConfigAttribute() ) ) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.afterinvocation.AfterInvocationProvider#supports(java.lang.Class)
     */
    public boolean supports( Class arg0 ) {
        return true;
    }

    /**
     * @param requirePermission
     */
    public void setRequirePermission( int[] requirePermission ) {
        this.requirePermission = requirePermission;
    }

    /**
     * @return
     */
    public int[] getRequirePermission() {
        return requirePermission;
    }

    /**
     * @param aclManager
     */
    public void setAclManager( AclManager aclManager ) {
        this.aclManager = aclManager;
    }

    /**
     * @return
     */
    public AclManager getAclManager() {
        return aclManager;
    }

}
