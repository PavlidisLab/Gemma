/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.loader.expression.geo.model;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Used to contain GEO summary information from the 'Browse' views.
 * <p>
 * This model is not intended for parsing GEO metadata in general, but rather get high-level metadata for browsing, use
 * {@link GeoPlatform}, {@link GeoSeries} and {@link GeoSample} for more advanced parsing scenarios.
 * @author pavlidis
 */
@Getter
@Setter
@SuppressWarnings("unused") // Possible external use
public class GeoRecord extends GeoData {

    private String summary = "";
    private String overallDesign = "";
    private String contactName = "";
    private Collection<Long> correspondingExperiments = new HashSet<>();
    @Nullable
    private Collection<String> organisms = new HashSet<>();
    private String platform = ""; // can be more than one here, for mixed data type series
    private Date releaseDate;
    private String seriesType = "";
    private boolean subSeries = false;
    private String subSeriesOf = "";
    private boolean superSeries = false;

    @Nullable
    private List<String> pubMedIds;
    /**
     * MeSh headings, collected from PubMed.
     */
    @Nullable
    private Collection<String> meshHeadings;

    // sample details
    private int numSamples = 0;
    private String libraryStrategy = "";
    private String librarySource = "";
    @Nullable
    private Collection<String> sampleGEOAccessions = new ArrayList<>();
    private String sampleDescriptions = "";
    private String sampleDetails = "";
    private String sampleMolecules = "";
    private String sampleExtractProtocols = "";
    private String sampleLabels = "";
    private String sampleLabelProtocols = "";
    private String sampleDataProcessing = "";

    /**
     * Curator judgement about whether this is loadable. False indicates a problem.
     */
    private boolean usable = true;

    /**
     * How many times a curator has already looked at the details. this helps us track data sets we've already examined
     * for usefulness.
     */
    private int previousClicks = 0;
}
