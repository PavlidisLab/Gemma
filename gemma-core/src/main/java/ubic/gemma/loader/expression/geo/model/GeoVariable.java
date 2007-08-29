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
package ubic.gemma.loader.expression.geo.model;

import java.util.Collection;

/**
 * A GeoVariable represents variables which were investigated.
 * <p>
 * According to the GEO web site, the valid values for the "name" are: dose, time, tissue, strain, gender, cell line,
 * development stage, age, agent, cell type, infection, isolate, metabolism, shock, stress, temperature, specimen,
 * disease state, protocol, growth protocol, genotype/genetic variation, species, individual, or other.
 * </p>
 * <p>
 * These map to ExperimentalFactors in Gemma.
 * </p>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoVariable {

    @Override
    public String toString() {
        return this.getType().toString() + this.getDescription();
    }

    VariableType type;
    String description = "";

    /**
     * The samples to which this variable applies.
     */
    Collection<GeoSample> samples;

    /**
     * Permitted descriptions of terms. These will correspond to OntologyEntries in Gemma. Also known as subset type.
     */
    public enum VariableType {
        age, agent, cellLine, cellType, developmentStage, diseaseState, dose, gender, genotypeOrVariation, growthProtocol, individual, infection, isolate, metabolism, other, protocol, shock, species, specimen, stress, strain, temperature, time, tissue
    };

    /**
     * @param d
     */
    public void addToDescription( String d ) {
        this.description += d;
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

    public void addToVariableSampleList( GeoSample sample ) {
        this.samples.add( sample );
    }

    /**
     * @return Returns the variableSampleList.
     */
    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    /**
     * @param variableSampleList The variableSampleList to set.
     */
    public void setSamples( Collection<GeoSample> variableSampleList ) {
        this.samples = variableSampleList;
    }

    /**
     * @return Returns the name.
     */
    public VariableType getType() {
        return this.type;
    }

    /**
     * @param name The name to set.
     */
    public void setType( VariableType name ) {
        this.type = name;
    }

    /**
     * Convert a string e.g., "age" to the corresponding enumerated type.
     * 
     * @param string
     * @return
     */
    public static VariableType convertStringToType( String string ) {
        if ( string.toLowerCase().equals( "age" ) ) {
            return VariableType.age;
        } else if ( string.toLowerCase().equals( "agent" ) ) {
            return VariableType.agent;
        } else if ( string.equals( "cell line" ) ) {
            return VariableType.cellLine;
        } else if ( string.toLowerCase().equals( "cell type" ) ) {
            return VariableType.cellType;
        } else if ( string.toLowerCase().equals( "development stage" ) ) {
            return VariableType.developmentStage;
        } else if ( string.toLowerCase().equals( "disease state" ) ) {
            return VariableType.diseaseState;
        } else if ( string.toLowerCase().equals( "dose" ) ) {
            return VariableType.dose;
        } else if ( string.toLowerCase().equals( "gender" ) ) {
            return VariableType.gender;
        } else if ( string.toLowerCase().equals( "genotype/variation" ) ) {
            return VariableType.genotypeOrVariation;
        } else if ( string.toLowerCase().equals( "growth protocol" ) ) {
            return VariableType.growthProtocol;
        } else if ( string.toLowerCase().equals( "individual" ) ) {
            return VariableType.individual;
        } else if ( string.toLowerCase().equals( "infection" ) ) {
            return VariableType.infection;
        } else if ( string.toLowerCase().equals( "isolate" ) ) {
            return VariableType.isolate;
        } else if ( string.toLowerCase().equals( "metabolism" ) ) {
            return VariableType.metabolism;
        } else if ( string.toLowerCase().equals( "other" ) ) {
            return VariableType.other;
        } else if ( string.toLowerCase().equals( "protocol" ) ) {
            return VariableType.protocol;
        } else if ( string.toLowerCase().equals( "shock" ) ) {
            return VariableType.shock;
        } else if ( string.toLowerCase().equals( "species" ) ) {
            return VariableType.species;
        } else if ( string.toLowerCase().equals( "specimen" ) ) {
            return VariableType.specimen;
        } else if ( string.toLowerCase().equals( "stress" ) ) {
            return VariableType.stress;
        } else if ( string.toLowerCase().equals( "strain" ) ) {
            return VariableType.strain;
        } else if ( string.toLowerCase().equals( "temperature" ) ) {
            return VariableType.temperature;
        } else if ( string.toLowerCase().equals( "time" ) ) {
            return VariableType.time;
        } else if ( string.toLowerCase().equals( "tissue" ) ) {
            return VariableType.tissue;
        } else {
            throw new IllegalArgumentException( "Unknown variable type " + string );
        }

    }

}