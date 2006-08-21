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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractGeoService {

    protected static Log log = LogFactory.getLog( AbstractGeoService.class );
    protected GeoDomainObjectGenerator generator;
    protected Persister persisterHelper;
    protected Converter converter;
    protected boolean loadPlatformOnly = false;

    /**
     * This is supplied to allow plugging in non-standard generators for testing (e.g., using local files only)
     * 
     * @param generator
     */
    public void setGenerator( GeoDomainObjectGenerator generator ) {
        this.generator = generator;
    }

    /**
     * @param expressionLoader
     */
    public void setPersister( PersisterHelper expressionLoader ) {
        this.persisterHelper = expressionLoader;
    }

    /**
     * @param geoConv to set
     */
    public void setConverter( Converter geoConv ) {
        this.converter = geoConv;
    }

    /**
     * @param geoAccession
     * @return
     */
    public abstract Object fetchAndLoad( String geoAccession );

    public void setLoadPlatformOnly( boolean b ) {
        this.loadPlatformOnly = b;
    }
}
