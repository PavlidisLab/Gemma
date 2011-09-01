/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.persistence;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.EhCacheRegionFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Allows us to configure the CacheManager separately from Hibernate, so we can use a single CacheManager for the whole
 * application.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class ExternalCacheRegionFactory extends EhCacheRegionFactory implements ApplicationContextAware,
        InitializingBean {

    private ApplicationContext ctx;

    public ExternalCacheRegionFactory() {
        super( null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        /*
         * I have to do this rather than @Autowire because the manager is already declared in the superclass (yes, I can
         * reimplement the whole thing ...)
         */
        this.manager = ctx.getBean( CacheManager.class );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.
     * ApplicationContext)
     */
    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.ctx = applicationContext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.ehcache.hibernate.EhCacheRegionFactory#start(org.hibernate.cfg.Settings, java.util.Properties)
     */
    @Override
    public void start( Settings s, Properties p ) throws CacheException {
        assert this.manager != null;
        mbeanRegistrationHelper.registerMBean( manager, p );
        return;
    }

}
