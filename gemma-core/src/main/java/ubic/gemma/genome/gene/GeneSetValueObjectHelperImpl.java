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

package ubic.gemma.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetDao;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.security.SecurityService;

/**
 * This class will handle population of GeneSetValueObjects. Services need to be accessed in order to define values for
 * size, geneIds, and public/private fields.
 * 
 * @author tvrossum
 * @version $Id$
 */
@Component
public class GeneSetValueObjectHelperImpl implements GeneSetValueObjectHelper {

    @Autowired
    private GeneSetDao geneSetDao;

    @Autowired
    private SecurityService securityService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToValueObject(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public DatabaseBackedGeneSetValueObject convertToValueObject( GeneSet gs ) {

        DatabaseBackedGeneSetValueObject dbgsvo = convertToLightValueObject( gs );
        if ( dbgsvo != null ) {
            // no duplicates
            Set<Long> ids = new HashSet<Long>( this.geneSetDao.getGeneIds( gs.getId() ) );
            dbgsvo.setGeneIds( ids );
            dbgsvo.setSize( ids.size() );
        }

        return dbgsvo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToLightValueObject(ubic.gemma.model.genome.gene.GeneSet)
     */
    @Override
    public DatabaseBackedGeneSetValueObject convertToLightValueObject( GeneSet gs ) {
        if ( gs == null ) {
            return null;
        }

        DatabaseBackedGeneSetValueObject dbgsvo = new DatabaseBackedGeneSetValueObject();

        dbgsvo.setName( gs.getName() );

        dbgsvo.setId( gs.getId() );

        dbgsvo.setGeneIds( null );

        dbgsvo.setDescription( gs.getDescription() );
        dbgsvo.setSize( this.geneSetDao.getGeneCount( gs.getId() ) );

        Taxon tax = this.geneSetDao.getTaxon( gs.getId() );
        if ( tax != null ) {
            while ( tax.getParentTaxon() != null ) {
                tax = tax.getParentTaxon();
            }
            dbgsvo.setTaxonId( tax.getId() );
            dbgsvo.setTaxonName( tax.getCommonName() );
        } else {
            dbgsvo.setTaxonId( null );
            dbgsvo.setTaxonName( null );
        }

        dbgsvo.setUserCanWrite( this.securityService.isEditable( gs ) );
        dbgsvo.setUserOwned( this.securityService.isOwnedByCurrentUser( gs ) );
        dbgsvo.setIsPublic( this.securityService.isPublic( gs ) );
        dbgsvo.setIsShared( this.securityService.isShared( gs ) );

        return dbgsvo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToValueObjects(java.util.Collection)
     */
    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> sets ) {
        return convertToValueObjects( sets, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToValueObjects(java.util.Collection, boolean)
     */
    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {

        return convertToLightValueObjects( genesets, includeOnesWithoutGenes, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToLightValueObjects(java.util.Collection, boolean)
     */
    @Override
    public List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {

        return convertToLightValueObjects( genesets, includeOnesWithoutGenes, true );
    }

    private List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes, boolean light ) {

        List<DatabaseBackedGeneSetValueObject> results = new ArrayList<DatabaseBackedGeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            if ( !includeOnesWithoutGenes && this.geneSetDao.getGeneCount( gs.getId() ) <= 0 ) {
                continue;
            }

            if ( light ) {
                results.add( convertToLightValueObject( gs ) );
            } else {
                results.add( convertToValueObject( gs ) );
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
            sbgsvo.setSize( this.geneSetDao.getGeneCount( gs.getId() ) );
        }

        Collection<Long> gids = new HashSet<Long>();
        for ( GeneSetMember gm : gs.getMembers() ) {
            gids.add( gm.getGene().getId() );
        }
        sbgsvo.setGeneIds( gids );

        Taxon tax = this.geneSetDao.getTaxon( gs.getId() );
        if ( tax != null ) {
            while ( tax.getParentTaxon() != null ) {
                tax = tax.getParentTaxon();
            }
            sbgsvo.setTaxonId( tax.getId() );
            sbgsvo.setTaxonName( tax.getCommonName() );
        }

        sbgsvo.setId( new Long( -1 ) );
        sbgsvo.setModified( false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.genome.gene.GeneSetValueObjectHelper#convertToGOValueObject(ubic.gemma.model.genome.gene.GeneSet,
     * java.lang.String, java.lang.String)
     */
    @Override
    public GOGroupValueObject convertToGOValueObject( GeneSet gs, String goId, String searchTerm ) {

        GOGroupValueObject ggvo = new GOGroupValueObject();
        fillSessionBoundValueObject( ggvo, gs );

        ggvo.setGoId( goId );
        ggvo.setSearchTerm( searchTerm );

        return ggvo;
    }

}
