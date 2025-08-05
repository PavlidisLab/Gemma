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
package ubic.gemma.core.loader.expression.geo.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.GeoSampleCorrespondence;

import java.util.*;

/**
 * Represents a set of GEO samples that were submitted together. In many cases this corresponds to a full study, but for
 * studies that used more than one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
@CommonsLog
@Getter
@Setter
public class GeoSeries extends GeoData {

    private String seriesId;
    private String status;
    private String submissionDate;
    private String platformId;
    // use a LinkedHashSet for samples to preserve order
    private final Collection<GeoSample> samples = new LinkedHashSet<>();
    private final Collection<GeoSeriesType> seriesTypes = new HashSet<>();
    private final Collection<String> subSeries = new HashSet<>();
    private final Map<Integer, GeoVariable> variables = new HashMap<>();
    private Collection<GeoContact> contributors = new HashSet<>();
    private Collection<GeoDataset> dataSets = new HashSet<>();
    private boolean isSubSeries = false;
    private boolean isSuperSeries = false;
    private Collection<String> keyWords = new HashSet<>();
    private String lastUpdateDate = "";
    private String overallDesign = "";
    private Collection<String> pubmedIds = new HashSet<>();
    private Map<Integer, GeoReplication> replicates = new HashMap<>();
    private GeoSampleCorrespondence sampleCorrespondence;
    private List<String> summaries = new ArrayList<>();
    private final Collection<String> supplementaryFiles = new LinkedHashSet<>();
    private GeoValues values = new GeoValues();
    private Collection<String> webLinks = new HashSet<>();

    public boolean isSuperSeries() {
        return isSuperSeries;
    }

    public void setIsSuperSeries( boolean isSuperSeries ) {
        this.isSuperSeries = isSuperSeries;
    }

    public boolean isSubSeries() {
        return isSubSeries;
    }

    public void setIsSubSeries( boolean isSubSeries ) {
        this.isSubSeries = isSubSeries;
    }

    public void addContributor( GeoContact contributor ) {
        this.contributors.add( contributor );
    }

    public void addDataSet( GeoDataset dataset ) {
        dataset.addSeries( this );
        this.dataSets.add( dataset );
    }

    public void addSample( GeoSample sample ) {
        this.samples.add( sample );
    }

    /**
     * Add a group of samples to this series.
     *
     * @param s samples
     */
    public void addSamples( Collection<GeoSample> s ) {
        this.samples.addAll( s );
    }

    public void addSubSeries( String value ) {
        this.subSeries.add( value );
    }

    public void addToKeyWords( String keyword ) {
        this.keyWords.add( keyword );
    }

    /**
     * @param id in format "1239954" or "1239954,2194919", etc. The latter will be split into two.
     */
    public void addToPubmedIds( String id ) {
        String[] ids = id.split( "," );
        assert ids.length > 0;
        this.pubmedIds.addAll( Arrays.asList( ids ) );
        assert this.pubmedIds.size() > 0;
    }

    public void addToSeriesTypes( GeoSeriesType type ) {
        this.seriesTypes.add( type );
    }

    /**
     * Add a summary to the series.
     */
    public void addToSummaries( String text ) {
        this.summaries.add( text );
    }

    public void addToVariables( Integer number, GeoVariable variable ) {
        this.variables.put( number, variable );
    }

    public void addToSupplementaryFiles( String supplementaryFile ) {
        this.supplementaryFiles.add( supplementaryFile );
    }

    /**
     * Get a subset of the values. This is only used for 'splitting' a series.
     *
     * @param  s Samples to include data from.
     * @return geo values
     */
    public GeoValues getValues( Collection<GeoSample> s ) {
        return values.subset( s );
    }

    /**
     * Only keep the given samples.
     */
    public void keepSamples( Collection<GeoSample> samplesToKeep ) {
        if ( samplesToKeep == null || samplesToKeep.isEmpty() ) {
            return;
        }
        this.samples.removeIf( s -> !samplesToKeep.contains( s ) );
    }

    /**
     * Clean up samples we have decided are ineligible (i.e., non transcriptomic)
     *
     * @param samplesToSkip the samples to remove
     */
    public void removeSamples( Collection<GeoSample> samplesToSkip ) {
        if ( samplesToSkip == null || samplesToSkip.isEmpty() ) {
            return;
        }
        this.samples.removeAll( samplesToSkip );
    }
}