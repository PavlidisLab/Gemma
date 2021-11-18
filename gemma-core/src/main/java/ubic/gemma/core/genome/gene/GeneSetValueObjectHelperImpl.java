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

package ubic.gemma.core.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.util.EntityUtils;

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

        return this.geneSetService.loadValueObjectsLite( EntityUtils.getIds( Collections.singleton( gs ) ) ).iterator()
                .next();

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

        Collection<Long> ids = EntityUtils
                .getIds( this.geneSetService.getGenesInGroup( gs ) );
        dbgsvo.getGeneIds().addAll( ids );
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

        Collection<Long> genesetIds = EntityUtils.getIds( geneSets );

        List<DatabaseBackedGeneSetValueObject> results = new ArrayList<>();

        if ( light ) {
            results.addAll( geneSetService.loadValueObjectsLite( genesetIds ) );
        } else {
            results.addAll( geneSetService.loadValueObjects( genesetIds ) );
        }

        if ( !includeOnesWithoutGenes ) {
            for ( Iterator<DatabaseBackedGeneSetValueObject> it = results.iterator(); it.hasNext(); ) {
                if ( it.next().getSize() == 0 ) {
                    it.remove();
                }
            }
        }

        Collections.sort( results, new Comparator<GeneSetValueObject>() {
            @Override
            public int compare( GeneSetValueObject o1, GeneSetValueObject o2 ) {
                return -o1.getSize().compareTo( o2.getSize() );
            }
        } );

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
            sbgsvo.setSize( this.geneSetService.getSize( gs ) );
        }

        Set<Long> gids = new HashSet<>();
        for ( GeneSetMember gm : gs.getMembers() ) {
            gids.add( gm.getGene().getId() );
        }
        sbgsvo.setGeneIds( gids );

        Taxon tax = this.geneSetService.getTaxon( gs );
        if ( tax != null ) {

            sbgsvo.setTaxonId( tax.getId() );
            sbgsvo.setTaxonName( tax.getCommonName() );
        }

        sbgsvo.setId( new Long( -1 ) );
        sbgsvo.setModified( false );
    }

}
