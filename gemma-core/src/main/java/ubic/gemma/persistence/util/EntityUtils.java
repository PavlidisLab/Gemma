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
package ubic.gemma.persistence.util;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.acl.domain.AclEntry;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Sid;
import ubic.gemma.model.common.Identifiable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author paul
 */
public class EntityUtils {

    /**
     * Checks if the given map already contains a Set for the given key, and if it does, adds the given has code to it.
     * If a key does not exist in the map, it creates a new set, and adds the has into it.
     *
     * @param map  the map to be checked
     * @param key  the key
     * @param hash the hash to be added to the set at the given key of the map
     * @param <T>  the key type parameter of the map
     * @param <S>  the type parameter of the set
     */
    public static <T, S> void populateMapSet( Map<T, Set<S>> map, T key, S hash ) {
        if ( map.containsKey( key ) ) {
            map.get( key ).add( hash );
        } else {
            Set<S> set = new HashSet<>();
            if ( hash != null ) {
                set.add( hash );
            }
            map.put( key, set );
        }
    }

    /**
     * @param entities entities
     * @return returns a collection of IDs. Avoids using reflection by requiring that the given entities all
     * implement the Identifiable interface.
     */
    public static <T extends Identifiable> List<Long> getIds( Collection<T> entities ) {
        return entities.stream().map( Identifiable::getId ).collect( Collectors.toList() );
    }

    /**
     * Given a set of entities, create a map of their ids to the entities.
     *
     * Note: If more than one entity share the same ID, there is no guarantee on which will be kept in the final
     * mapping.
     *
     * @param entities where id is called "id"
     * @param <T>      the type
     * @return the created map
     */
    public static <T extends Identifiable> Map<Long, T> getIdMap( Collection<T> entities ) {
        Map<Long, T> result = new HashMap<>();
        for ( T entity : entities ) {
            result.put( entity.getId(), entity );
        }
        return result;
    }

    public static Class<?> getImplClass( Class<?> type ) {
        String canonicalName = type.getName();
        try {
            return Class.forName( canonicalName.endsWith( "Impl" ) ? type.getName() : type.getName() + "Impl" );
        } catch ( ClassNotFoundException e ) {
            // OLDCODE throw new RuntimeException( "No 'impl' class for " + type.getCanonicalName() + " found" );
            // not all our mapped classes end with Impl any more.
            return type;
        }
    }

    /**
     * Expert only. Must be called within a session? Not sure why this is necessary. Obtain the implementation for a
     * proxy. If target is not an instanceof HibernateProxy, target is returned.
     *
     * @param target The object to be un-proxied.
     * @return the underlying implementation.
     */
    public static Object getImplementationForProxy( Object target ) {
        if ( EntityUtils.isProxy( target ) ) {
            HibernateProxy proxy = ( HibernateProxy ) target;
            return proxy.getHibernateLazyInitializer().getImplementation();
        }
        return target;
    }

    /**
     * @param target target
     * @return true if the target is a hibernate proxy.
     */
    public static boolean isProxy( Object target ) {
        return target instanceof HibernateProxy;
    }

    /**
     * Have to add 'and' to start of this if it's a later clause
     * Author: nicolas with fixes to generalize by paul, same code appears in the PhenotypeAssociationDaoImpl
     *
     * @param showOnlyEditable only show those the user has access to edit
     * @param showPublic       also show public items (wont work if showOnlyEditable is true)
     * @return clause
     */
    public static String addGroupAndUserNameRestriction( boolean showOnlyEditable, boolean showPublic ) {

        String sqlQuery = "";

        if ( !SecurityUtil.isUserAnonymous() ) {

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "  ((sid.PRINCIPAL = :userName";
            } else {
                sqlQuery += "  (sid.PRINCIPAL = :userName";
            }

            // if ( !groups.isEmpty() ) { // is it possible to be empty? If they are non-anonymous it will not be, there
            // // will at least be GROUP_USER? Or that doesn't count?
            sqlQuery += " or (ace.SID_FK in (";
            // SUBSELECT
            sqlQuery += " select sid.ID from USER_GROUP ug ";
            sqlQuery += " join GROUP_AUTHORITY ga on ug.ID = ga.GROUP_FK ";
            sqlQuery += " join ACLSID sid on sid.GRANTED_AUTHORITY=CONCAT('GROUP_', ga.AUTHORITY) ";
            sqlQuery += " where ug.name in (:groups) ";
            if ( showOnlyEditable ) {
                sqlQuery += ") and ace.MASK = 2) "; // 2 = read-writable
            } else {
                sqlQuery += ") and (ace.MASK = 1 or ace.MASK = 2)) "; // 1 = read only
            }
            // }
            sqlQuery += ") ";

            if ( showPublic && !showOnlyEditable ) {
                // publicly readable data.
                sqlQuery += "or (ace.SID_FK = 4 and ace.MASK = 1)) "; // 4 =IS_AUTHENTICATED_ANONYMOUSLY
            }

        } else if ( showPublic && !showOnlyEditable ) {
            // publicly readable data
            sqlQuery += " (ace.SID_FK = 4 and ace.MASK = 1) "; // 4 = IS_AUTHENTICATED_ANONYMOUSLY
        }

