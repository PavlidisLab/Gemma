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
package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * String argument type for taxon API, referencing the Taxon scientific name or common name. Can also be
 * null.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "A taxon identifier that matches either its scientific or common name.")
public class TaxonNameArg extends TaxonArg<String> {

    TaxonNameArg( String s ) {
        super( "commonName", String.class, s );
    }

    @Override
    Taxon getEntity( TaxonService service ) {
        return this.tryAllNameProperties( service );
    }

    /**
     * Tries to retrieve a Taxon based on its names.
     *
     * @param service the TaxonService that handles the search.
     * @return Taxon or null if no taxon with any property matching this#value was found.
     */
    private Taxon tryAllNameProperties( TaxonService service ) {
        // Most commonly used
        Taxon taxon = service.findByCommonName( this.getValue() );

        if ( taxon == null ) {
            taxon = service.findByScientificName( this.getValue() );
        }

        return taxon;
    }
}
