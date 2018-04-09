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
package ubic.gemma.core.loader.expression.geo;

import ubic.gemma.core.loader.expression.geo.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * This simply holds the results obtained from parsing.
 */
public class GeoParseResult {
    private final Map<String, GeoDataset> datasetMap;

    private final Map<String, GeoPlatform> platformMap;

    private final Map<String, GeoSample> sampleMap;

    private final Map<String, GeoSeries> seriesMap;

    private final Map<String, GeoSubset> subsetMap;

    public GeoParseResult() {
        sampleMap = new HashMap<>();
        platformMap = new HashMap<>();
        seriesMap = new HashMap<>();

        subsetMap = new HashMap<>();

        datasetMap = new HashMap<>();
    }

    /**
     * @return Returns the datasetMap.
     */
    public Map<String, GeoDataset> getDatasetMap() {
        return this.datasetMap;
    }

    public Map<String, GeoDataset> getDatasets() {
        return this.datasetMap;
    }

    /**
     * @return Returns the platformMap.
     */
    public Map<String, GeoPlatform> getPlatformMap() {
        return this.platformMap;
    }

    public Map<String, GeoPlatform> getPlatforms() {
        return this.platformMap;
    }

    /**
     * @return Returns the sampleMap.
     */
    public Map<String, GeoSample> getSampleMap() {
        return this.sampleMap;
    }

    public Map<String, GeoSample> getSamples() {
        return this.sampleMap;
    }

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
