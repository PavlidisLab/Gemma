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

import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.session.GemmaSessionBackedValueObject;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public class GOGroupValueObject extends SessionBoundGeneSetValueObject implements GemmaSessionBackedValueObject {

    private static final long serialVersionUID = -185326197992950287L;
    private String goId;
    private String searchTerm;
    
    public GOGroupValueObject(){
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
    public GOGroupValueObject( String name, String description, Long taxonId, String taxonName,
            Collection<Long> memberIds, String goId, String searchTerm ) {

        this.setName( name );
        this.setDescription( description );
        this.setSize( memberIds.size() );
        this.setTaxonId( taxonId );
        this.setTaxonName( taxonName );
        this.setGeneIds( memberIds );
        this.setId( new Long( -1 ) );
        this.setModified( false );
        this.setGoId( goId );
        this.setSearchTerm( searchTerm );
    }

    /**
     * Method to create a display object from scratch
     */
    public GOGroupValueObject( GeneSet gs, String goId, String searchTerm ) {

        GeneSetValueObject gsvo = new SessionBoundGeneSetValueObject( gs );

        this.setName( gsvo.getName() );
        this.setDescription( gsvo.getDescription() );
        this.setSize( gsvo.getSize() );
        this.setTaxonId( gsvo.getTaxonId() );
        this.setTaxonName( gsvo.getTaxonName() );
        this.setGeneIds( gsvo.getGeneIds() );
        this.setId( new Long( -1 ) );
        this.setModified( false );
        this.setGoId( goId );
        this.setSearchTerm( searchTerm );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObject#isSessionBound()
     */
    @Override
    public boolean isSessionBound() {
        return true;
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

    /**
     * @param goId the goId to set
     */
    public void setGoId( String goId ) {
        this.goId = goId;
    }

    /**
     * @return the goId
     */
    public String getGoId() {
        return goId;
    }

}
