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
package ubic.gemma.loader.expression.geo.service;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.Persister;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractGeoService implements BeanFactoryAware, GeoService {

    protected static Log log = LogFactory.getLog( AbstractGeoService.class );

    protected GeoDomainObjectGenerator geoDomainObjectGenerator;

    @Autowired
    protected Persister persisterHelper;

    @Autowired
    protected ArrayDesignService arrayDesignService;

    protected BeanFactory beanFactory;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoService#fetchAndLoad(java.lang.String, boolean, boolean,
     * boolean, boolean, boolean, boolean)
     */
    @Override
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms,
            boolean allowSuperSeriesImport, boolean allowSubSeriesImport );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoService#fetchAndLoad(java.lang.String, boolean, boolean,
     * boolean, boolean)
     */
    @Override
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.geo.service.GeoService#getGeoDomainObjectGenerator()
     */
    @Override
    public GeoDomainObjectGenerator getGeoDomainObjectGenerator() {
        return this.geoDomainObjectGenerator;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    @Override
    public void setGeoDomainObjectGenerator( GeoDomainObjectGenerator generator ) {
        this.geoDomainObjectGenerator = generator;
    }

    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    @Override
    public void setBeanFactory( BeanFactory beanFactory ) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
