/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.genome.gene;

import java.util.Collection;

import ubic.gemma.model.genome.gene.GeneSetValueObject;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class PhenotypeGroupValueObject extends SessionBoundGeneSetValueObject {

    private static final long serialVersionUID = -7264201170714207356L;

    /**
     * Method to create a display object from scratch
     * 
     * @param name cannot be null
     * @param description should not be null
     * @param taxonId can be null
     * @param taxonName can be null
     * @param memberIds can be null; for a gene this is a collection just containing their id
     */
    public static PhenotypeGroupValueObject convertFromGeneSetValueObject( GeneSetValueObject gsvo, String searchTerm ) {
        return new PhenotypeGroupValueObject( gsvo.getName(), gsvo.getDescription(), gsvo.getTaxonId(),
                gsvo.getTaxonName(), gsvo.getGeneIds(), gsvo.getName(), gsvo.getDescription(), searchTerm );
    }

    private String phenotypeCategory;
    private String phenotypeName;

    private String searchTerm;

    public PhenotypeGroupValueObject() {
        super();
    }

    /**
     * Method to create a display object from scratch
     * 
     * @param name cannot be null
     * @param description should not be null
     * @param taxonId can be null
     * @param taxonName can be null
     * @param memberIds can be null; for a gene this is a collection just containing their id
     */
    public PhenotypeGroupValueObject( String name, String description, Long taxonId, String taxonName,
            Collection<Long> memberIds, String phenotypeName, String phenotypeCategory, String searchTerm ) {

        this.setName( name );
        this.setDescription( description );
        // this.setSize( memberIds.size() );
        this.setTaxonId( taxonId );
        this.setTaxonName( taxonName );
        this.setGeneIds( memberIds );
        this.setId( new Long( -1 ) );
        this.setModified( false );
        this.setPhenotypeName( phenotypeName );
        this.setPhenotypeCategory( phenotypeCategory );
        this.setSearchTerm( searchTerm );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !super.equals( obj ) ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        PhenotypeGroupValueObject other = ( PhenotypeGroupValueObject ) obj;
        if ( phenotypeCategory == null ) {
            if ( other.phenotypeCategory != null ) {
                return false;
            }
        } else if ( !phenotypeCategory.equals( other.phenotypeCategory ) ) {
            return false;
        }
        if ( phenotypeName == null ) {
            if ( other.phenotypeName != null ) {
                return false;
            }
        } else if ( !phenotypeName.equals( other.phenotypeName ) ) {
            return false;
        }
        return true;
    }

    public String getPhenotypeCategory() {
        return phenotypeCategory;
    }

    public String getPhenotypeName() {
        return phenotypeName;
    }

    /**
     * @return the searchTerm
     */
    public String getSearchTerm() {
        return searchTerm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( phenotypeCategory == null ) ? 0 : phenotypeCategory.hashCode() );
        result = prime * result + ( ( phenotypeName == null ) ? 0 : phenotypeName.hashCode() );
        return result;
    }

    public void setPhenotypeCategory( String phenotypeCategory ) {
        this.phenotypeCategory = phenotypeCategory;
    }

    public void setPhenotypeName( String phenotypeName ) {
        this.phenotypeName = phenotypeName;
    }

    /**
     * @param searchTerm the searchTerm to set
     */
    public void setSearchTerm( String searchTerm ) {
        this.searchTerm = searchTerm;
    }

}
