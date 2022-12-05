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
package ubic.gemma.core.loader.expression.geo.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;

/**
 * @author pavlidis
 */
public abstract class AbstractGeoService implements BeanFactoryAware, GeoService {

    static final Log log = LogFactory.getLog( AbstractGeoService.class );
    @Autowired
    protected PersisterHelper persisterHelper;
    @Autowired
    protected ArrayDesignService arrayDesignService;
    GeoDomainObjectGenerator geoDomainObjectGenerator;
    BeanFactory beanFactory;

    @Override
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitByPlatform );

    @Override
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean splitByPlatform, boolean allowSuperSeriesImport, boolean allowSubSeriesImport );

    @Override
    public GeoDomainObjectGenerator getGeoDomainObjectGenerator() {
        return this.geoDomainObjectGenerator;
    }

    @Override
    public void setGeoDomainObjectGenerator( GeoDomainObjectGenerator generator ) {
        this.geoDomainObjectGenerator = generator;
    }

    @Override
    public void setBeanFactory( BeanFactory beanFactory ) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
