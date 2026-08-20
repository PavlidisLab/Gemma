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
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
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
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "no constructor or setter call ever populates it")
    private Boolean isSpecies;
    /**
     * Whether Gemma has gene records loaded for this taxon — a loader-maintained capability bit,
     * written {@code false} at creation and flipped by {@code NcbiGeneLoader.updateTaxaWithGenesUsable}
     * once genes actually land. Read internally by
     * {@code TaxonReadServiceImpl.loadAllTaxaWithGenes()}, whose one consumer,
     * {@code GeneSearchServiceImpl.getGOGroupGenes}, uses it to bound an untargeted GO search's
     * fan-out.
     * <p>
     * Withheld because production only serves taxa that have genes loaded, so on the wire this would
     * be a constant {@code true}: it invites clients to branch on a condition that never occurs.
     * Note this is a claim about the data rather than the structure — {@code GET /taxa} serves every
     * taxon unfiltered, and {@code GeoConverterImpl} still writes {@code false} for taxa imported
     * from GEO, so a genes-less taxon reaching a client is possible in principle. If that becomes
     * routine, the field is informative again and this reason no longer holds.
     * <p>
     * Distinct from {@link #isSpecies}, which is withheld because nothing populates it at all.
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "production serves only genes-usable taxa, so this would publish a constant true and invite clients to branch on a condition that never occurs; unlike isSpecies it IS populated, so recheck if genes-less taxa start being served")
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
