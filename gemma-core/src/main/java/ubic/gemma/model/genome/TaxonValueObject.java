/*
 * The gemma project
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
package ubic.gemma.model.genome;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

/**
 * @author Paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Used in frontend
@Getter
@Setter
public class TaxonValueObject extends IdentifiableValueObject<Taxon> {

    private String scientificName;
    private String commonName;
    private Integer ncbiId;
    @GemmaWebOnly
    private Boolean isSpecies;
    @GemmaWebOnly
    private Boolean isGenesUsable;
    private ExternalDatabaseValueObject externalDatabase;

    public TaxonValueObject() {
        super();
    }

    public TaxonValueObject( Taxon taxon ) {
        super( taxon );
        this.setScientificName( taxon.getScientificName() );
        this.setCommonName( taxon.getCommonName() );

        this.setNcbiId( taxon.getNcbiId() );
        this.setIsGenesUsable( taxon.getIsGenesUsable() );

        if ( taxon.getExternalDatabase() != null ) {
            this.setExternalDatabase( new ExternalDatabaseValueObject( taxon.getExternalDatabase() ) );
        }
    }

    public TaxonValueObject( Long id ) {
        super( id );
    }

    public TaxonValueObject( Long id, String commonName ) {
        super( id );
        this.commonName = commonName;
    }

    public static TaxonValueObject fromEntity( Taxon taxon ) {
        return new TaxonValueObject( taxon );
    }

}
