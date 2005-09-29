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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * Undocumented type of GEO data. In many cases this is associated with just one GeoSeries, but for studies that used
 * more than one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDataset extends GeoData {

    private String completeness;
    private String description;

    public ExperimentType experimentType;

    public enum ExperimentType {
        singleChannel, dualChannel, singleChannelGenomic, dualChannelGenomic, SAGE, MPSS
    };

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

    private String order;
    private String organism;
    private GeoPlatform platform;
    private String probeType;

    private Collection<GeoSeries> series;
    private String title;
    private int numSamples;
    private String updateDate;
    private String valueType;
    private Collection<GeoSubset> subsets;

    public GeoDataset() {
        this.subsets = new HashSet<GeoSubset>();
        this.series = new HashSet<GeoSeries>();
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
     * @param completeness The completeness to set.
     */
    public void setCompleteness( String completeness ) {
        this.completeness = completeness;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return Returns the experimentType.
     */
    public GeoDataset.ExperimentType getExperimentType() {
        return this.experimentType;
    }

    /**
     * @param experimentType The experimentType to set.
     */
    public void setExperimentType( GeoDataset.ExperimentType experimentType ) {
        this.experimentType = experimentType;
    }

    /**
     * @return Returns the numSamples.
     */
    public int getNumSamples() {
        return this.numSamples;
    }

    /**
     * @param numSamples The numSamples to set.
     */
    public void setNumSamples( int numSamples ) {
        this.numSamples = numSamples;
    }

    /**
     * @return Returns the order.
     */
    public String getOrder() {
        return this.order;
    }

    /**
     * @param order The order to set.
     */
    public void setOrder( String order ) {
        this.order = order;
    }

    /**
     * @return Returns the organism.
     */
    public String getOrganism() {
        return this.organism;
    }

    /**
     * @param organism The organism to set.
     */
    public void setOrganism( String organism ) {
        this.organism = organism;
    }

    /**
     * @return Returns the platform.
     */
    public GeoPlatform getPlatform() {
        return this.platform;
    }

    /**
     * @param platform The platform to set.
     */
    public void setPlatform( GeoPlatform platform ) {
        this.platform = platform;
    }

    /**
     * @return Returns the probeType.
     */
    public String getProbeType() {
        return this.probeType;
    }

    /**
     * @param probeType The probeType to set.
     */
    public void setProbeType( String probeType ) {
        this.probeType = probeType;
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
     * @param subsets The subsets to set.
     */
    public void setSubsets( Collection<GeoSubset> subsets ) {
        this.subsets = subsets;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle( String title ) {
        this.title = title;
    }

    /**
     * @return Returns the updateDate.
     */
    public String getUpdateDate() {
        return this.updateDate;
    }

    /**
     * @param updateDate The updateDate to set.
     */
    public void setUpdateDate( String updateDate ) {
        this.updateDate = updateDate;
    }

    /**
     * @return Returns the valueType.
     */
    public String getValueType() {
        return this.valueType;
    }

    /**
     * @param valueType The valueType to set.
     */
    public void setValueType( String valueType ) {
        this.valueType = valueType;
    }

    /**
     * @param newSeries
     */
    public void addSeries( GeoSeries newSeries ) {
        assert this.series != null;
        this.series.add( newSeries );

    }

}
