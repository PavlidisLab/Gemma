/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeanUtils;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.security.acl.basic.jdbc.CustomAclDao;
import ubic.gemma.util.ReflectionUtil;
import ubic.gemma.util.UserConstants;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="securityService"
 * @spring.property name="securableDao" ref="securableDao"
 * @spring.property name="customAclDao" ref="customAclDao"
 * @spring.property name="crudUtils" ref="crudUtils"
 */
public class SecurityService {

    private Log log = LogFactory.getLog( SecurityService.class );

    private SecurableDao securableDao = null;
    private CustomAclDao customAclDao = null;
    private CrudUtils crudUtils = null;

    public static final String ADMIN_AUTHORITY = "admin";
    public static final String USER_AUTHORITY = "user";

    /**
     * Makes the object private. If the object is a {@link Collection}, makes the all the objects private.
     * 
     * @param object
     */
    public void makePrivate( Object object ) {
        if ( object == null ) {
            log.warn( "Null cannot be made private" );
            return;
        }
        Collection<VisitedEntity> visited = new HashSet<VisitedEntity>();
        int parent = Integer.parseInt( CustomAclDao.ADMIN_CONTROL_NODE_PARENT_ID );
        makePrivateOrPublic( object, parent, visited );
    }

    /**
     * Makes the object public. If the object is a {@link Collection}, makes the all the objects public.
     * 
     * @param object
     */
    public void makePublic( Object object ) {
        if ( object == null ) {
            log.warn( "Null cannot be made public" );
            return;
        }
        Collection<VisitedEntity> visited = new HashSet<VisitedEntity>();
        int parent = Integer.parseInt( CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID );
        makePrivateOrPublic( object, parent, visited );
    }

    /**
     * Changes the parent object identity to either the {@link CustomAclDao.ADMIN_CONTROL_NODE} or
     * {@link CustomAclDao.PUBLIC_CONTROL_NODE}.
     * 
     * @param object
     * @param parentObjectIdentity
     * @param visited
     */
    @SuppressWarnings("unchecked")
    private void makePrivateOrPublic( Object object, int parentObjectIdentity, Collection<VisitedEntity> visited ) {
        log.debug( "Changing acl of object " + object + "." );

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object principal = authentication.getPrincipal();

        if ( object instanceof Securable ) {
            Securable secObject = ( Securable ) object;
            VisitedEntity visitedEntity = new VisitedEntity( secObject );
            if ( !visited.contains( visitedEntity ) ) {
                visited.add( visitedEntity );
                processAssociations( object, parentObjectIdentity, authentication, principal, visited );
            } else {
                log.debug( "Object " + object.getClass() + " already visited." );
            }
        } else if ( object instanceof Collection ) {
            Collection objects = ( Collection ) object;
            for ( Object o : objects ) {
                makePrivateOrPublic( o, parentObjectIdentity, visited );
            }
        } else {
            log.error( "Object not Securable.  Cannot change permissions for object of type "
                    + object.getClass().getName() + "." );
            return;
        }
    }

    /**
     * @param targetObject
     * @param parentObjectIdentity
     * @param authentication
     * @param principal
     * @param visited
     */
    @SuppressWarnings("unchecked")
    private void processAssociations( Object targetObject, int parentObjectIdentity, Authentication authentication,
            Object principal, Collection<VisitedEntity> visited ) {

        EntityPersister persister = crudUtils.getEntityPersister( targetObject );
        if ( persister == null ) {
            // FIXME this happens when the object is a proxy.
            log.error( "No Entity Persister found for " + targetObject.getClass().getName() );
            return;
        }
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();

        for ( int j = 0; j < propertyNames.length; j++ ) {
            CascadeStyle cs = cascadeStyles[j];
            if ( !crudUtils.needCascade( cs ) ) {
                continue;
            }

            PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( targetObject.getClass(), propertyNames[j] );

            Object associatedObject = null;
            try {
                associatedObject = ReflectionUtil.getProperty( targetObject, descriptor );
            } catch ( Exception e ) {
                throw new RuntimeException( "Error changing permission.  Not changing any of the permissions: " + e );
            }

            if ( associatedObject == null ) continue;

            Class<?> propertyType = descriptor.getPropertyType();

            if ( Securable.class.isAssignableFrom( propertyType ) ) {

                // if ( !crudUtils.needCascade( cs ) ) continue;

                if ( log.isDebugEnabled() ) log.debug( "Processing ACL for " + propertyNames[j] + ", Cascade=" + cs );
                makePrivateOrPublic( associatedObject, parentObjectIdentity, visited );
            } else if ( Collection.class.isAssignableFrom( propertyType ) ) {

                /*
                 * This block commented out because of lazy-load problems.
                 */
                Collection associatedObjects = ( Collection ) associatedObject;
                for ( Object object2 : associatedObjects ) {
                    if ( Securable.class.isAssignableFrom( object2.getClass() ) ) {

                        // if ( !crudUtils.needCascade( cs ) ) continue;

                        if ( log.isDebugEnabled() ) {
                            log.debug( "Processing ACL for member " + object2 + " of collection " + propertyNames[j]
                                    + ", Cascade=" + cs );
                        }
                        makePrivateOrPublic( object2, parentObjectIdentity, visited );
                    }
                }
            }
        }

        this.changeParent( targetObject, parentObjectIdentity );
    }

    /**
     * Change the parent acl object identity of obj to aclObjectIdentityParentId.
     * 
     * @param obj
     * @param aclObjectIdentityParentId
     */
    private void changeParent( Object obj, int aclObjectIdentityParentId ) {
        Securable securable = ( Securable ) obj;
        Long aclObjectIdentityId = securableDao.getAclObjectIdentityId( securable );

        customAclDao.updateAclObjectIdentityParent( aclObjectIdentityId, aclObjectIdentityParentId );
    }