        return sqlQuery;
    }

    /**
     * Populates parameters in query created using addGroupAndUserNameRestriction(boolean, boolean).
     *
     * @param queryObject    the query object created using the sql query with group and username restrictions.
     * @param sessionFactory session factory from the DAO that is using this method.
     */
    public static void addUserAndGroupParameters( SQLQuery queryObject, SessionFactory sessionFactory ) {
        if ( SecurityUtil.isUserAnonymous() ) {
            return;
        }
        String sqlQuery = queryObject.getQueryString();
        String userName = SecurityUtil.getCurrentUsername();

        // if user is member of any groups.
        if ( sqlQuery.contains( ":groups" ) ) {
            //noinspection unchecked
            Collection<String> groups = sessionFactory.getCurrentSession().createQuery(
                            "select ug.name from UserGroup ug inner join ug.groupMembers memb where memb.userName = :user" )
                    .setParameter( "user", userName ).list();
            queryObject.setParameterList( "groups", optimizeParameterList( groups ) );
        }

        if ( sqlQuery.contains( ":userName" ) ) {
            queryObject.setParameter( "userName", userName );
        }

    }

    /**
     * Checks ACL related properties from the AclObjectIdentity.
     * Some of the code is adapted from {@link gemma.gsec.util.SecurityUtil}, but allows usage without an Acl object.
     *
     * @param aoi the acl object identity of an object whose permissions are to be checked.
     * @return an array of booleans that represent permissions of currently logged in user as follows:
     * <ol>
     * <li>is object public</li>
     * <li>can user write to object</li>
     * <li>is object shared</li>
     * </ol>
     * (note that actual indexing in the array starts at 0).
     */
    public static boolean[] getPermissions( AclObjectIdentity aoi ) {
        boolean isPublic = false;
        boolean canWrite = false;
        boolean isShared = false;

        for ( AclEntry ace : aoi.getEntries() ) {
            if ( SecurityUtil.isUserAdmin() ) {
                canWrite = true;
            } else if ( SecurityUtil.isUserAnonymous() ) {
                canWrite = false;
            } else {
                if ( ( ace.getMask() & BasePermission.WRITE.getMask() ) != 0 || ( ace.getMask() & BasePermission.ADMINISTRATION.getMask() ) != 0 ) {
                    Sid sid = ace.getSid();
                    if ( sid instanceof AclGrantedAuthoritySid ) {
                        //noinspection unused //FIXME if user is in granted group then he can write probably
                        String grantedAuthority = ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority();
                    } else if ( sid instanceof AclPrincipalSid ) {
                        if ( ( ( AclPrincipalSid ) sid ).getPrincipal().equals( SecurityUtil.getCurrentUsername() ) ) {
                            canWrite = true;
                        }
                    }
                }
            }

            // Check public and shared - code adapted from SecurityUtils, only we do not hold an ACL object.
            if ( ( ace.getMask() & BasePermission.READ.getMask() ) != 0 ) {
                Sid sid = ace.getSid();
                if ( sid instanceof AclGrantedAuthoritySid ) {
                    String grantedAuthority = ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority();

                    if ( grantedAuthority.equals( AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) && ace
                            .isGranting() ) {
                        isPublic = true;
                    }
                    if ( grantedAuthority.startsWith( "GROUP_" ) && ace.isGranting() ) {
                        if ( !grantedAuthority.equals( AuthorityConstants.AGENT_GROUP_AUTHORITY ) && !grantedAuthority
                                .equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                            isShared = true;
                        }
                    }
                }
            }
        }

        return new boolean[] { isPublic, canWrite, isShared };
    }

    public static void createFile( File file ) throws IOException {
        if ( !file.createNewFile() ) {
            Exception e = new RuntimeException( "Could not create file " + file.getPath() );
            e.printStackTrace();
        }
    }

    public static void deleteFile( File file ) {
        if ( !file.delete() ) {
            Exception e = new RuntimeException( "Could not delete file " + file.getPath() );
            e.printStackTrace();
        }
    }

    public static void renameFile( File file, File newFile ) {
        if ( !file.renameTo( newFile ) ) {
            Exception e = new RuntimeException( "Could not rename file " + file.getPath() );
            e.printStackTrace();
        }
    }
}

