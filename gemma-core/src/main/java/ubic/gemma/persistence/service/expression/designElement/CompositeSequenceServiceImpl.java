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
package ubic.gemma.persistence.service.expression.designElement;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.sequence.GeneMappingSummary;
import ubic.gemma.core.analysis.sequence.ProbeMapUtils;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;

import java.util.*;

/**
 * @author keshav
 * @author pavlidis
 * @see CompositeSequenceService
 */
@Service
public class CompositeSequenceServiceImpl extends CompositeSequenceServiceBase {

    private final BioSequenceService bioSequenceService;
    private final GeneProductService geneProductService;
    private final BlatResultService blatResultService;

    @Autowired
    public CompositeSequenceServiceImpl( CompositeSequenceDao compositeSequenceDao,
            BioSequenceService bioSequenceService, GeneProductService geneProductService,
            BlatResultService blatResultService ) {
        super( compositeSequenceDao );
        this.bioSequenceService = bioSequenceService;
        this.geneProductService = geneProductService;
        this.blatResultService = blatResultService;
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequence thaw( CompositeSequence compositeSequence ) {
        return this.compositeSequenceDao.thaw( compositeSequence );
    }

    @Override
    public void thaw( Collection<CompositeSequence> compositeSequences ) {
        this.compositeSequenceDao.thaw( compositeSequences );
    }

    @Override
    public Collection<GeneMappingSummary> getGeneMappingSummary( CompositeSequence cs ) {
        BioSequence biologicalCharacteristic = cs.getBiologicalCharacteristic();

        biologicalCharacteristic = bioSequenceService.thaw( biologicalCharacteristic );

        Map<Integer, GeneMappingSummary> results = new HashMap<>();
        if ( biologicalCharacteristic == null || biologicalCharacteristic.getBioSequence2GeneProduct() == null ) {
            return results.values();
        }

        Collection<BioSequence2GeneProduct> bs2gps = biologicalCharacteristic.getBioSequence2GeneProduct();

        for ( BioSequence2GeneProduct bs2gp : bs2gps ) {
            GeneProductValueObject geneProduct = new GeneProductValueObject(
                    geneProductService.thaw( bs2gp.getGeneProduct() ) );

            GeneValueObject gene = new GeneValueObject( bs2gp.getGeneProduct().getGene() );

            assert gene != null;

            BlatResultValueObject blatResult = null;

            if ( ( bs2gp instanceof BlatAssociation ) ) {
                BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                blatResult = new BlatResultValueObject( blatResultService.thaw( blatAssociation.getBlatResult() ) );
            } else if ( bs2gp instanceof AnnotationAssociation ) {
                /*
                 * Make a dummy blat result
                 */
                blatResult = new BlatResultValueObject();
                blatResult.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
                blatResult.setId( biologicalCharacteristic.getId() );
            }

            if ( blatResult == null ) {
                continue;
            }

            if ( results.containsKey( ProbeMapUtils.hashBlatResult( blatResult ) ) ) {
                results.get( ProbeMapUtils.hashBlatResult( blatResult ) ).addGene( geneProduct, gene );
            } else {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.addGene( geneProduct, gene );
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( loadValueObject( cs ) );
                results.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }

        }

        addBlatResultsLackingGenes( cs, results );

        if ( results.size() == 0 ) {
            // add a 'dummy' that at least contains the information about the CS. This is a bit of a hack...
            GeneMappingSummary summary = new GeneMappingSummary();
            summary.setCompositeSequence( loadValueObject( cs ) );
            BlatResultValueObject newInstance = new BlatResultValueObject( -1L );
            newInstance.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
            summary.setBlatResult( newInstance );
            results.put( ProbeMapUtils.hashBlatResult( newInstance ), summary );
        }

        return results.values();
    }

    @Override
    protected Collection<CompositeSequence> handleFindByBioSequence( BioSequence bioSequence ) {
        return this.compositeSequenceDao.findByBioSequence( bioSequence );
    }

    @Override
    protected Collection<CompositeSequence> handleFindByBioSequenceName( String name ) {
        return this.compositeSequenceDao.findByBioSequenceName( name );
    }

    @Override
    protected Collection<CompositeSequence> handleFindByGene( Gene gene ) {
        return this.compositeSequenceDao.findByGene( gene );
    }

    @Override
    protected Collection<CompositeSequenceValueObject> handleLoadValueObjectsForGene( Gene gene, int start,
            int limit ) {
        return loadValueObjects( this.compositeSequenceDao.findByGene( gene, start, limit ) );
    }

    @Override
    protected Collection<CompositeSequence> handleFindByGene( Gene gene, ArrayDesign arrayDesign ) {
        return this.compositeSequenceDao.findByGene( gene, arrayDesign );
    }

    @Override
    protected CompositeSequence handleFindByName( ArrayDesign arrayDesign, String name ) {
        return this.compositeSequenceDao.findByName( arrayDesign, name );
    }

    @Override
    protected Collection<CompositeSequence> handleFindByName( String name ) {
        return this.compositeSequenceDao.findByName( name );
    }

    /**
     * Checks to see if the CompositeSequence exists in any of the array designs. If so, it is internally stored in the
     * collection of composite sequences as a HashSet, preserving order based on insertion.
     */
    @Override
    protected Collection<CompositeSequence> handleFindByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns ) {
        LinkedHashMap<String, CompositeSequence> compositeSequencesMap = new LinkedHashMap<>();

        for ( ArrayDesign arrayDesign : arrayDesigns ) {
            for ( Object obj : compositeSequenceNames ) {
                String name = ( String ) obj;
                name = StringUtils.trim( name );
                log.debug( "entered: " + name );
                CompositeSequence cs = this.findByName( arrayDesign, name );
                if ( cs != null && !compositeSequencesMap.containsKey( cs.getName() ) ) {
                    compositeSequencesMap.put( cs.getName(), cs );
                } else {
                    log.warn( "Composite sequence " + name + " does not exist.  Discarding ... " );
                }
            }
        }

        if ( compositeSequencesMap.isEmpty() )
            return null;

        return compositeSequencesMap.values();
    }

