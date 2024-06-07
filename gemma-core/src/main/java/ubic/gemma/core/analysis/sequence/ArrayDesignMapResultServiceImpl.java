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
package ubic.gemma.core.analysis.sequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.util.EntityUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * Supports obtaining detailed information about the sequence analysis of probes on microarrays.
 *
 * @author Paul
 */
@Component
public class ArrayDesignMapResultServiceImpl implements ArrayDesignMapResultService {

    private static final Log log = LogFactory.getLog( ArrayDesignMapResultServiceImpl.class.getName() );

    private final BlatResultService blatResultService;
    private final BlatAssociationService blatAssociationService;
    private final ArrayDesignService arrayDesignService;
    private final CompositeSequenceService compositeSequenceService;

    @Autowired
    public ArrayDesignMapResultServiceImpl( BlatResultService blatResultService,
            BlatAssociationService blatAssociationService, ArrayDesignService arrayDesignService,
            CompositeSequenceService compositeSequenceService ) {
        this.blatResultService = blatResultService;
        this.blatAssociationService = blatAssociationService;
        this.arrayDesignService = arrayDesignService;
        this.compositeSequenceService = compositeSequenceService;
    }

    @Override
    public Collection<CompositeSequenceMapSummary> summarizeMapResults( ArrayDesign arrayDesign ) {
        arrayDesign = arrayDesignService.thaw( arrayDesign );
        return this.summarizeMapResults( arrayDesign.getCompositeSequences() );

    }

    @Override
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( ArrayDesign arrayDesign ) {
        Collection<Object[]> sequenceData = compositeSequenceService.getRawSummary( arrayDesign, -1 );
        return this.getSummaryMapValueObjects( sequenceData );
    }

    @Override
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( Collection<Object[]> sequenceData ) {
        Map<Long, CompositeSequenceMapValueObject> summary = new HashMap<>();
        Map<Long, Set<Integer>> blatResultCount = new HashMap<>();

        for ( Object o : sequenceData ) {
            Object[] row = ( Object[] ) o;

            Long csId = ( ( BigInteger ) row[0] ).longValue();

            CompositeSequenceMapValueObject vo;
            if ( summary.containsKey( csId ) ) {
                vo = summary.get( csId );
            } else {
                vo = new CompositeSequenceMapValueObject();
                summary.put( csId, vo );
            }

            String csName = ( String ) row[1];
            String bioSequenceName = ( String ) row[2];
            String bioSequenceNcbiId = ( String ) row[3];

            Long blatId = null;
            if ( row[4] != null ) {
                blatId = ( ( BigInteger ) row[4] ).longValue();
            }

            if ( row[10] != null ) {
                // When viewing array designs, many will not have a gene.
                Long geneProductId = ( ( BigInteger ) row[5] ).longValue();
                String geneProductName = ( String ) row[6];
                String geneProductAccession = ( String ) row[7];
                Object geneProductGeneId = row[8];
                Long geneId = ( ( BigInteger ) row[9] ).longValue();
                String geneName = ( String ) row[10];
                Integer geneAccession = ( Integer ) row[11]; // NCBI

                // fill in value object for geneProducts
                Map<Long, GeneProductValueObject> geneProductSet = vo.getGeneProducts();
                // if the geneProduct is already in the map, do not do anything.
                // if it isn't there, put it in the map
                if ( !geneProductSet.containsKey( geneProductId ) ) {
                    GeneProductValueObject gpVo = new GeneProductValueObject( geneProductId );
                    gpVo.setName( geneProductName );
                    gpVo.setNcbiId( geneProductAccession );
                    if ( geneProductGeneId != null ) {
                        gpVo.setGeneId( ( ( BigInteger ) geneProductGeneId ).longValue() );
                    }
                    geneProductSet.put( geneProductId, gpVo );
                }

                Map<Long, GeneValueObject> geneSet = vo.getGenes();
                if ( !geneSet.containsKey( geneId ) ) {
                    GeneValueObject gVo = new GeneValueObject( geneId );
                    gVo.setOfficialSymbol( geneName );
                    gVo.setNcbiId( geneAccession );
                    geneSet.put( geneId, gVo );
                }

            }

            String arrayDesignShortName = ( String ) row[12];
            Long arrayDesignId = ( ( BigInteger ) row[13] ).longValue();

            String csDesc = ( String ) row[19];
            vo.setCompositeSequenceDescription( csDesc );

            vo.setArrayDesignId( arrayDesignId );

            vo.setCompositeSequenceId( csId.toString() );
            vo.setCompositeSequenceName( csName );

            vo.setArrayDesignShortName( arrayDesignShortName );
            vo.setArrayDesignName( ( String ) row[20] );

            // fill in value object
            if ( bioSequenceName != null && vo.getBioSequenceName() == null ) {
                vo.setBioSequenceName( bioSequenceName );
            }

            // fill in value object
            if ( bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null ) {
                vo.setBioSequenceNcbiId( bioSequenceNcbiId );
            }

            if ( blatId != null )
                this.countBlatHits( row, blatResultCount, csId, vo );

        }

        return summary.values();
    }

    @Override
    public Collection<CompositeSequenceMapSummary> summarizeMapResults(
            Collection<CompositeSequence> compositeSequences ) {
        Collection<CompositeSequenceMapSummary> result = new HashSet<>();

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

            Collection<BlatAssociation> maps = blatAssociationService.findAndThaw( bioSequence );
            for ( BlatAssociation association : maps ) {
                summary.getGeneProducts().add( association.getGeneProduct() );
                summary.getGenes().add( association.getGeneProduct().getGene() );
            }

            result.add( summary );

            if ( ++count % 1000 == 0 ) {
                ArrayDesignMapResultServiceImpl.log.info( "Processed " + count + " elements..." );
            }

        }
        ArrayDesignMapResultServiceImpl.log.info( "Done, processed " + count + " elements" );
        return result;
    }

    /**
     * count the number of distinct blat hits
     *
     * @param blatResultCount map of csid to blat result hashes.
     */
    private void countBlatHits( Object[] row, Map<Long, Set<Integer>> blatResultCount, Long csId,
            CompositeSequenceMapValueObject vo ) {

        Long chromId = ( ( BigInteger ) row[14] ).longValue();
        Long targetStart = ( ( BigInteger ) row[15] ).longValue();
        Long targetEnd = ( ( BigInteger ) row[16] ).longValue();
        String targetStarts = ( String ) row[17];
        Long queryId = ( ( BigInteger ) row[18] ).longValue();

        int hash = Objects.hash( chromId, targetStart, targetEnd, targetStarts, queryId );

        EntityUtils.populateMapSet( blatResultCount, csId, hash );

        if ( vo.getNumBlatHits() == null ) {
            vo.setNumBlatHits( 1 );
        } else {
            vo.setNumBlatHits( blatResultCount.get( csId ).size() );
        }

    }

}