    /**
     * Returns the username of the current principal (user). This can be invoked from anywhere (ie. in a controller,
     * service, dao), and does not rely on any external security features. This is useful for determining who is the
     * current user.
     * 
     * @return String
     */
    public static String getPrincipalName() {

        Object obj = getPrincipal();

        String username = null;
        if ( obj instanceof UserDetails ) {
            username = ( ( UserDetails ) obj ).getUsername();
        } else {
            username = obj.toString();
        }

        return username;
    }

    /**
     * Returns the username of the current principal (user). This can be invoked from anywhere (ie. in a controller,
     * service, dao), and does not rely on any external security features. The return type should checked if it is an
     * instance of UserDetails and typecast to access information about the current user (ie. GrantedAuthority).
     * 
     * @return Object
     */
    public static Object getPrincipal() {
        return getAuthentication().getPrincipal();
    }

    /**
     * Returns the Authentication object from the SecurityContextHolder.
     * 
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) throw new RuntimeException( "Null authentication object" );

        return authentication;
    }

    /**
     * Returns true if the current user has admin authority.
     * 
     * @return true if the current user has admin authority
     */
    public static boolean isUserAdmin() {
        GrantedAuthority[] authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( ADMIN_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the user is non-anonymous.
     * 
     * @return
     */
    public static boolean isUserLoggedIn() {
        Authentication authentication = getAuthentication();
        GrantedAuthority[] authorities = authentication.getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( USER_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the parent object identity is the {@link CustomAclDao.ADMIN_CONTROL_NODE}
     * 
     * @param s
     * @return
     */
    public boolean isPrivate( Securable s ) {

        Integer parent = securableDao.getAclObjectIdentityParentId( s );

        if ( parent != null && parent.equals( Integer.parseInt( CustomAclDao.ADMIN_CONTROL_NODE_PARENT_ID ) ) ) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public java.util.Map<Securable, Boolean> arePrivate( Collection securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        for ( Object o : securables ) {
            if ( !( o instanceof Securable ) ) throw new RuntimeException( "Object not securable found." );

            Securable s = ( Securable ) o;
            boolean p = this.isPrivate( s );
            result.put( s, p );
        }
        return result;
    }

    /**
     * Change the permissions of the user by setting the acl_object_identity to that of the adminControlNode. This gives
     * the user "admin" privileges.
     * 
     * @param user
     */
    private void setControlNodeAsAdmin( User user ) {
        int mask = SimpleAclEntry.READ_WRITE;
        int newObjectIdentity = Integer.parseInt( CustomAclDao.ADMIN_CONTROL_NODE_PARENT_ID );
        int oldObjectIdentity = Integer.parseInt( CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID );
        String recipient = user.getUserName();
        customAclDao.updateControlNodeForRecipient( newObjectIdentity, oldObjectIdentity, mask, recipient );
    }

    /**
     * Change the permissions of the user by setting the acl_object_identity to that of the publicControlNode. This
     * gives the user "user" privileges.
     * 
     * @param user
     */
    private void setControlNodeAsUser( User user ) {
        int mask = SimpleAclEntry.READ;
        int newObjectIdentity = Integer.parseInt( CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID );
        int oldObjectIdentity = Integer.parseInt( CustomAclDao.ADMIN_CONTROL_NODE_PARENT_ID );
        String recipient = user.getUserName();
        customAclDao.updateControlNodeForRecipient( newObjectIdentity, oldObjectIdentity, mask, recipient );
    }

    /**
     * Change the control node this user points to based on the new role. That is, for each user in the system there is
     * an acl_permission that does not have an associated acl_object_identity that is a {@link Securable}. Instead, the
     * acl_object_identity in the acl_permission table points to one of the control nodes in the acl_object_identity
     * table (like CustomAclDao.ADMIN_CONTROL_NODE or CustomAclDao.USER_CONTROL_NODE).
     * 
     * @param user
     * @param role The new role
     */
    public void changeControlNode( User user, String role ) {
        if ( StringUtils.equals( role, UserConstants.USER_ROLE ) ) {
            setControlNodeAsUser( user );
        } else if ( StringUtils.equals( role, UserConstants.ADMIN_ROLE ) ) {
            setControlNodeAsAdmin( user );
        } else {
            throw new RuntimeException( "Role " + role + " does not exist in the system.  Cannot change control node." );
        }
    }

    /**
     * @param securableDao the securableDao to set
     */
    public void setSecurableDao( SecurableDao securableDao ) {
        this.securableDao = securableDao;
    }

    /**
     * @param customAclDao
     */
    public void setCustomAclDao( CustomAclDao customAclDao ) {
        this.customAclDao = customAclDao;
    }

    /**
     * @param crudUtils
     */
    public void setCrudUtils( CrudUtils crudUtils ) {
        this.crudUtils = crudUtils;
    }

    /**
     * @author keshav
     * @version $Id$
     */
    public class VisitedEntity {

        private Securable entity;

        /**
         * @param entity
         */
        VisitedEntity( Securable entity ) {
            this.entity = entity;
        }

        /**
         * @return
         */
        public Class getEntityClass() {
            return entity.getClass();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return 29 * entity.getId().hashCode() + entity.getClass().hashCode();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object other ) {
            if ( this == other ) {
                return true;
            }
            if ( !( other instanceof VisitedEntity ) ) {
                return false;
            }
            final VisitedEntity that = ( VisitedEntity ) other;
            if ( this.entity.getId() == null || that.entity.getId() == null
                    || !this.entity.getId().equals( that.entity.getId() )
                    || !this.entity.getClass().equals( that.getEntityClass() ) ) {
                return false;
            }
            return true;
        }
    }
}