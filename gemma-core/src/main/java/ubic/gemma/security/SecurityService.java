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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.util.EntityUtils;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="securityService"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 * @spring.property name="securableDao" ref="securableDao"
 */
public class SecurityService {

    private Log log = LogFactory.getLog( SecurityService.class );

    private static final String ACCESSOR_PREFIX = "get";

    private BasicAclExtendedDao basicAclExtendedDao = null;
    private SecurableDao securableDao = null;
    private UnsecuredSecurableSet unsecuredClasses = new UnsecuredSecurableSet( null );

    public static final String ADMIN_AUTHORITY = "admin";
    public static final int PUBLIC_MASK = SimpleAclEntry.READ_WRITE;
    public static final int PRIVATE_MASK = SimpleAclEntry.NOTHING;

    private Collection visited = null;

    /**
     * Makes the object private. If the object is a {@link Collection}, makes the all the objects private.
     * 
     * @param object
     */
    @SuppressWarnings("unchecked")
    public void makePrivate( Object object ) {
        visited = new HashSet();
        makePrivateOrPublic( object, PRIVATE_MASK );
    }

    /**
     * Makes the object public. If the object is a {@link Collection}, makes the all the objects public.
     * 
     * @param object
     */
    @SuppressWarnings("unchecked")
    public void makePublic( Object object ) {
        visited = new HashSet();
        makePrivateOrPublic( object, PUBLIC_MASK );
    }

    /**
     * Changes the acl_permission of the object to either administrator/PRIVATE (mask=0), or read-write/PUBLIC (mask=6).
     * 
     * @param object
     * @param mask
     */
    @SuppressWarnings("unchecked")
    private void makePrivateOrPublic( Object object, int mask ) {
        log.debug( "Changing acl of object " + object + "." );

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object principal = authentication.getPrincipal();

        if ( object instanceof Securable ) {
            if ( !visited.contains( object ) ) {
                visited.add( object );
                processAssociations( object, mask, authentication, principal );
            } else {
                log.debug( "Object " + object.getClass() + " already visited." );
            }
        } else if ( object instanceof Collection ) {
            Collection objects = ( Collection ) object;
            for ( Object o : objects ) {
                makePrivateOrPublic( o, mask );
            }
        } else {
            log.error( "Object not Securable.  Cannot change permissions for object of type "
                    + object.getClass().getName() + "." );
            return;
        }
    }

    /**
     * @param targetObject
     * @param mask
     * @param authentication
     * @param principal
     */
    private void processAssociations( Object targetObject, int mask, Authentication authentication, Object principal ) {

        Class clazz = targetObject.getClass();
        Method[] methods = clazz.getMethods();

        for ( Method method : methods ) {
            String name = method.getName();
            if ( StringUtils.startsWithIgnoreCase( name, ACCESSOR_PREFIX ) ) {
                Class returnType = method.getReturnType();
                if ( unsecuredClasses.contains( returnType )
                        || ( returnType != java.util.Collection.class && !Securable.class.isAssignableFrom( returnType
                                .getClass() ) ) ) {
                    continue;
                }

                try {
                    if ( returnType == java.util.Collection.class ) {

                        Collection returnedCollection = ( Collection ) clazz.getMethod( name, ( Class[] ) null )
                                .invoke( targetObject, ( Object[] ) null );
                        if ( returnedCollection == null || returnedCollection.isEmpty() ) continue;

                        /* check if an object in collection is in unsecuredCol */
                        Object objInCol = returnedCollection.iterator().next();
                        if ( unsecuredClasses.contains( objInCol.getClass() )
                                || !Securable.class.isAssignableFrom( objInCol.getClass() ) ) {
                            continue;
                        } else {
                            /* if object in collection is not in unsecuredCol and is a Securable, process */
                            Iterator iter = returnedCollection.iterator();
                            while ( iter.hasNext() ) {
                                Object ob = iter.next();
                                log.debug( "process " + ob );
                                makePrivateOrPublic( ob, mask );// recursive
                            }
                        }
                    } else {
                        Object ob = clazz.getMethod( name, ( Class[] ) null ).invoke( targetObject, ( Object[] ) null );

                        ob = EntityUtils.getImplementationForProxy( ob );

                        if ( ob == null || unsecuredClasses.contains( ob.getClass() )
                                || !Securable.class.isAssignableFrom( ob.getClass() )
                                || ( ( Securable ) ob ).getId() == null ) {
                            continue;
                        }
                        makePrivateOrPublic( ob, mask );// recursive
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }

        }
        String recipient = configureWhoToRunAs( targetObject, mask, authentication, principal );
        if ( recipient != null ) changeMask( targetObject, mask, recipient );
    }

    /**
     * @param object
     * @param mask
     * @param recipient
     */
    private void changeMask( Object object, int mask, String recipient ) {
        try {
            basicAclExtendedDao.changeMask( new NamedEntityObjectIdentity( object ), recipient, mask );
        } catch ( Exception e ) {
            throw new RuntimeException( "Problems changing mask of " + object, e );
        }
    }

    /**
     * Runs as the recipient (in acl_permission) if the principal does not match the recipient. Returns null if
     * principal is not an administrator.
     * 
     * @param object
     * @param mask
     * @param authentication
     * @param principal
     */
    private String configureWhoToRunAs( Object object, int mask, Authentication authentication, Object principal ) {
        assert principal != null;
        Securable securedObject = ( Securable ) object;

        /* id of acl_object_identity */
        Long objectIdentityId = securableDao.getAclObjectIdentityId( securedObject );

        String recipient = null;
        if ( objectIdentityId == null ) return recipient;

        recipient = securableDao.getRecipient( objectIdentityId );

        if ( recipient == null ) {
            throw new IllegalStateException( "No recipient for object " + objectIdentityId + " object=" + securedObject );
        }

        if ( isUserAdmin() ) {
            if ( !recipient.equals( principal.toString() ) ) {
                RunAsManager runAsManager = new RunAsManager();
                runAsManager.buildRunAs( object, authentication, recipient );

            } else {
                recipient = principal.toString();
            }
        } else {
            throw new RuntimeException( "User '" + principal
                    + "' is not authorized to execute this method, you must be an 'administrator'." );
        }
        return recipient;
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
     * Returns true if the mask on the {@link Securable} is equal to the PRIVATE_MASK or false if it is equal to the
     * PUBLIC_MASK.
     * 
     * @param s
     * @return
     */
    public boolean isPrivate( Securable s ) {

        Integer mask = securableDao.getMask( s );

        if ( mask != null && mask.equals( PRIVATE_MASK ) ) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public java.util.Map<Securable, Boolean> arePrivate( Collection securables ) {
        Map<Securable, Integer> masks = securableDao.getMasks( securables );
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        for ( Securable s : masks.keySet() ) {
            Integer mask = masks.get( s );
            if ( mask.equals( PRIVATE_MASK ) ) {
                result.put( s, true );
            } else {
                result.put( s, false );
            }
        }
        return result;
    }

    /**
     * @param aclDao the aclDao to set
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    /**
     * @param securableDao the securableDao to set
     */
    public void setSecurableDao( SecurableDao securableDao ) {
        this.securableDao = securableDao;
    }

}
