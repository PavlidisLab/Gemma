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
    private String phenotypeName;
    private String phenotypeCategory;
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
     * @param memberIds can be null; for a gene or experiment, this is a collection just containing their id
     */
    public static PhenotypeGroupValueObject convertFromGeneSetValueObject( GeneSetValueObject gsvo, String searchTerm ) {

        return new PhenotypeGroupValueObject( gsvo.getName(), gsvo.getDescription(), gsvo.getTaxonId(), gsvo.getTaxonName(),
                gsvo.getGeneIds(), gsvo.getName(), gsvo.getDescription(), searchTerm );
    }
    
    /**
     * Method to create a display object from scratch
     * 
     * @param name cannot be null
     * @param description should not be null
     * @param taxonId can be null
     * @param taxonName can be null
     * @param memberIds can be null; for a gene or experiment, this is a collection just containing their id
     */
    public PhenotypeGroupValueObject( String name, String description, Long taxonId, String taxonName,
            Collection<Long> memberIds, String phenotypeName, String phenotypeCategory, String searchTerm ) {

        this.setName( name );
        this.setDescription( description );
        this.setSize( memberIds.size() );
        this.setTaxonId( taxonId );
        this.setTaxonName( taxonName );
        this.setGeneIds( memberIds );
        this.setId( new Long( -1 ) );
        this.setModified( false );
        this.setPhenotypeName( phenotypeName );
        this.setPhenotypeCategory( phenotypeCategory );
        this.setSearchTerm( searchTerm );
    }

    /**
     * @param searchTerm the searchTerm to set
     */
    public void setSearchTerm( String searchTerm ) {
        this.searchTerm = searchTerm;
    }

    /**
     * @return the searchTerm
     */
    public String getSearchTerm() {
        return searchTerm;
    }

    public String getPhenotypeCategory() {
        return phenotypeCategory;
    }

    public void setPhenotypeCategory( String phenotypeCategory ) {
        this.phenotypeCategory = phenotypeCategory;
    }

    public String getPhenotypeName() {
        return phenotypeName;
    }

    public void setPhenotypeName( String phenotypeName ) {
        this.phenotypeName = phenotypeName;
    }


}
