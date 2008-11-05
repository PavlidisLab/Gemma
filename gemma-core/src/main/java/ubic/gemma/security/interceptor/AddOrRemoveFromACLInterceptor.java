/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.security.interceptor;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acl.basic.AbstractBasicAclEntry;
import org.springframework.security.acl.basic.AclObjectIdentity;
import org.springframework.security.acl.basic.BasicAclExtendedDao;
import org.springframework.security.acl.basic.NamedEntityObjectIdentity;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.model.association.Relationship;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserImpl;
import ubic.gemma.model.common.auditAndSecurity.UserRoleImpl;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.UnsecuredSecurableSet;
import ubic.gemma.security.acl.basic.jdbc.CustomAclDao;
import ubic.gemma.security.acl.basic.jdbc.CustomJdbcExtendedDaoImpl;
import ubic.gemma.security.principal.UserDetailsServiceImpl;
import ubic.gemma.util.ReflectionUtil;

/**
 * Adds security controls to newly created objects, and removes them for objects
 * that are deleted. Methods in this interceptor are run for all new objects (to
 * add security if needed) and when objects are deleted.
 * <p>
 * Implementation Note: For permissions modification to be triggered, the method
 * name must match certain patterns, which include "create" and "remove". Other
 * methods that would require changes to permissions will not work without
 * modifying the source code.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id: AddOrRemoveFromACLInterceptor.java,v 1.29 2008/10/20 04:04:13
 *          keshav Exp $
 * @see ubic.gemma.security.interceptor.AclPointcut
 * @spring.bean name="aclAdvice"
 * @spring.property name="crudUtils" ref="crudUtils"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 * @spring.property name="customAclDao" ref="customAclDao"
 */
@SuppressWarnings("deprecation")
public class AddOrRemoveFromACLInterceptor implements AfterReturningAdvice {

	CrudUtils crudUtils;

	public AddOrRemoveFromACLInterceptor() {
		this.crudUtils = new CrudUtils();
	}

	private static Log log = LogFactory
			.getLog(AddOrRemoveFromACLInterceptor.class.getName());

	/**
	 * For some types of Securables, we don't put permissions on them directly,
	 * but on the containing object. Example: reporter - we secure the
	 * arrayDesign, but not the reporter, even though Reporter is Securable.
	 */
	private static final Class[] additionalSecurableClasses = {
			DatabaseEntry.class, Relationship.class, Taxon.class,
			GeneAlias.class };
	private static final Collection<Class> unsecuredClasses = new UnsecuredSecurableSet(
			additionalSecurableClasses);

	private AbstractBasicAclEntry simpleAclEntry;

	private BasicAclExtendedDao basicAclExtendedDao;

	private CustomAclDao customAclDao;

	/**
	 * Creates the acl_permission object and the acl_object_identity object.
	 * 
	 * @param object
	 *            The domain object.
	 */
	public void addPermission( Securable object ) {

        /*
         * When adding a new user to the system, make sure they can see the public data by adding a control node for
         * that user and set the acl_object_identity of this to CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID
         */
        if ( object instanceof UserImpl ) {
            User u = ( User ) object;
            String recipient = u.getUserName();
            customAclDao.insertPublicAccessControlNodeForRecipient( recipient, SimpleAclEntry.READ );
        }

        /*
         * Set up the SimpleAclEntry, which includes both acl_permission and acl_object_identity stuff.
         */
        // When persisting to the database, the acl_permission may not be
        // persisted.
        // Create acl_permission entry in database only if:
        // 1. creating a new user
        // 2. adding Securable(s) and not logged in as an admin. If user is
        // logged in
        // as an admin, the data they load will be public because their
        // corresponding
        // acl_object_identity.parent_object will be set to
        // CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID.
        /*                                                                                              */
        simpleAclEntry = this.createNonPersistentAclEntry( object );

        try {
            boolean isAdmin = SecurityService.isUserAdmin();

            boolean createAclPermission = false;// default case for admin

            if ( object instanceof UserImpl ) createAclPermission = true;

            if ( object instanceof UserRoleImpl ) createAclPermission = true;

            if ( !isAdmin ) createAclPermission = true;

            ( ( CustomJdbcExtendedDaoImpl ) basicAclExtendedDao ).create( simpleAclEntry, createAclPermission );

        } catch ( DataIntegrityViolationException ignored ) {

            // This happens in two situations:
            // 1. When a 'findOrCreate' resulted in just a 'find'.
            // 2. When a create was called, but some associated object was
            // already in the system.
            //              
            // Either way, we can ignore it.
            //              
            //
            // if ( method.getName().equals( "findOrCreate" ) ) {
            // do nothing. This happens when the object already exists and has
            // permissions assigned (for example,
            // findOrCreate resulted in a 'find')
            // } else {
            // something else must be wrong
            // log.fatal( method.getName() + " on " + getAuthority() + " for
            // recipient " + getUsername() + " on " +
            // object, ignored );
            // throw ( ignored );
            // }
        }
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object,
	 *      java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	@SuppressWarnings( { "unused", "unchecked" })
	public void afterReturning(Object retValue, Method m, Object[] args,
			Object target) throws Throwable {

		assert args != null;
		Object persistentObject = getPersistentObject(retValue, m, args);

		Session sess = crudUtils.getSessionFactory().openSession();

		try {
			Hibernate.initialize(persistentObject);
		} catch (HibernateException e) {
			// this can result in a second objet being created if the object is
			// already in a session that has not been
			// flushed.
			persistentObject = sess.merge(persistentObject);
		}

		if (persistentObject == null)
			return;
		if (Collection.class.isAssignableFrom(persistentObject.getClass())) {
			for (Object o : (Collection<Object>) persistentObject) {
				if (!Securable.class.isAssignableFrom(persistentObject
						.getClass())) {
					return; // they will all be the same type.
				}
				processObject(m, o);
			}
		} else { // note that check for securable is in the pointcut.
			processObject(m, persistentObject);
		}

		sess.close();
	}

	/**
	 * Delete acl permissions for an object.
	 * 
	 * @param object
	 * @throws IllegalArgumentException
	 * @throws DataAccessException
	 */
	public void deletePermission(Securable object) throws DataAccessException,
			IllegalArgumentException {
		if (object == null)
			return;
		try {
			basicAclExtendedDao.delete(makeObjectIdentity(object));
		} catch (org.springframework.dao.DataRetrievalFailureException e) {
			/*
			 * this happens during tests where we have flushed during a
			 * transaction. It is also observed in some other situations where
			 * the object has already been deleted (due to a cascade?)
			 */
			log.warn("Could not delete aclObjectIdentity for " + object + "("
					+ e.getMessage() + ")");
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleted object " + object
					+ " ACL permissions for recipient "
					+ UserDetailsServiceImpl.getCurrentUsername());
		}
	}

