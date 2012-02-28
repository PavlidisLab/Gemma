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
import ubic.gemma.security.SecurityService;


/**
 * This class will handle population of GeneSetValueObjects. Services need to be accessed in order
 * to define values for size, geneIds, and public/private fields.
 * 
 * 
 * @author tvrossum
 * @version $Id$
 */
@Component
public class GeneSetValueObjectHelper {

    @Autowired
    private GeneSetDao geneSetDao;
    
    @Autowired
    private SecurityService securityService;
    
    /**
     * Constructor to build value object from GeneSet.
     * 
     * No security filtering is done here, assuming that if the user could load the experimentSet entity, they have
     * access to it.
     * 
     * @param gs an expressionExperimentSet entity to create a value object for
     */
    public DatabaseBackedGeneSetValueObject convertToValueObject( GeneSet gs ) {        

        DatabaseBackedGeneSetValueObject dbgsvo = convertToLightValueObject( gs );
        if ( dbgsvo != null ) {
            // no duplicates
            Set<Long> ids = new HashSet<Long>(geneSetDao.getGeneIds( gs.getId() ));
            dbgsvo.setGeneIds( ids );
            dbgsvo.setSize( ids.size() );
        }
        
        return dbgsvo;
    }
    
    /**
     * Constructor to build value object from GeneSet. This is a light version and *does not include member ids*! (But the size is set.)
     * 
     * No security filtering is done here, assuming that if the user could load the experimentSet entity, they have
     * access to it.
     * 
     * @param gs an expressionExperimentSet entity to create a value object for
     * @return a gene set value object with all fields filled except for gene members
     */
    public DatabaseBackedGeneSetValueObject convertToLightValueObject( GeneSet gs ) {        
        if ( gs == null ) {
            return null;
        }
        
        DatabaseBackedGeneSetValueObject dbgsvo = new DatabaseBackedGeneSetValueObject();
        
        dbgsvo.setName( gs.getName() );

        dbgsvo.setId( gs.getId() );

        
        dbgsvo.setDescription( gs.getDescription() );
        dbgsvo.setSize( geneSetDao.getGeneCount( gs.getId() ) );
        
        Taxon tax = geneSetDao.getTaxon( gs.getId() );
        if( tax != null){
            while(tax.getParentTaxon() != null){
                tax = tax.getParentTaxon();
            }  
            dbgsvo.setTaxonId( tax.getId() );
            dbgsvo.setTaxonName( tax.getCommonName() );
        }
 
        dbgsvo.setCurrentUserHasWritePermission( securityService.isEditable( gs ) );
        dbgsvo.setCurrentUserIsOwner( securityService.isOwnedByCurrentUser( gs ) );
        dbgsvo.setPublik( securityService.isPublic( gs ) );
        dbgsvo.setShared( securityService.isShared( gs ) );
        
        return dbgsvo;
    }
    
    
    /**
     * results will be sorted by size
     * gene sets that lack genes will be excluded 
     * @see ubic.gemma.genome.gene.service.GeneSetServiceImpl.convertToValueObjects(Collection<GeneSet>, boolean) if you want empty sets returned
     * @param sets
     * @return
     */
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> sets ){
        return convertToValueObjects(sets, false);
    }
    

    /**
     * results will be sorted by size
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return
     */
    public List<DatabaseBackedGeneSetValueObject> convertToValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {

        return convertToLightValueObjects( genesets, includeOnesWithoutGenes, false );
    }
    

    /**
     * results will be sorted by size
     * @param genesets
     * @param includeOnesWithoutGenes if true, even gene sets that lack genes will be returned.
     * @return
     */
    public List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes ) {

        return convertToLightValueObjects( genesets, includeOnesWithoutGenes, true );
    }

    private List<DatabaseBackedGeneSetValueObject> convertToLightValueObjects( Collection<GeneSet> genesets,
            boolean includeOnesWithoutGenes, boolean light ) {
        
        List<DatabaseBackedGeneSetValueObject> results = new ArrayList<DatabaseBackedGeneSetValueObject>();

        for ( GeneSet gs : genesets ) {
            if ( !includeOnesWithoutGenes && geneSetDao.getGeneCount( gs.getId() ) <= 0 ) {
                continue;
            }

            if(light){
                results.add( convertToLightValueObject( gs ) );
            }else{
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
    

    /**
     * Method to create a GO group object from an ad hoc entity
     */
    public GOGroupValueObject convertToGOValueObject( GeneSet gs, String goId, String searchTerm ) {

        GOGroupValueObject ggvo = new GOGroupValueObject();

        
        ggvo.setName( gs.getName() );
        ggvo.setDescription( gs.getDescription() );
        ggvo.setSize( geneSetDao.getGeneCount( gs.getId() ) );
        
        Collection<Long> gids = new HashSet<Long>();
        for ( GeneSetMember gm : gs.getMembers() ) {
            gids.add( gm.getGene().getId() );
        }
        ggvo.setGeneIds( gids );
        
        
        Taxon tax = geneSetDao.getTaxon( gs.getId() );
        if( tax != null){
            while(tax.getParentTaxon() != null){
                tax = tax.getParentTaxon();
            }  
            ggvo.setTaxonId( tax.getId() );
            ggvo.setTaxonName( tax.getCommonName() );
        }
        
        ggvo.setId( new Long( -1 ) );
        ggvo.setModified( false );
        ggvo.setGoId( goId );
        ggvo.setSearchTerm( searchTerm );
        
        return ggvo;
    }
}
