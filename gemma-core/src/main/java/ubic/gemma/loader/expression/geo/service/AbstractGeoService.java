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
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractGeoService {

    protected static Log log = LogFactory.getLog( AbstractGeoService.class );

    protected GeoDomainObjectGenerator geoDomainObjectGenerator;

    @Autowired
    protected PersisterHelper persisterHelper;

    @Autowired
    protected ArrayDesignService arrayDesignService;

    @Autowired
    protected GeoConverter geoConverter;

    /**
     * @param geoAccession
     * @param allowSuperSeriesImport TODO
     * @return
     */
    public abstract Collection<?> fetchAndLoad( String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQuantitationTypeRemoval, boolean splitIncompatiblePlatforms,
            boolean allowSuperSeriesImport );

    /**
     * This is supplied to allow clients to check that the generator has been set correctly.
     * 
     * @return
     */
    public GeoDomainObjectGenerator getGeoDomainObjectGenerator() {
        return this.geoDomainObjectGenerator;
    }

    /**
     * @param arrayDesignService the arrayDesignService to set
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param geoConv to set
     */
    public void setGeoConverter( GeoConverter geoConverter ) {
        this.geoConverter = geoConverter;
    }

    /**
     * This is supplied to allow plugging in non-standard generators for testing (e.g., using local files only)
     * 
     * @param generator
     */
    public void setGeoDomainObjectGenerator( GeoDomainObjectGenerator generator ) {
        this.geoDomainObjectGenerator = generator;
    }

    /**
     * @param expressionLoader
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
