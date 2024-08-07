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
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
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
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author keshav
 * @author pavlidis
 * @see CompositeSequenceService
 */
@Service
public class CompositeSequenceServiceImpl
        extends AbstractFilteringVoEnabledService<CompositeSequence, CompositeSequenceValueObject>
        implements CompositeSequenceService {

    private final BioSequenceService bioSequenceService;
    private final GeneProductService geneProductService;
    private final BlatResultService blatResultService;
    private final ArrayDesignService arrayDesignService;
    private final CompositeSequenceDao compositeSequenceDao;


    @Autowired
    public CompositeSequenceServiceImpl( CompositeSequenceDao compositeSequenceDao,
            BioSequenceService bioSequenceService, GeneProductService geneProductService,
            BlatResultService blatResultService, ArrayDesignService arrayDesignService ) {
        super( compositeSequenceDao );
        this.compositeSequenceDao = compositeSequenceDao;
        this.bioSequenceService = bioSequenceService;
        this.geneProductService = geneProductService;
        this.blatResultService = blatResultService;
        this.arrayDesignService = arrayDesignService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequence( BioSequence bioSequence ) {
        return this.compositeSequenceDao.findByBioSequence( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByBioSequenceName( String name ) {
        return this.compositeSequenceDao.findByBioSequenceName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( Gene gene ) {
        return this.compositeSequenceDao.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequenceValueObject loadValueObjectWithGeneMappingSummary( CompositeSequence cs ) {
        CompositeSequenceValueObject vo = loadValueObject( cs );
        if ( vo != null ) {
            // Not passing the vo since that would create data redundancy in the returned structure
            vo.setGeneMappingSummaries(
                    this.getGeneMappingSummary( this.bioSequenceService.findByCompositeSequence( cs ), null ) );
        }
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<CompositeSequenceValueObject> loadValueObjectsForGene( Gene gene, int start, int limit ) {
        Slice<CompositeSequence> probes = this.compositeSequenceDao.findByGene( gene, start, limit );
        Set<ArrayDesign> platforms = probes.stream().map( CompositeSequence::getArrayDesign ).collect( Collectors.toSet() );
        Map<Long, ArrayDesignValueObject> platformVos = arrayDesignService.loadValueObjects( platforms ).stream()
                .collect( Collectors.toMap( ArrayDesignValueObject::getId, Function.identity() ) );
        // FIXME: deal with potential null return values of loadValueObject
        return probes.map( probe -> {
            CompositeSequenceValueObject probeVo = loadValueObject( probe );
            if ( probeVo != null ) {
                probeVo.setArrayDesign( platformVos.get( probe.getArrayDesign().getId() ) );
            }
            return probeVo;
        } );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign ) {
        return this.compositeSequenceDao.findByGene( gene, arrayDesign );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByName( String name ) {
        return this.compositeSequenceDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequence findByName( ArrayDesign arrayDesign, String name ) {
        return this.compositeSequenceDao.findByName( arrayDesign, name );
    }

    /**
     * Checks to see if the CompositeSequence exists in any of the array designs. If so, it is internally stored in the
     * collection of composite sequences as a HashSet, preserving order based on insertion.
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> findByNamesInArrayDesigns( Collection<String> compositeSequenceNames,
            Collection<ArrayDesign> arrayDesigns ) {
        LinkedHashMap<String, CompositeSequence> compositeSequencesMap = new LinkedHashMap<>();

        for ( ArrayDesign arrayDesign : arrayDesigns ) {
            for ( String obj : compositeSequenceNames ) {
                String name = obj;
                name = StringUtils.strip( name );
                AbstractService.log.debug( "entered: " + name );
                CompositeSequence cs = this.findByName( arrayDesign, name );
                if ( cs != null && !compositeSequencesMap.containsKey( cs.getName() ) ) {
                    compositeSequencesMap.put( cs.getName(), cs );
                } else {
                    AbstractService.log.warn( "Composite sequence " + name + " does not exist.  Discarding ... " );
                }
            }
        }

        if ( compositeSequencesMap.isEmpty() )
            return null;

        return compositeSequencesMap.values();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<Gene>> getGenes( Collection<CompositeSequence> sequences ) {
        return this.compositeSequenceDao.getGenes( sequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenes( CompositeSequence compositeSequence ) {
        return this.getGenes( compositeSequence, 0, -1 );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Gene> getGenes( CompositeSequence compositeSequence, int offset, int limit ) {
        return this.compositeSequenceDao.getGenes( compositeSequence, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences ) {
        return this.compositeSequenceDao.getGenesWithSpecificity( compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( Collection<CompositeSequence> compositeSequences ) {
        return this.compositeSequenceDao.getRawSummary( compositeSequences );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Object[]> getRawSummary( ArrayDesign arrayDesign, int numResults ) {
        return this.compositeSequenceDao.getRawSummary( arrayDesign, numResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneMappingSummary> getGeneMappingSummary( BioSequence biologicalCharacteristic,
            @Nullable CompositeSequenceValueObject cs ) {

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

            BlatResultValueObject blatResult = null;

            if ( ( bs2gp instanceof BlatAssociation ) ) {
                BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                blatResult = new BlatResultValueObject( blatResultService.thaw( blatAssociation.getBlatResult() ) );
            } else if ( bs2gp instanceof AnnotationAssociation ) {
                /*
                 * Make a dummy blat result
                 */
                blatResult = new BlatResultValueObject( biologicalCharacteristic.getId() );
                blatResult.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
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
                summary.setCompositeSequence( cs );
                results.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }

        }

        this.addBlatResultsLackingGenes( biologicalCharacteristic, results, cs );

        if ( results.size() == 0 ) {
            // add a 'dummy' that at least contains the information about the CS. This is a bit of a hack...
            GeneMappingSummary summary = new GeneMappingSummary();
            summary.setCompositeSequence( cs );
            BlatResultValueObject newInstance = new BlatResultValueObject( -1L );
            newInstance.setQuerySequence( BioSequenceValueObject.fromEntity( biologicalCharacteristic ) );
            summary.setBlatResult( newInstance );
            results.put( ProbeMapUtils.hashBlatResult( newInstance ), summary );
        }

        return results.values();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> thaw( Collection<CompositeSequence> compositeSequences ) {
        compositeSequences = load( compositeSequences.stream().map( CompositeSequence::getId ).collect( Collectors.toSet() ) );
        this.compositeSequenceDao.thaw( compositeSequences );
        return compositeSequences;
    }

    @Override
    @Transactional(readOnly = true)
    public CompositeSequence thaw( CompositeSequence compositeSequence ) {
        compositeSequence = loadOrFail( compositeSequence.getId() );
        this.compositeSequenceDao.thaw( compositeSequence );
        return compositeSequence;
    }

    @Override
    @Transactional
    public void remove( Collection<CompositeSequence> sequencesToDelete ) {
        // check the collection to make sure it contains no transitive entities (just check the id and make sure its
        // non-null
        Collection<CompositeSequence> filteredSequence = new Vector<>();
        for ( CompositeSequence sequence : sequencesToDelete ) {
            if ( sequence.getId() != null )
                filteredSequence.add( sequence );
        }

        super.remove( filteredSequence );
    }

    /**
     * Note that duplicate hits will be ignored here. See bug 4037.
     */
    private void addBlatResultsLackingGenes( BioSequence biologicalCharacteristic,
            Map<Integer, GeneMappingSummary> blatResults, @Nullable CompositeSequenceValueObject cs ) {
        Collection<BlatResultValueObject> allBlatResultsForCs = blatResultService.loadValueObjects(
                blatResultService.thaw( blatResultService.findByBioSequence( biologicalCharacteristic ) ) );
        for ( BlatResultValueObject blatResult : allBlatResultsForCs ) {
            if ( !blatResults.containsKey( ProbeMapUtils.hashBlatResult( blatResult ) ) ) {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( cs );
                // no gene...
                blatResults.put( ProbeMapUtils.hashBlatResult( blatResult ), summary );
            }
        }
    }
}