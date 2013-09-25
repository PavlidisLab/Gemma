/*
 * The gemma-model project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class PhenotypeValueObject {

    private String value = "";
    private String valueUri = "";

    public PhenotypeValueObject( String value, String valueUri ) {
        super();
        this.value = value;
        this.valueUri = valueUri;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        PhenotypeValueObject other = ( PhenotypeValueObject ) obj;
        if ( value == null ) {
            if ( other.value != null ) return false;
        } else if ( !value.equals( other.value ) ) return false;
        if ( valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !valueUri.equals( other.valueUri ) ) return false;
        return true;
    }

    public String getValue() {
        return this.value;
    }

    public String getValueUri() {
        return this.valueUri;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
        result = prime * result + ( ( valueUri == null ) ? 0 : valueUri.hashCode() );
        return result;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

    @Override
    public String toString() {
        return "PhenotypeVO [value=" + value + " : " + valueUri + "]";
    }

}
