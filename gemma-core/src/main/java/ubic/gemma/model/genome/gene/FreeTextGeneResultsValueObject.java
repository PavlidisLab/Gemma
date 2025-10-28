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
 * *
 *
 * @author tvrossum
 */
public class FreeTextGeneResultsValueObject extends SessionBoundGeneSetValueObject {

    private static final long serialVersionUID = 3557304710219740029L;
    private String queryString;

    /**
     * default constructor to satisfy java bean contract
     */
    public FreeTextGeneResultsValueObject() {
        super();
    }

    /**
     * Method to create a display object from scratch
     *
     * @param name        cannot be null
     * @param description should not be null
     * @param taxonId     can be null
     * @param taxonName   can be null
     * @param geneIds     can be null; for a gene or experiment, this is a collection just containing their id
     * @param queryString the query string
     */
    public FreeTextGeneResultsValueObject( String name, String description, Long taxonId, String taxonName,
            Collection<Long> geneIds, String queryString ) {

        this.setName( name );
        this.setDescription( description );

        this.setTaxon( new TaxonValueObject( taxonId, taxonName ) );

        this.setGeneIds( geneIds );
        this.setId( Long.valueOf( -1 ) );
        this.setModified( false );
        this.setQueryString( queryString );
    }

    /**
     * @return the queryString
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * @param queryString the queryString to set
     */
    public void setQueryString( String queryString ) {
        this.queryString = queryString;
    }

}
