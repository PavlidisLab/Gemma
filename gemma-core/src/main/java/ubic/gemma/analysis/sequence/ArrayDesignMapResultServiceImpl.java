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

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;

/**
 * Supports obtaining detailed information about the sequence analysis of probes on microarrays.
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class ArrayDesignMapResultServiceImpl implements ArrayDesignMapResultService {

    private static Log log = LogFactory.getLog( ArrayDesignMapResultServiceImpl.class.getName() );

    @Autowired
    private BlatResultService blatResultService;

    @Autowired
    private BlatAssociationService blatAssociationService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.sequence.ArrayDesignMapResultService#summarizeMapResults(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign)
     */
    @Override
    public Collection<CompositeSequenceMapSummary> summarizeMapResults( ArrayDesign arrayDesign ) {
        arrayDesign = arrayDesignService.thaw( arrayDesign );
        return this.summarizeMapResults( arrayDesign.getCompositeSequences() );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.sequence.ArrayDesignMapResultService#getSummaryMapValueObjects(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign)
     */
    @Override
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( ArrayDesign arrayDesign ) {
        Collection<Object[]> sequenceData = compositeSequenceService.getRawSummary( arrayDesign, null );
        return getSummaryMapValueObjects( sequenceData );
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // * ubic.gemma.analysis.sequence.ArrayDesignMapResultService#getSmallerSummaryMapValueObjects(java.util.Collection)
    // */
    // @Override
    // public Collection<CompositeSequenceMapValueObject> getSmallerSummaryMapValueObjects(
    // Collection<Object[]> sequenceData ) {
    // Map<Long, CompositeSequenceMapValueObject> summary = new HashMap<>();
    // Map<Long, Set<Long>> blatResultCount = new HashMap<>();
    //
    // for ( Object o : sequenceData ) {
    // Object[] row = ( Object[] ) o;
    //
    // Long csId = ( ( BigInteger ) row[0] ).longValue();
    // CompositeSequenceMapValueObject vo;
    // if ( summary.containsKey( csId ) ) {
    // vo = summary.get( csId );
    // } else {
    // vo = new CompositeSequenceMapValueObject();
    // summary.put( csId, vo );
    // }
    //
    // String csName = ( String ) row[1];
    // String bioSequenceName = ( String ) row[2];
    // String bioSequenceNcbiId = ( String ) row[3];
    //
    // Object blatId = row[4];
    //
    // Object geneId = row[5];
    //
    // String geneName = ( String ) row[6];
    //
    // vo.setCompositeSequenceId( csId.toString() );
    // vo.setCompositeSequenceName( csName );
    //
    // // fill in value object
    // if ( bioSequenceName != null && vo.getBioSequenceName() == null ) {
    // vo.setBioSequenceName( bioSequenceName );
    // }
    //
    // // fill in value object
    // if ( bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null ) {
    // vo.setBioSequenceNcbiId( bioSequenceNcbiId );
    // }
    //
    // countBlatHits( blatResultCount, csId, vo, blatId );
    //
    // // fill in value object for genes
    // if ( geneId != null ) {
    // Map<String, GeneValueObject> geneSet = vo.getGenes();
    // if ( !geneSet.containsKey( geneId ) ) {
    // GeneValueObject gVo = new GeneValueObject();
    // gVo.setId( ( ( BigInteger ) geneId ).longValue() );
    // gVo.setOfficialSymbol( geneName );
    // geneSet.put( ( ( BigInteger ) geneId ).toString(), gVo );
    // }
    // }
    //
    // }
    //
    // return summary.values();
    // }

    /**
     * count the number of distinct blat hits
     * 
     * @param row
     * @param blatResultCount map of csid to blat result hashes.
     * @param csId
     * @param vo
     */
    private void countBlatHits( Object[] row, Map<Long, Set<Integer>> blatResultCount, Long csId,
            CompositeSequenceMapValueObject vo ) {

        Long chromId = ( ( BigInteger ) row[15] ).longValue();
        Long targetStart = ( ( BigInteger ) row[16] ).longValue();
        Long targetEnd = ( ( BigInteger ) row[17] ).longValue();
        String targetStarts = ( String ) row[18];
        Long queryId = ( ( BigInteger ) row[19] ).longValue();

        int hash = 1;
        int prime = 31;
        hash = prime * hash + chromId.hashCode();
        hash = prime * hash + targetStart.hashCode();
        hash = prime * hash + targetEnd.hashCode();
        hash = prime * hash + targetStarts.hashCode();
        hash = prime * hash + queryId.hashCode();

        if ( blatResultCount.containsKey( csId ) ) {
            blatResultCount.get( csId ).add( hash );
        } else {
            Set<Integer> blatResultHash = new HashSet<>();
            blatResultHash.add( hash );
            blatResultCount.put( csId, blatResultHash );
        }

        if ( vo.getNumBlatHits() == null ) {
            vo.setNumBlatHits( 1 );
        } else {
            vo.setNumBlatHits( blatResultCount.get( csId ).size() );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.sequence.ArrayDesignMapResultService#getSummaryMapValueObjects(java.util.Collection)
     */
    @Override
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( Collection<Object[]> sequenceData ) {
        Map<Long, CompositeSequenceMapValueObject> summary = new HashMap<>();
        Map<Long, Set<Integer>> blatResultCount = new HashMap<>();

        for ( Object o : sequenceData ) {
            Object[] row = ( Object[] ) o;

            Long csId = ( ( BigInteger ) row[0] ).longValue();

            String csName = ( String ) row[1];
            String bioSequenceName = ( String ) row[2];
            String bioSequenceNcbiId = ( String ) row[3];

            Long blatId = null;
            if ( row[4] != null ) {
                blatId = ( ( BigInteger ) row[4] ).longValue();
            }

            Long geneProductId = ( ( BigInteger ) row[5] ).longValue();

            String geneProductName = ( String ) row[6];
            String geneProductAccession = ( String ) row[7];
            Object geneProductGeneId = row[8];
            String geneProductType = ( String ) row[9];

            Object geneId = row[10];

            String geneName = ( String ) row[11];
            Integer geneAccession = ( Integer ) row[12]; // NCBI

            String arrayDesignShortName = ( String ) row[13];
            Long arrayDesignId = ( ( BigInteger ) row[14] ).longValue();

            CompositeSequenceMapValueObject vo;
            if ( summary.containsKey( csId ) ) {
                vo = summary.get( csId );
            } else {
                vo = new CompositeSequenceMapValueObject();
                summary.put( csId, vo );
            }

            String csDesc = ( String ) row[20];
            vo.setCompositeSequenceDescription( csDesc );

            vo.setArrayDesignId( arrayDesignId.longValue() );

            vo.setCompositeSequenceId( csId.toString() );
            vo.setCompositeSequenceName( csName );

            vo.setArrayDesignShortName( arrayDesignShortName );
            vo.setArrayDesignName( ( String ) row[21] );

            // fill in value object
            if ( bioSequenceName != null && vo.getBioSequenceName() == null ) {
                vo.setBioSequenceName( bioSequenceName );
            }

            // fill in value object
            if ( bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null ) {
                vo.setBioSequenceNcbiId( bioSequenceNcbiId );
            }

            if ( blatId != null ) countBlatHits( row, blatResultCount, csId, vo );

            // fill in value object for geneProducts
            if ( geneProductId != null ) {
                Map<String, GeneProductValueObject> geneProductSet = vo.getGeneProducts();
                // if the geneProduct is already in the map, do not do anything.
                // if it isn't there, put it in the map
                if ( !geneProductSet.containsKey( geneProductId ) ) {
                    GeneProductValueObject gpVo = new GeneProductValueObject();
                    gpVo.setId( geneProductId.longValue() );
                    gpVo.setName( geneProductName );
                    gpVo.setNcbiId( geneProductAccession );
                    if ( geneProductGeneId != null ) {
                        gpVo.setGeneId( ( ( BigInteger ) geneProductGeneId ).longValue() );
                    }
                    gpVo.setType( geneProductType );
                    geneProductSet.put( geneProductId.toString(), gpVo );
                }
            }

            // fill in value object for genes
            if ( geneId != null ) {
                Map<String, GeneValueObject> geneSet = vo.getGenes();
                if ( !geneSet.containsKey( geneId ) ) {
                    GeneValueObject gVo = new GeneValueObject();
                    gVo.setId( ( ( BigInteger ) geneId ).longValue() );
                    gVo.setOfficialSymbol( geneName );
                    gVo.setNcbiId( geneAccession );
                    geneSet.put( ( ( BigInteger ) geneId ).toString(), gVo );
                }
            }

        }

        return summary.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.sequence.ArrayDesignMapResultService#summarizeMapResults(java.util.Collection)
     */
    @Override
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

}
