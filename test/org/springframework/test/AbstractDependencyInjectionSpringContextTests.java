/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Convenient superclass for tests depending on a Spring context.
 * The test instance itself is populated by Dependency Injection.
 *
 * <p>Really for integration testing, not unit testing.
 * You should <i>not</i> normally use the Spring container
 * for unit tests: simply populate your POJOs in plain JUnit tests!
 *
 * <p>This supports two modes of populating the test:
 * <ul>
 * <li>Via Setter Dependency Injection. Simply express dependencies on objects
 * in the test fixture, and they will be satisfied by autowiring by type.
 * <li>Via Field Injection. Declare protected variables of the required type
 * which match named beans in the context. This is autowire by name,
 * rather than type. This approach is based on an approach originated by
 * Ara Abrahmian. Setter Dependency Injection is the default: set the
 * "populateProtectedVariables" property to true in the constructor to switch
 * on Field Injection.
 * </ul>
 *
 * <p>This class will normally cache contexts based on a <i>context key</i>:
 * normally the config locations String array describing the Spring resource
 * descriptors making up the context. Unless the <code>setDirty()</code> method
 * is called by a test, the context will not be reloaded, even across different
 * subclasses of this test. This is particularly beneficial if your context is
 * slow to construct, for example if you are using Hibernate and the time taken
 * to load the mappings is an issue.
 *
 * <p>If you don't want this behavior, you can override the <code>contextKey()</code>
 * method, most likely to return the test class. In conjunction with this you would
 * probably override the <code>getContext</code> method, which by default loads
 * the locations specified in the <code>getConfigLocations()</code> method.
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see #setDirty
 * @see #contextKey
 * @see #getContext
 * @see #getConfigLocations
 */
public abstract class AbstractDependencyInjectionSpringContextTests extends AbstractSpringContextTests {

	private boolean populateProtectedVariables;
	
	private boolean dependencyCheck = true;

	/**
	 * Key for the context.
	 * This enables multiple contexts to share the same key.
	 */
	private Object contextKey;

	/**
	 * Application context this test will run against.
	 */
	protected ConfigurableApplicationContext applicationContext;

	protected String[] managedVariableNames;

	private int loadCount = 0;


	public void setPopulateProtectedVariables(boolean populateFields) {
		this.populateProtectedVariables = populateFields;
	}

	public boolean isPopulateProtectedVariables() {
		return populateProtectedVariables;
	}
	
	/**
	 * Set whether or not dependency checking should be performed
	 * for test properties set by Dependency Injection. The default
	 * is true, meaning that tests cannot be run unless all properties
	 * are populated.
	 * @param dependencyCheck whether or not to perform
	 * dependency checking on all object properties.
	 */
	public void setDependencyCheck(boolean dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}
	
	public boolean isDependencyCheck() {
		return dependencyCheck;
	}

	public final int getLoadCount() {
		return loadCount;
	}

	/**
	 * Called to say that the applicationContext instance variable is dirty and
	 * should be reloaded. We need to do this if a test has modified the context
	 * (for example, by replacing a bean definition).
	 */
	public void setDirty() {
		setDirty(getConfigLocations());
	}


	protected final void setUp() throws Exception {
		if (this.contextKey == null) {
			this.contextKey = contextKey();
		}

		this.applicationContext = getContext(this.contextKey);

		if (isPopulateProtectedVariables()) {
			if (this.managedVariableNames == null) {
				initManagedVariableNames();
			}
			populateProtectedVariables();
		}
		else {
			this.applicationContext.getBeanFactory().autowireBeanProperties(
			    this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, isDependencyCheck());
		}

		try {
			onSetUp();
		}
		catch (Exception ex) {
			logger.error("Setup error: " + ex);
			throw ex;
		}
	}

	/**
	 * Return a key for this context. Usually based on config locations, but
	 * a subclass overriding buildContext() might want to return its class.
	 * Called once and cached.
	 */
	protected Object contextKey() {
		return getConfigLocations();
	}

	protected ConfigurableApplicationContext loadContextLocations(String[] locations) {
		++this.loadCount;
		return super.loadContextLocations(locations);
	}

	protected void initManagedVariableNames() throws IllegalAccessException {
		LinkedList managedVarNames = new LinkedList();
		Class clazz = getClass();

		do {
			Field[] fields = clazz.getDeclaredFields();
			logger.debug(fields.length + " fields on " + clazz);

			for (int i = 0; i < fields.length; i++) {
				// TODO go up tree but not to this class
				Field f = fields[i];
				f.setAccessible(true);
				logger.debug("Candidate field " + f);
				if (!Modifier.isStatic(f.getModifiers()) && Modifier.isProtected(f.getModifiers())) {
					Object oldValue = f.get(this);
					if (oldValue == null) {
						managedVarNames.add(f.getName());
						logger.info("Added managed variable '" + f.getName() + "'");
					}
					else {
						logger.info("Rejected managed variable '" + f.getName() + "'");
					}
				}
			}
			clazz = clazz.getSuperclass();
		}
		while (clazz != AbstractSpringContextTests.class);

		this.managedVariableNames = (String[]) managedVarNames.toArray(new String[managedVarNames.size()]);
	}

	protected void populateProtectedVariables() throws IllegalAccessException {
		for (int i = 0; i < this.managedVariableNames.length; i++) {
			Object bean = null;
			Field f = null;
			try {
				f = findField(getClass(), this.managedVariableNames[i]);
				// TODO what if not found?
				bean = this.applicationContext.getBean(this.managedVariableNames[i]);
				f.setAccessible(true);
				f.set(this, bean);
				logger.info("Populated " + f);
			}
			catch (NoSuchFieldException ex) {
				logger.warn("No field with name '" + this.managedVariableNames[i] + "'");
			}
			catch (IllegalArgumentException ex) {
				logger.error("Value " + bean + " not compatible with " + f);
			}
			catch (NoSuchBeanDefinitionException ex) {
				logger.warn("No bean with name '" + this.managedVariableNames[i] + "'");
			}
		}
	}

	private Field findField(Class clazz, String name) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(name);
		}
		catch (NoSuchFieldException ex) {
			Class superclass = clazz.getSuperclass();
			if (superclass != AbstractSpringContextTests.class) {
				return findField(superclass, name);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * Subclasses can override this method in place of
	 * the setUp() method, which is final in this class.
	 * This implementation does nothing.
	 */
	protected void onSetUp() throws Exception {
	}


	/**
	 * Reload the context if it's marked as dirty.
	 * @see #onTearDown
	 */
	protected final void tearDown() {
		try {
			onTearDown();
		}
		catch (Exception ex) {
			logger.error("onTearDown error", ex);
		}
	}

	/**
	 * Subclasses can override this to add custom behavior on teardown.
	 */
	protected void onTearDown() throws Exception {
	}


	/**
	 * Subclasses must implement this method to return the locations of their
	 * config files. A plain path will be treated as class path location.
	 * E.g.: "org/springframework/whatever/foo.xml". Note however that you may
     * prefix path locations with standard Spring resource prefixes. Therefore,
     * a config location path prefixed with "classpath:" with behave the same
     * as a plain path, but a config location such as
     * "file:/some/path/path/location/appContext.xml" will be treated as a 
     * filesystem location.
	 * @return an array of config locations
	 */
	protected abstract String[] getConfigLocations();

}
