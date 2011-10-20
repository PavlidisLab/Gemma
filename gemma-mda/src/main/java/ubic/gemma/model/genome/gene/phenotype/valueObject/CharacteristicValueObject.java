/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
 * CharacteristicValueObject containing a category to a value
 * 
 * @author ??
 * @version $Id$
 */
public class CharacteristicValueObject implements Comparable<CharacteristicValueObject> {

    private String category = "";
    private String categoryUri = "";

    private String value = "";
    private String valueUri = "";

    private long occurence = 0;

    private String ontologyUsed = null;

    private Boolean alreadyPresentOnGene = false;

    public CharacteristicValueObject( String value, String category ) {
        this.category = category;
        this.value = value;
    }

    public CharacteristicValueObject( String value, String category, String valueUri, String categoryUri ) {
        super();
        this.category = category;
        this.categoryUri = categoryUri;
        this.value = value;
        this.valueUri = valueUri;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public String getValueUri() {
        return valueUri;
    }

    public void Uri( String valueUri ) {
        this.valueUri = valueUri;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value.toLowerCase();
    }

    public long getOccurence() {
        return occurence;
    }

    public void setOccurence( long occurence ) {
        this.occurence = occurence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( category == null ) ? 0 : category.hashCode() );
        result = prime * result + ( ( categoryUri == null ) ? 0 : categoryUri.hashCode() );
        result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
        result = prime * result + ( ( valueUri == null ) ? 0 : valueUri.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CharacteristicValueObject other = ( CharacteristicValueObject ) obj;
        if ( valueUri == null ) {
            if ( other.valueUri != null ) return false;
        } else if ( !valueUri.equals( other.valueUri ) ) return false;
        return true;
    }

    @Override
    public int compareTo( CharacteristicValueObject o ) {

        if ( this.category.equalsIgnoreCase( o.category ) ) {
            return this.value.compareToIgnoreCase( o.value );
        }
        return this.category.compareTo( o.category );

    }

    public String getOntologyUsed() {
        return ontologyUsed;
    }

    public void setOntologyUsed( String ontologyUsed ) {
        this.ontologyUsed = ontologyUsed;
    }

    public Boolean getAlreadyPresentOnGene() {
        return alreadyPresentOnGene;
    }

    public void setAlreadyPresentOnGene( Boolean alreadyPresentOnGene ) {
        this.alreadyPresentOnGene = alreadyPresentOnGene;
    }

}
