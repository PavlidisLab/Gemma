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

package ubic.gemma.persistence.util;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cfg.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Allows us to configure the CacheManager separately from Hibernate, so we can use a single CacheManager for the whole
 * application.
 *
 * @author paul
 */
@Component
public class ExternalCacheRegionFactory extends EhCacheRegionFactory {

    @Autowired
    public ExternalCacheRegionFactory( CacheManager cacheManager ) {
        super( null );
        this.manager = ( ( EhCacheCacheManager ) cacheManager ).getCacheManager();
    }

    @Override
    public void start( Settings s, Properties p ) throws CacheException {
        mbeanRegistrationHelper.registerMBean( manager, p );
    }

}
