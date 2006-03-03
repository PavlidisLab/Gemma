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
package edu.columbia.gemma.loader.expression.geo;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.loaderutils.Converter;
import edu.columbia.gemma.loader.loaderutils.Persister;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetService {

    private static Log log = LogFactory.getLog( GeoDatasetService.class.getName() );
    private GeoDomainObjectGenerator generator;
    private Persister persisterHelper;
    private Converter converter;
    private boolean loadPlatformOnly;

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download and parse GDS file</li>
     * <li>Download and parse GSE family file(s).</li>
     * <li>Convert the GDS and GSE into a ExpressionExperiment (or just the ArrayDesigns)
     * <li>Load the resulting ExpressionExperiment and/or ArrayDesigns into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    public Object fetchAndLoad( String geoDataSetAccession ) {

        generator.setProcessPlatformsOnly( false );
        if ( generator == null ) generator = new GeoDomainObjectGenerator();
        generator.setProcessPlatformsOnly( this.loadPlatformOnly );

        if ( this.loadPlatformOnly ) {
            Collection<Object> platforms = generator.generate( geoDataSetAccession );
            Collection<Object> arrayDesigns = converter.convert( platforms );
            return persisterHelper.persist( arrayDesigns );
        }

        GeoSeries series = ( GeoSeries ) generator.generate( geoDataSetAccession ).iterator().next();

        log.info( "Generated GEO domain objects for " + geoDataSetAccession );

        ExpressionExperiment result = ( ExpressionExperiment ) converter.convert( series );

        log.info( "Converted " + series.getGeoAccession() );
        series = null; // try to help GC.
        assert persisterHelper != null;
        return ( ExpressionExperiment ) persisterHelper.persist( result );

    }

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
     * @param b
     */
    public void setLoadPlatformOnly( boolean b ) {
        this.loadPlatformOnly = b;
    }

}
