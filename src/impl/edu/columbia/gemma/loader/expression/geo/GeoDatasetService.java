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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.loaderutils.Converter;
import edu.columbia.gemma.loader.loaderutils.Persister;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
import edu.columbia.gemma.loader.loaderutils.SourceDomainObjectGenerator;

/**
 * Non-interactive fetching, processing and persisting of GEO data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetService {

    private static Log log = LogFactory.getLog( GeoDatasetService.class.getName() );
    private SourceDomainObjectGenerator generator;
    private Persister expLoader;
    private Converter converter;

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download and parse GDS file</li>
     * <li>Download and parse GSE family file(s).</li>
     * <li>Convert the GDS and GSE into a ExpressionExperiment.
     * <li>Load the resulting ExpressionExperiment into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    @SuppressWarnings("unchecked")
    public void fetchAndLoad( String geoDataSetAccession ) {

        generator = new GeoDomainObjectGenerator();

        GeoSeries series = ( GeoSeries ) generator.generate( geoDataSetAccession ).iterator().next();

        log.info( "Generated GEO domain objects for " + geoDataSetAccession );

        ExpressionExperiment result = ( ExpressionExperiment ) converter.convert( series );

        log.info( "Converted " + series.getGeoAccession() );

        assert expLoader != null;
        expLoader.persist( result );

        log.info( "Persisted " + series.getGeoAccession() );
    }

    /**
     * @param expressionLoader
     */
    public void setPersister( PersisterHelper expressionLoader ) {
        this.expLoader = expressionLoader;
    }

    /**
     * @param geoConv to set
     */
    public void setConverter( Converter geoConv ) {
        this.converter = geoConv;
    }

}
