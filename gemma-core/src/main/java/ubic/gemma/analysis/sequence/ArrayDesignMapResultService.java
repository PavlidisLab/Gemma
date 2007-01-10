/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.analysis.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;

/**
 * Supports obtaining detailed information about the sequence analysis of probes on microarrays.
 * 
 * @spring.bean id="arrayDesignMapResultService"
 * @spring.property name="blatAssociationService" ref="blatAssociationService"
 * @spring.property name="arrayDesignService" ref ="arrayDesignService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="geneProductService" ref="geneProductService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @author Paul
 * @version $Id$
 */
public class ArrayDesignMapResultService {

    private static Log log = LogFactory.getLog( ArrayDesignMapResultService.class.getName() );

    private BlatResultService blatResultService;
    private BlatAssociationService blatAssociationService;
    private ArrayDesignService arrayDesignService;
    private GeneProductService geneProductService;
    private GeneService geneService;
    private BioSequenceService bioSequenceService;
    private CompositeSequenceService compositeSequenceService;

    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    public void setBlatAssociationService( BlatAssociationService blatAssociationService ) {
        this.blatAssociationService = blatAssociationService;
    }

    /**
     * @param arrayDesign
     * @return
     */
    public Collection<CompositeSequenceMapSummary> summarizeMapResults( ArrayDesign arrayDesign ) {
        arrayDesignService.thaw( arrayDesign );
        return this.summarizeMapResults( arrayDesign.getCompositeSequences() );

    }
    
    public Collection<CompositeSequenceMapSummary> getSummaryMapResults(ArrayDesign arrayDesign) {
        Collection<CompositeSequenceMapSummary> compositeSequenceSummaries = null;
        
        Collection sequenceData = arrayDesignService.getRawCompositeSequenceSummary( arrayDesign );
        
        HashMap<Long, Long> bioSequenceIds = new HashMap<Long,Long>();
        HashMap<Long, HashSet<Long>> blatResultIds = new HashMap<Long,HashSet<Long>>();
        HashMap<Long, HashSet<Long>> geneProductIds = new HashMap<Long,HashSet<Long>>();
        HashMap<Long, HashSet<Long>> geneIds = new HashMap<Long,HashSet<Long>>();
        HashSet<Long> compositeSequenceIds = new HashSet<Long>();
        
        for ( Object o : sequenceData ) {
            String[] row = (String[]) o;
            Long csId = Long.parseLong( row[0] );
            
            Long bioSequenceId = null ;
            if (row[1] != null) {
                Long.parseLong( row[1] );
            }
            Long blatId = null; 
            if (row[3] != null) {
                Long.parseLong(row[3]);
            }
            Long geneProductId = null;
            if (row[4] != null) {
                Long.parseLong(row[4]);
            }
            Long geneId = null;
            if (row[5] != null) {
                Long.parseLong(row[5]);
            }
            
            // store compositeSequenceId
            compositeSequenceIds.add( csId );
            // fill in bioSequence if not known
            if ( bioSequenceId != null && !bioSequenceIds.containsKey( csId ) ) {
                bioSequenceIds.put( csId, bioSequenceId );
            }
            // add in blatResult (if it exists)
            addToResultSet( blatResultIds, csId, blatId );
            // add to geneProducts (if it exists)
            addToResultSet( geneProductIds, csId, geneProductId);
            // add to genes (if it exists)
            addToResultSet(geneIds, csId, geneId);
        }
        
        sequenceData = null;
        
        // all ids are in memory now. Mass-load compositeSequences, bioSequences, blatResults, geneProducts, and genes.
        Collection<CompositeSequence> compositeSequences = compositeSequenceService.load( compositeSequenceIds );
        Collection<BioSequence> bioSequences = bioSequenceService.load( bioSequenceIds.values() );
        Collection<BlatResult> blatResults = blatResultService.load( blatResultIds.values() );
        Collection<GeneProduct> geneProducts = geneProductService.load( geneProductIds.values() );
        Collection<Gene> genes = geneService.load( geneIds.values() );
     
        // build summary array
        compositeSequenceSummaries = new ArrayList<CompositeSequenceMapSummary>();
        for ( CompositeSequence cs : compositeSequences) {
            Long csId = cs.getId();
            CompositeSequenceMapSummary summary = new CompositeSequenceMapSummary(cs);
            summary.setBlatResults( blatResults );
            
        }
        return compositeSequenceSummaries;
    }

    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects(ArrayDesign arrayDesign) {
        
        Collection sequenceData = arrayDesignService.getRawCompositeSequenceSummary( arrayDesign );
        
        HashMap<Long,CompositeSequenceMapValueObject> summary = new HashMap<Long,CompositeSequenceMapValueObject>();
        
        for ( Object o : sequenceData ) {
            String[] row = (String[]) o;
            Long csId = Long.parseLong( row[0] );
            String csName = row[1];
            String bioSequenceName = row[2];
            String bioSequenceNcbiId = row[3];
            String blatId = row[4];
            String geneProductId = row[5];
            String geneProductName = row[6];
            String geneProductAccession = row[7];


            Long geneId = null;
            if (row[5] != null) {
                Long.parseLong(row[5]);
            }
            
            CompositeSequenceMapValueObject vo;
            if (summary.containsKey( csId )) {
                vo = summary.get( csId );
            }
            else {
                vo = new CompositeSequenceMapValueObject();
                summary.put( csId, vo );
            }
            
            // fill in value object
            if (bioSequenceName != null && vo.getBioSequenceName() == null) {
                vo.setBioSequenceName( bioSequenceName );
            }
            
            // fill in value object
            if (bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null) {
                vo.setBioSequenceNcbiId( bioSequenceNcbiId );
            }
            
            // count the number of blat hits
            if (blatId != null) {
                if (vo.getNumBlatHits() == null) {
                    vo.setNumBlatHits( new Long(1) );
                }   
                else {
                    Long blatHits = vo.getNumBlatHits();
                    blatHits++;
                    vo.setNumBlatHits( blatHits );
                }
            }
            
            // fill in value object for geneProducts
            if (geneProductId != null) {
                Set<GeneProductValueObject> geneProductSet =  vo.getGeneProducts();
                

                
            }

        }
        
             
        
        return null;
    }
    