	/**
	 * @param basicAclExtendedDao
	 */
	public void setBasicAclExtendedDao(BasicAclExtendedDao basicAclExtendedDao) {
		this.basicAclExtendedDao = basicAclExtendedDao;
	}

	/**
	 * @param retValue
	 * @param m
	 * @param args
	 * @return
	 */
	private Object getPersistentObject(Object retValue, Method m, Object[] args) {
		if (CrudUtils.methodIsDelete(m) || CrudUtils.methodIsUpdate(m)) {
			assert args.length > 0;
			return args[0];
		}
		return retValue;
	}

	/**
	 * @param m
	 * @param object
	 */
	private void processAssociations(Method m, Object object)
			throws IllegalAccessException, InvocationTargetException {

		EntityPersister persister = crudUtils.getEntityPersister(object);
		if (persister == null) {
			// FIXME this happens when the object is a proxy.
			log.error("No Entity Persister found for "
					+ object.getClass().getName());
			return;
		}
		CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
		String[] propertyNames = persister.getPropertyNames();

		for (int j = 0; j < propertyNames.length; j++) {
			CascadeStyle cs = cascadeStyles[j];
			if (!crudUtils.needCascade(m, cs)) {
				// log.debug( "Not processing association " + propertyNames[j] +
				// ", Cascade=" + cs );
				continue;
			}

			PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(
					object.getClass(), propertyNames[j]);

			/*
			 * This can yield a lazy-load error if the property is not
			 * initialized...
			 */
			Object associatedObject = null;
			try {
				associatedObject = ReflectionUtil.getProperty(object,
						descriptor);
			} catch (Exception e) {
				log.fatal(e.getClass() + " while processing: "
						+ object.getClass() + " --> " + propertyNames[j]);
				throw (new RuntimeException(e));
			}

			if (associatedObject == null)
				continue;

			Class<?> propertyType = descriptor.getPropertyType();

			if (Securable.class.isAssignableFrom(propertyType)) {
				if (log.isDebugEnabled())
					log.debug("Processing ACL for " + propertyNames[j]
							+ ", Cascade=" + cs);
				processObject(m, associatedObject);
			} else if (Collection.class.isAssignableFrom(propertyType)) {

				/*
				 * This block was previously commented out because of lazy-load
				 * problems.
				 */
				Collection associatedObjects = (Collection) associatedObject;
				if (Hibernate.isInitialized(associatedObjects)) {
					for (Object object2 : associatedObjects) {
						if (Securable.class
								.isAssignableFrom(object2.getClass())) {
							if (log.isDebugEnabled()) {
								log.debug("Processing ACL for member "
										+ object2 + " of collection "
										+ propertyNames[j] + ", Cascade=" + cs);
							}
							processObject(m, object2);
						}
					}
				}
			}
		}
	}

