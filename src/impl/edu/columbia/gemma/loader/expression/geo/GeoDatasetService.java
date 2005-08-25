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

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.expression.ExpressionLoaderImpl;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoFile;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.loaderutils.Persister;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetService {

    private static Log log = LogFactory.getLog( GeoDatasetService.class.getName() );
    private Persister expLoader;

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download, parse and convert the GDS file</li>
     * <li>Download, parse and convert the associated GSE file(s)</li>
     * <li>Load the resulting data into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    public void fetchAndLoad( String geoDataSetAccession ) throws ConfigurationException, SocketException, IOException {
        DatasetFetcher df = new DatasetFetcher();
        Collection<LocalFile> result = df.fetch( geoDataSetAccession );

        if ( result == null ) return;

        String localPath = result.getLocalPath();

        GeoFamilyParser gfp = new GeoFamilyParser();

        gfp.parse( localPath );

        Map<String, GeoDataset> datasetMap = gfp.getDatasets();
        if ( !datasetMap.containsKey( geoDataSetAccession ) )
            throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );

        GeoDataset gds = datasetMap.get( geoDataSetAccession );

        Collection<GeoSeries> seriesSet = gds.getSeries();
        SeriesFetcher sf = new SeriesFetcher();
        for ( GeoSeries series : seriesSet ) {
            log.info( "Processing series " + series );
            Collection<LocalFile> fullSeries = sf.fetch( series.getGeoAccesssion() );
            gfp.parseToMap( fullSeries.getLocalPath() );
        }

        ExpressionExperiment expexp = gfp.convertDataSet();

        if ( expexp == null ) throw new NullPointerException( "Got a null expressionExpression " );

        log.info( "Loading expressionExperiment: " + expexp.getAccession().getAccession() );
        assert expLoader != null;
        expLoader.persist( expexp );
    }

    public void setExpressionLoader( ExpressionLoaderImpl expressionLoader ) {
        this.expLoader = expressionLoader;
    }

}