    /**
     * @param results
     * @param keyId
     * @param valueId
     */
    private void addToResultSet( HashMap<Long, HashSet<Long>> results, Long keyId, Long valueId ) {
        if ( valueId != null ) {
            HashSet<Long> blatList;
            if ( results.containsKey( keyId ) ) {
                blatList = ( HashSet<Long> ) results.get( keyId );
            } else {
                blatList = new HashSet<Long>();
            }
            blatList.add( valueId );
        }
    }

    /**
     * @param compositeSequences
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<CompositeSequenceMapSummary> summarizeMapResults( Collection<CompositeSequence> compositeSequences ) {
        Collection<CompositeSequenceMapSummary> result = new HashSet<CompositeSequenceMapSummary>();

        int count = 0;
        for ( CompositeSequence cs : compositeSequences ) {
            CompositeSequenceMapSummary summary = new CompositeSequenceMapSummary( cs );

            BioSequence bioSequence = cs.getBiologicalCharacteristic();

            if ( bioSequence == null ) {
                result.add( summary );
                continue;
            }

            Collection<BlatResult> blats = blatResultService.findByBioSequence( bioSequence );
            summary.setBlatResults( blats );

            Collection<BlatAssociation> maps = blatAssociationService.find( bioSequence );
            blatAssociationService.thaw( maps );
            for ( BlatAssociation association : maps ) {
                summary.getGeneProducts().add( association.getGeneProduct() );
                summary.getGenes().add( association.getGeneProduct().getGene() );
            }

            result.add( summary );

            if ( ++count % 1000 == 0 ) {
                log.info( "Processed " + count + " elements..." );
            }

        }
        log.info( "Done, processed " + count + " elements" );
        return result;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

}