    @Override
    protected Map<CompositeSequence, Collection<Gene>> handleGetGenes( Collection<CompositeSequence> sequences ) {
        return this.compositeSequenceDao.getGenes( sequences );
    }

    @Override
    protected Collection<Gene> handleGetGenes( CompositeSequence compositeSequence ) {
        return this.compositeSequenceDao.getGenes( compositeSequence );
    }

    @Override
    protected Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences ) {
        return this.compositeSequenceDao.getGenesWithSpecificity( compositeSequences );
    }

    @Override
    protected Collection<Object[]> handleGetRawSummary( ArrayDesign arrayDesign, Integer numResults ) {
        return this.compositeSequenceDao.getRawSummary( arrayDesign, numResults );
    }

    @Override
    protected Collection<Object[]> handleGetRawSummary( Collection<CompositeSequence> compositeSequences,
            Integer numResults ) {
        return this.compositeSequenceDao.getRawSummary( compositeSequences, numResults );
    }

    @Override
    protected Collection<Object[]> handleGetRawSummary( CompositeSequence compositeSequence, Integer numResults ) {
        return this.compositeSequenceDao.getRawSummary( compositeSequence, numResults );
    }

    @Override
    protected void handleRemove( Collection<CompositeSequence> sequencesToDelete ) {
        // check the collection to make sure it contains no transitive entities (just check the id and make sure its
        // non-null
        Collection<CompositeSequence> filteredSequence = new Vector<>();
        for ( Object sequence : sequencesToDelete ) {
            if ( ( ( CompositeSequence ) sequence ).getId() != null )
                filteredSequence.add( ( CompositeSequence ) sequence );
        }

        this.compositeSequenceDao.remove( filteredSequence );
    }

    /**
     * Note that duplicate hits will be ignored here. See bug 4037.
     */
    private void addBlatResultsLackingGenes( CompositeSequence cs, Map<Integer, GeneMappingSummary> blatResults ) {
        /*
         * Pick up blat results that didn't map to genes.
         */
        BioSequence biologicalCharacteristic = bioSequenceService.thaw( cs.getBiologicalCharacteristic() );

        Collection<BlatResultValueObject> allBlatResultsForCs = blatResultService.loadValueObjects(
                blatResultService.thaw( blatResultService.findByBioSequence( biologicalCharacteristic ) ) );
        for ( BlatResultValueObject blatResult : allBlatResultsForCs ) {
            if ( !blatResults.containsKey( ProbeMapUtils.hashBlatResult( blatResult ) ) ) {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( loadValueObject( cs ) );
                // no gene...
                blatResults.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }
        }
    }
}