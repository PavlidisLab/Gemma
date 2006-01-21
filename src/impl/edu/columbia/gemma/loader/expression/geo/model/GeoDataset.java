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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * A GEO-curated dataset. In many cases this is associated with just one GeoSeries, but for studies that used more than
 * one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDataset extends GeoData {

    public ExperimentType experimentType;
    private String completeness;

    private String datasetType;

    private String description;;

    private String featureCount;

    private int numChannels;
    private int numSamples;
    private String order;
    private String organism;

    private GeoPlatform platform;
    private String probeType;
    private String pubmedId;
    private String sampleType;
    private Collection<GeoSeries> series;
    private Collection<GeoSubset> subsets;
    private String title;
    private String updateDate;
    private String valueType;

    public GeoDataset() {
        this.subsets = new HashSet<GeoSubset>();
        this.series = new HashSet<GeoSeries>();
    }

    /**
     * @param newSeries
     */
    public void addSeries( GeoSeries newSeries ) {
        assert this.series != null;
        this.series.add( newSeries );

    }

    public void addSubset( GeoSubset subset ) {
        this.subsets.add( subset );
    }

    /**
     * @return Returns the completeness.
     */
    public String getCompleteness() {
        return this.completeness;
    }

    /**
     * @return Returns the datasetType.
     */
    public String getDatasetType() {
        return this.datasetType;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return Returns the experimentType.
     */
    public GeoDataset.ExperimentType getExperimentType() {
        return this.experimentType;
    }

    /**
     * @return Returns the featureCount.
     */
    public String getFeatureCount() {
        return this.featureCount;
    }

    /**
     * @return Returns the numChannels.
     */
    public int getNumChannels() {
        return this.numChannels;
    }

    /**
     * @return Returns the numSamples.
     */
    public int getNumSamples() {
        return this.numSamples;
    }

    /**
     * @return Returns the order.
     */
    public String getOrder() {
        return this.order;
    }

    /**
     * @return Returns the organism.
     */
    public String getOrganism() {
        return this.organism;
    }

    /**
     * @return Returns the platform.
     */
    public GeoPlatform getPlatform() {
        return this.platform;
    }

    /**
     * @return Returns the probeType.
     */
    public String getProbeType() {
        return this.probeType;
    }

    /**
     * @return Returns the pubmedId.
     */
    public String getPubmedId() {
        return this.pubmedId;
    }

    /**
     * @return Returns the sampleType.
     */
    public String getSampleType() {
        return this.sampleType;
    }

    /**
     * @return Returns the series.
     */
    public Collection<GeoSeries> getSeries() {
        return this.series;
    }

    /**
     * @return Returns the subsets.
     */
    public Collection<GeoSubset> getSubsets() {
        return this.subsets;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @return Returns the updateDate.
     */
    public String getUpdateDate() {
        return this.updateDate;
    }

    /**
     * @return Returns the valueType.
     */
    public String getValueType() {
        return this.valueType;
    }

    /**
     * @param completeness The completeness to set.
     */
    public void setCompleteness( String completeness ) {
        this.completeness = completeness;
    }

    /**
     * @param datasetType The datasetType to set.
     */
    public void setDatasetType( String datasetType ) {
        this.datasetType = datasetType;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param experimentType The experimentType to set.
     */
    public void setExperimentType( GeoDataset.ExperimentType experimentType ) {
        this.experimentType = experimentType;
    }

    /**
     * @param featureCount The featureCount to set.
     */
    public void setFeatureCount( String featureCount ) {
        this.featureCount = featureCount;
    }

    /**
     * @param numChannels The numChannels to set.
     */
    public void setNumChannels( int numChannels ) {
        this.numChannels = numChannels;
    }

    /**
     * @param numSamples The numSamples to set.
     */
    public void setNumSamples( int numSamples ) {
        this.numSamples = numSamples;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder( String order ) {
        this.order = order;
    }

    /**
     * @param organism The organism to set.
     */
    public void setOrganism( String organism ) {
        this.organism = organism;
    }

    /**
     * @param platform The platform to set.
     */
    public void setPlatform( GeoPlatform platform ) {
        this.platform = platform;
    }

    /**
     * @param probeType The probeType to set.
     */
    public void setProbeType( String probeType ) {
        this.probeType = probeType;
    }

    /**
     * @param pubmedId The pubmedId to set.
     */
    public void setPubmedId( String pubmedId ) {
        this.pubmedId = pubmedId;
    }

    /**
     * @param sampleType The sampleType to set.
     */
    public void setSampleType( String sampleType ) {
        this.sampleType = sampleType;
    }

    /**
     * @param series The series to set.
     */
    public void setSeries( Collection<GeoSeries> series ) {
        this.series = series;
    }

    /**
     * @param subsets The subsets to set.
     */
    public void setSubsets( Collection<GeoSubset> subsets ) {
        this.subsets = subsets;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @param updateDate The updateDate to set.
     */
    public void setUpdateDate( String updateDate ) {
        this.updateDate = updateDate;
    }

    /**
     * @param valueType The valueType to set.
     */
    public void setValueType( String valueType ) {
        this.valueType = valueType;
    }

    public static ExperimentType convertStringToType( String string ) {
        if ( string.equals( "single channel" ) ) {
            return ExperimentType.singleChannel;
        } else if ( string.equals( "dual channel" ) ) {
            return ExperimentType.dualChannel;
        } else if ( string.equals( "single channel genomic" ) ) {
            return ExperimentType.singleChannelGenomic;
        } else if ( string.equals( "dual channel genomic" ) ) {
            return ExperimentType.dualChannelGenomic;
        } else if ( string.equals( "SAGE" ) ) {
            return ExperimentType.SAGE;
        } else if ( string.equals( "MPSS" ) ) {
            return ExperimentType.MPSS;
        } else {
            throw new IllegalArgumentException( "Unknown experiment type " + string );
        }
    }

    public enum ExperimentType {
        dualChannel, dualChannelGenomic, MPSS, SAGE, singleChannel, singleChannelGenomic
    }

}
