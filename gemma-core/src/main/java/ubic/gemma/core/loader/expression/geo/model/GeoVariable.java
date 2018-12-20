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
 */
@SuppressWarnings("unused") // Possible external use
public class GeoVariable {

    private VariableType type;
    private String description = "";
    /**
     * The samples to which this variable applies.
     */
    private Collection<GeoSample> samples;

    /**
     * Convert a string found in the GEO sample descriptions e.g., "age" to the corresponding category, represented here
     * by VariableType. TODO: this list has gotten unwieldy and should be replaced with a config file that is read in,
     * and it should be more directly related the list in EFO.factor.categories.txt
     *
     * @param  string string
     * @return        variable type
     */
    public static VariableType convertStringToType( String string ) {
        String lcstring = string.toLowerCase();
        if ( lcstring.equals( "age" ) ) {
            return VariableType.age;
        } else if ( lcstring.equals( "agent" ) ) {
            return VariableType.agent;
        } else if ( string.equals( "cell line" ) ) {
            return VariableType.cellLine;
        } else if ( lcstring.equals( "cell type" ) ) {
            return VariableType.cellType;
        } else if ( lcstring.equals( "development stage" ) || lcstring.equals( "developmental stage" ) ) {
            return VariableType.developmentStage;
        } else if ( lcstring.equals( "disease state" ) ) {
            return VariableType.diseaseState;
        } else if ( lcstring.equals( "dose" ) ) {
            return VariableType.dose;
        } else if ( lcstring.equals( "gender" ) ) {
            return VariableType.gender;
        } else if ( lcstring.equals( "sex" ) || lcstring.equals( "sex (gender)" ) ) {
            return VariableType.gender;
        } else if ( lcstring.equals( "genotype/variation" ) ) {
            return VariableType.genotypeOrVariation;
        } else if ( lcstring.equals( "growth protocol" ) ) {
            return VariableType.growthProtocol;
        } else if ( lcstring.equals( "individual" ) ) {
            return VariableType.individual;
        } else if ( lcstring.equals( "infection" ) ) {
            return VariableType.infection;
        } else if ( lcstring.equals( "isolate" ) ) {
            return VariableType.isolate;
        } else if ( lcstring.equals( "metabolism" ) ) {
            return VariableType.metabolism;
        } else if ( lcstring.equals( "other" ) ) {
            return VariableType.other;
        } else if ( lcstring.equals( "phenotype" ) ) {
            return VariableType.phenotype;
        } else if ( lcstring.equals( "protocol" ) ) {
            return VariableType.protocol;
        } else if ( lcstring.equals( "shock" ) ) {
            return VariableType.shock;
        } else if ( lcstring.equals( "species" ) ) {
            return VariableType.species;
        } else if ( lcstring.equals( "specimen" ) ) {
            return VariableType.specimen;
        } else if ( lcstring.equals( "stress" ) ) {
            return VariableType.stress;
        } else if ( lcstring.equals( "strain" ) || lcstring.equals( "strain/background" )
                || lcstring.equals( "genetic background" ) ) {
            return VariableType.strain;
        } else if ( lcstring.equals( "temperature" ) ) {
            return VariableType.temperature;
        } else if ( lcstring.equals( "time" ) ) {
            return VariableType.time;
        } else if ( lcstring.equals( "tissue" ) || lcstring.equals( "tissue source" )
                || lcstring.equals( "tissue of origin" ) || lcstring.equals( "tissue type" )
                || lcstring.equals( "organ" )
                || lcstring.equals( "brain region" ) || lcstring.equals( "tumor location" ) ) {
            return VariableType.organismPart;
        } else if ( lcstring.equals( "genotype" ) ) {
            return VariableType.genotypeOrVariation;
        } else if ( lcstring.equals( "race" ) || lcstring.equals( "ancestry" ) ) {
            return VariableType.population;
        } else if ( lcstring.equals( "smoking status" ) ) {
            return VariableType.environmentalHistory;
        } else if ( lcstring.equals( "treatment" ) || lcstring.equals( "treatment arm" )
                || lcstring.equals( "treated with" ) ) {
            return VariableType.treatment;
        } else {
            return VariableType.other;
        }

    }

    public void addToVariableSampleList( GeoSample sample ) {
        this.samples.add( sample );
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( description == null ) ? 0 : description.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        final GeoVariable other = ( GeoVariable ) obj;
        if ( description == null ) {
            if ( other.description != null )
                return false;
        } else if ( !description.equals( other.description ) )
            return false;
        if ( type == null ) {
            return other.type == null;
        }
        return type.equals( other.type );
    }

    @Override
    public String toString() {
        return this.getType().toString() + this.getDescription();
    }

    /**
     * Permitted descriptions of terms. These will correspond to Characteristic categories in Gemma. Also known as
     * subset type.
     */
    public enum VariableType {
        age, agent, cellLine, cellType, developmentStage, treatment, diseaseState, dose, gender, genotypeOrVariation, growthProtocol, individual, infection, isolate, metabolism, other, protocol, shock, species, specimen, stress, strain, temperature, time, organismPart, population, environmentalHistory, phenotype
    }

}