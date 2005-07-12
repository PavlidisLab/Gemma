/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.experiment.ExperimentalDesign;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable;

/**
 * Convert GEO domain objects into Gemma objects.
 * <p>
 * GEO has three basic kinds of objects: Platforms (ArrayDesigns), Samples (BioAssays) and Series (Experiments). Note
 * that a sample can belong to more than one series.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverter {

    private static Log log = LogFactory.getLog( GeoConverter.class.getName() );

    private ExternalDatabase geoDatabase;

    public GeoConverter() {
        geoDatabase = ExternalDatabase.Factory.newInstance();
        geoDatabase.setName( "GEO" );
        geoDatabase.setType( DatabaseType.EXPRESSION );
    }

    /**
     * @param seriesMap
     */
    public void convert( Map<String, GeoSeries> seriesMap ) {
        for ( String seriesName : seriesMap.keySet() ) {

            GeoSeries series = seriesMap.get( seriesName );

            convertSeries( series );

        }
    }

    /**
     * @param series
     */
    private void convertSeries( GeoSeries series ) {
        log.info( "Converting series: " + series.getGeoAccesssion() );

        Collection<GeoVariable> variables = series.getVariables();

        ExperimentalDesign design = ExperimentalDesign.Factory.newInstance();

        ExpressionExperiment expExp = ExpressionExperiment.Factory.newInstance();
        expExp.setAccession( convertDatabaseEntry( series ) );

        expExp.setExperimentalDesigns( new HashSet() );
        expExp.getExperimentalDesigns().add( design );

        Collection<GeoSample> samples = series.getSamples();
        for ( GeoSample sample : samples ) {
            convert( sample );
        }

    }

    private DatabaseEntry convertDatabaseEntry( GeoData geoData ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();
        result.setExternalDatabase( this.geoDatabase );
        result.setAccession( geoData.getGeoAccesssion() );
        return result;
    }

    /**
     * @param sample
     */
    private void convert( GeoSample sample ) {
        log.info( "Converting sample: " + sample.getGeoAccesssion() );
        BioAssay bioAssay = BioAssay.Factory.newInstance();

        Collection<GeoPlatform> platforms = sample.getPlatforms();
        for ( GeoPlatform platform : platforms ) {

            convert( platform );
        }
    }

    /**
     * @param platform
     */
    private void convert( GeoPlatform platform ) {
        log.info( "Converting platform: " + platform.getGeoAccesssion() );
        // TODO Auto-generated method stub

    }

}
