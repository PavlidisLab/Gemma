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

import ubic.gemma.core.loader.expression.geo.model.GeoVariable.VariableType;
import ubic.gemma.core.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents a subset of samples.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class GeoSubset extends GeoData {

    private String dataSet;
    private GeoDataset owningDataset;
    private String description = "";
    private final Collection<GeoSample> samples = new HashSet<>();

    private VariableType type;

    public void addToDescription( String s ) {
        this.description = StringUtils.appendWithDelimiter( this.description, s );
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
     * @return Returns the sample.
     */
    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    public void addSample( GeoSample sample ) {
        this.samples.add( sample );
    }

    /**
     * @return Returns the type.
     */
    public VariableType getType() {
        return this.type;
    }

    /**
     * @param type The type to set.
     */
    public void setType( VariableType type ) {
        this.type = type;
    }

    /**
     * @return Returns the owningDataset.
     */
    public GeoDataset getOwningDataset() {
        return this.owningDataset;
    }

    /**
     * @param owningDataset The owningDataset to set.
     */
    public void setOwningDataset( GeoDataset owningDataset ) {
        this.owningDataset = owningDataset;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet( String dataSet ) {
        this.dataSet = dataSet;
    }
}
