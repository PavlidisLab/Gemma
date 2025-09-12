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

package ubic.gemma.model.genome.gene;

import ubic.gemma.model.genome.TaxonValueObject;

import java.util.Collection;

/**
 * @author tvrossum
 */
public class GOGroupValueObject extends SessionBoundGeneSetValueObject {

    private static final long serialVersionUID = -185326197992950287L;
    private String goId;
    private String searchTerm;

    public GOGroupValueObject() {
        super();
    }

    /**
     * Method to create a display object from scratch
     *
     * @param name        cannot be null
     * @param description should not be null
     * @param taxonId     can be null
     * @param taxonName   can be null
     * @param memberIds   can be null; for a gene or experiment, this is a collection just containing their id
     * @param searchTerm  search term
     * @param goId        go ID
     */
    public GOGroupValueObject( String name, String description, Long taxonId, String taxonName,
            Collection<Long> memberIds, String goId, String searchTerm ) {

        this.setName( name );
        this.setDescription( description );
        this.setSize( memberIds.size() );
        this.setTaxon( new TaxonValueObject( taxonId, taxonName ) );
        this.setGeneIds( memberIds );
        this.setId( Long.valueOf( -1 ) );
        this.setModified( false );
        this.setGoId( goId );
        this.setSearchTerm( searchTerm );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( goId == null ) ? 0 : goId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( !super.equals( obj ) ) {
            return false;
        }
        if ( this.getClass() != obj.getClass() ) {
            return false;
        }
        GOGroupValueObject other = ( GOGroupValueObject ) obj;
        if ( goId == null ) {
            return other.goId == null;
        }
        return goId.equals( other.goId );
    }

    /**
     * @return the goId
     */
    public String getGoId() {
        return goId;
    }

    /**
     * @param goId the goId to set
     */
    public void setGoId( String goId ) {
        this.goId = goId;
    }

    /**
     * @return the searchTerm
     */
    public String getSearchTerm() {
        return searchTerm;
    }

    /**
     * @param searchTerm the searchTerm to set
     */
    public void setSearchTerm( String searchTerm ) {
        this.searchTerm = searchTerm;
    }

}