	/**
	 * @param m
	 *            method that was called. This is used to determine what action
	 *            to take.
	 * @param object.
	 *            If null, no action is taken.
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void processObject(Method m, Object object)
			throws IllegalAccessException, InvocationTargetException {

		if (object == null)
			return;

		assert m != null;

		if (!Securable.class.isAssignableFrom(object.getClass())
				|| unsecuredClassesContains(object.getClass())) {
			if (log.isDebugEnabled()) {
				log
						.debug(object.getClass().getName()
								+ " is not a secured object, skipping permissions processing.");
			}
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug("Processing permissions for: "
					+ object.getClass().getName() + " for method "
					+ m.getName());
		}
		if (CrudUtils.methodIsCreate(m)) {
			addPermission((Securable) object);
			processAssociations(m, object);
		} else if (CrudUtils.methodIsDelete(m)) {
			deletePermission((Securable) object);
			processAssociations(m, object);
		} else {
			// nothing to do.
		}
	}

	/**
	 * @param class1
	 * @return
	 */
	private boolean unsecuredClassesContains(Class<? extends Object> c) {
		for (Class<? extends Object> clazz : unsecuredClasses) {
			if (clazz.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	/**
	 * @param object
	 * @return AbstractBasicAclEntry
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public AbstractBasicAclEntry createNonPersistentAclEntry( Securable object ) {
        SimpleAclEntry simpleAclEntry = new SimpleAclEntry();
        simpleAclEntry.setAclObjectIdentity( makeObjectIdentity( object ) );
        simpleAclEntry.setMask( getMaskByAuthority() );
        simpleAclEntry.setRecipient( UserDetailsServiceImpl.getCurrentUsername() );
        simpleAclEntry.setAclObjectParentIdentity( new NamedEntityObjectIdentity( CustomAclDao.PUBLIC_CONTROL_NODE,
                CustomAclDao.PUBLIC_CONTROL_NODE_PARENT_ID ) );

        /* If we are logged in, then we are adding private data. */
        if ( SecurityService.isUserLoggedIn() ) {
            simpleAclEntry.setAclObjectParentIdentity( new NamedEntityObjectIdentity( CustomAclDao.ADMIN_CONTROL_NODE,
                    CustomAclDao.ADMIN_CONTROL_NODE_PARENT_ID ) );
        }
        /*
         * Now check to see if the data we are adding is a new user (UserImpl). If so, decide what mask to use depending
         * on the type of user (i.e. based on role: user, admin).
         */
        if ( object instanceof UserImpl ) {
            simpleAclEntry.setMask( SimpleAclEntry.READ_WRITE );

            UserImpl newUser = ( UserImpl ) object;

            simpleAclEntry.setRecipient( newUser.getUserName() );

            if ( SecurityService.isUserAdmin() ) {
                simpleAclEntry.setMask( SimpleAclEntry.ADMINISTRATION );
                // FIXME - do we need to add a parent other than the global if
                // we are adding another admin to the
                // system.
            }
        }

        /*
         * Check if we are adding a role (UserRoleImpl). If so, set the mask to SimpleAclEntry.READ so the user cannot
         * change their role.
         */
        if ( object instanceof UserRoleImpl ) {
            simpleAclEntry.setMask( SimpleAclEntry.READ );

            UserRoleImpl role = ( UserRoleImpl ) object;

            simpleAclEntry.setRecipient( role.getUserName() );
        }

        return simpleAclEntry;
    }

	/**
	 * @param object
	 */
	private static boolean checkValidPrimaryKey(Securable object) {
		Class<? extends Securable> clazz = object.getClass();
		try {
			String methodName = "getId";
			Method m = clazz.getMethod(methodName, new Class[] {});
			Object result = m.invoke(object, new Object[] {});
			if (result == null) {
				return false;
			}
		} catch (NoSuchMethodException nsme) {
			throw new IllegalArgumentException("Object of class '" + clazz
					+ "' does not provide the required getId() method: "
					+ object);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * Forms the object identity to be inserted in acl_object_identity table.
	 * 
	 * @param object
	 * @return object identity.
	 */
	private static AclObjectIdentity makeObjectIdentity(Securable object) {
		assert checkValidPrimaryKey(object) : "No valid primary key for object "
				+ object;
		try {
			return new NamedEntityObjectIdentity(object);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * When an 'admin' loads data, we want to make this readable by everyone so
	 * we set the mask to SimpleAclEntry.READ (anonymous). When a 'user' loads
	 * data, we want to make this readable and writeable by that user only so we
	 * set the mask to SimpleAclEntry.READ_WRITE.
	 * 
	 * @return Integer
	 */
	protected static Integer getMaskByAuthority() {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		GrantedAuthority[] authorities = auth.getAuthorities();
		for (GrantedAuthority a : authorities) {
			if (a.getAuthority() == "admin") {
				/*
				 * When an admin loads data, we want to make this readable by
				 * everyone (anonymous) ... unless the admin adds a new user to
				 * the system in which case the new user should should be
				 * readable and writeable by himself/herself. This handled later
				 * by using the UserRole of the User that was added.
				 */
				return SimpleAclEntry.READ;

			}
		}
		/*
		 * When a user loads data, we want to make this readable and writeable
		 * by that user
		 */
		return SimpleAclEntry.READ_WRITE;
	}

	/**
	 * @param crudUtils
	 *            the crudUtils to set
	 */
	public void setCrudUtils(CrudUtils crudUtils) {
		this.crudUtils = crudUtils;
	}

	/**
	 * @param customAclDao
	 */
	public void setCustomAclDao(CustomAclDao customAclDao) {
		this.customAclDao = customAclDao;
	}
}
