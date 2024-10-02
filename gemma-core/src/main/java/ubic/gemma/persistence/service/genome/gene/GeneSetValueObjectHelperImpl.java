/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.persistence.service.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

/**
 * This class will handle population of GeneSetValueObjects. Services need to be accessed in order to define values for
 * size, geneIds, and public/private fields.
 *
 * @author tvrossum
 */
@Component
public class GeneSetValueObjectHelperImpl implements GeneSetValueObjectHelper {

    @Autowired
    private GeneSetService geneSetService;

    @Override
    public GOGroupValueObject convertToGOValueObject( GeneSet gs, String goId, String searchTerm ) {

        GOGroupValueObject ggvo = new GOGroupValueObject();
        fillSessionBoundValueObject( ggvo, gs );

        ggvo.setGoId( goId );
        ggvo.setSearchTerm( searchTerm );

        return ggvo;
    }

    @Override
    public DatabaseBackedGeneSetValueObject convertToLightValueObject( GeneSet gs ) {
        if ( gs == null ) {
            return null;
        }

        return this.geneSetService.loadValueObjectByIdLite( gs.getId() );
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> geneSets,
            boolean includeOnesWithoutGenes ) {
        return convertToLightValueObjects( geneSets, includeOnesWithoutGenes, true );
    }

    @Override
    public DatabaseBackedGeneSetValueObject convertToValueObject( GeneSet gs ) {
        if ( gs == null )
            return null;

        DatabaseBackedGeneSetValueObject dbgsvo = convertToLightValueObject( gs );

        Collection<Long> ids = IdentifiableUtils
                .getIds( this.geneSetService.getGenesInGroup( new GeneSetValueObject( gs.getId() ) ) );
        dbgsvo.setGeneIds( ids );
        dbgsvo.setSize( ids.size() );

        return dbgsvo;
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> sets ) {
        return convertToValueObjects( sets, false );
    }

    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {

        return convertToLightValueObjects( genesets, includeOnesWithoutGenes, false );
    }

    /**
     * @param  geneSets                gene sets
     * @param  includeOnesWithoutGenes should empty sets get removed?
     * @param  light                   Don't fill in the gene ids. Should be faster
     * @return list of gene set value objects
     */
    private List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> geneSets,
            boolean includeOnesWithoutGenes, boolean light ) {

        Collection<Long> genesetIds = IdentifiableUtils.getIds( geneSets );

        List<DatabaseBackedGeneSetValueObject> results = new ArrayList<>();

        if ( light ) {
            results.addAll( geneSetService.loadValueObjectsByIdsLite( genesetIds ) );
        } else {
            results.addAll( geneSetService.loadValueObjectsByIds( genesetIds ) );
        }

        if ( !includeOnesWithoutGenes ) {
            results.removeIf( databaseBackedGeneSetValueObject -> databaseBackedGeneSetValueObject.getSize() == 0 );
        }

        results.sort( Comparator.comparingLong( GeneSetValueObject::getSize ) );

        return results;
    }

    private void fillSessionBoundValueObject( SessionBoundGeneSetValueObject sbgsvo, GeneSet gs ) {

        sbgsvo.setName( gs.getName() );
        sbgsvo.setDescription( gs.getDescription() );
        // GO group gene sets don't have ids
        if ( gs.getId() == null ) {
            sbgsvo.setSize( gs.getMembers() != null ? gs.getMembers().size() : 0 );
        } else {// this case may never happen as this is only called from convertToGoValueObject() leaving here in case
            // this method is ever called from somewhere else
            sbgsvo.setSize( this.geneSetService.getSize( new GeneSetValueObject( gs.getId() ) ) );
        }

        Collection<Long> gids = new HashSet<>();
        for ( GeneSetMember gm : gs.getMembers() ) {
            gids.add( gm.getGene().getId() );
        }
        sbgsvo.setGeneIds( gids );

        gs.getMembers().stream().findAny()
                .map( GeneSetMember::getGene )
                .map( Gene::getTaxon )
                .ifPresent( tax -> sbgsvo.setTaxon( new TaxonValueObject( tax ) ) );

        sbgsvo.setId( Long.valueOf( -1 ) );
        sbgsvo.setModified( false );
    }

}
