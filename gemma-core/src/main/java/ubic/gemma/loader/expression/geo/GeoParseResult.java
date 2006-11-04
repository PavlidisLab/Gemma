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
package ubic.gemma.loader.expression.geo;

import java.util.HashMap;
import java.util.Map;

import ubic.gemma.loader.expression.geo.model.GeoDataset;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSample;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.loader.expression.geo.model.GeoSubset;

/**
 * This simply holds the results obtained from parsing.
 */
public class GeoParseResult {
    private Map<String, GeoDataset> datasetMap;

    private Map<String, GeoPlatform> platformMap;

    private Map<String, GeoSample> sampleMap;

    private Map<String, GeoSeries> seriesMap;

    private Map<String, GeoSubset> subsetMap;

    public GeoParseResult() {
        sampleMap = new HashMap<String, GeoSample>();
        platformMap = new HashMap<String, GeoPlatform>();
        seriesMap = new HashMap<String, GeoSeries>();

        subsetMap = new HashMap<String, GeoSubset>();

        datasetMap = new HashMap<String, GeoDataset>();
    }

    /**
     * @return Returns the datasetMap.
     */
    public Map<String, GeoDataset> getDatasetMap() {
        return this.datasetMap;
    }

    /**
     * @return
     */
    public Map<String, GeoDataset> getDatasets() {
        return this.datasetMap;
    }

    /**
     * @return Returns the platformMap.
     */
    public Map<String, GeoPlatform> getPlatformMap() {
        return this.platformMap;
    }

    /**
     * @return
     */
    public Map<String, GeoPlatform> getPlatforms() {
        return this.platformMap;
    }

    /**
     * @return Returns the sampleMap.
     */
    public Map<String, GeoSample> getSampleMap() {
        return this.sampleMap;
    }

    /**
     * @return
     */
    public Map<String, GeoSample> getSamples() {
        return this.sampleMap;
    }

    /**
     * @return
     */
    public Map<String, GeoSeries> getSeries() {
        return this.seriesMap;
    }

    /**
     * @return Returns the seriesMap.
     */
    public Map<String, GeoSeries> getSeriesMap() {
        return this.seriesMap;
    }

    /**
     * @return Returns the subsetMap.
     */
    public Map<String, GeoSubset> getSubsetMap() {
        return this.subsetMap;
    }

}
