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
public class ArrayDesignMapResultService {

    private static Log log = LogFactory.getLog( ArrayDesignMapResultService.class.getName() );

    @Autowired
    private BlatResultService blatResultService;

    @Autowired
    private BlatAssociationService blatAssociationService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

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
        arrayDesign = arrayDesignService.thaw( arrayDesign );
        return this.summarizeMapResults( arrayDesign.getCompositeSequences() );

    }

    /**
     * @param arrayDesign
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( ArrayDesign arrayDesign ) {
        Collection<Object[]> sequenceData = compositeSequenceService.getRawSummary( arrayDesign, null );
        return getSummaryMapValueObjects( sequenceData );
    }

    /**
     * Version of objects that retains less information.
     * 
     * @param sequenceData
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getSmallerSummaryMapValueObjects(
            Collection<Object[]> sequenceData ) {
        Map<Long, CompositeSequenceMapValueObject> summary = new HashMap<Long, CompositeSequenceMapValueObject>();
        Map<Long, HashSet<Long>> blatResultCount = new HashMap<Long, HashSet<Long>>();

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

            Object blatId = row[4];

            Object geneId = row[5];

            String geneName = ( String ) row[6];

            vo.setCompositeSequenceId( csId.toString() );
            vo.setCompositeSequenceName( csName );

            // fill in value object
            if ( bioSequenceName != null && vo.getBioSequenceName() == null ) {
                vo.setBioSequenceName( bioSequenceName );
            }

            // fill in value object
            if ( bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null ) {
                vo.setBioSequenceNcbiId( bioSequenceNcbiId );
            }

            countBlatHits( blatResultCount, csId, vo, blatId );

            // fill in value object for genes
            if ( geneId != null ) {
                Map<String, GeneValueObject> geneSet = vo.getGenes();
                if ( !geneSet.containsKey( geneId ) ) {
                    GeneValueObject gVo = new GeneValueObject();
                    gVo.setId( ( ( BigInteger ) geneId ).longValue() );
                    gVo.setOfficialSymbol( geneName );
                    geneSet.put( ( ( BigInteger ) geneId ).toString(), gVo );
                }
            }

        }

        return summary.values();
    }

    /**
     * @param blatResultCount
     * @param csId
     * @param vo
     * @param blatId
     */
    private void countBlatHits( Map<Long, HashSet<Long>> blatResultCount, Long csId,
            CompositeSequenceMapValueObject vo, Object blatId ) {
        // count the number of blat hits
        if ( blatId != null ) {
            Long blatIdObj = ( ( BigInteger ) blatId ).longValue();
            if ( blatResultCount.containsKey( csId ) ) {
                blatResultCount.get( csId ).add( blatIdObj );
            } else {
                HashSet<Long> blatResultHash = new HashSet<Long>();
                blatResultHash.add( blatIdObj );
                blatResultCount.put( csId, blatResultHash );
            }

            if ( vo.getNumBlatHits() == null ) {
                vo.setNumBlatHits( new Long( 1 ) );
            } else {
                vo.setNumBlatHits( blatResultCount.get( csId ).size() );
            }
        }
    }

    /**
     * FIXME this is only public so we can use it in the DesignElementController; need refactoring (see
     * CompositeSequenceService) Function to get a collection of CompositeSequenceMapValueObjects that contain
     * information about a composite sequence and related tables.
     * 
     * @param rawSummaryData - raw results from SQL query to get CS information.
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( Collection<Object[]> sequenceData ) {
        Map<Long, CompositeSequenceMapValueObject> summary = new HashMap<Long, CompositeSequenceMapValueObject>();
        Map<Long, HashSet<Long>> blatResultCount = new HashMap<Long, HashSet<Long>>();

        for ( Object o : sequenceData ) {
            Object[] row = ( Object[] ) o;

            Long csId = ( ( BigInteger ) row[0] ).longValue();

            String csName = ( String ) row[1];
            String bioSequenceName = ( String ) row[2];
            String bioSequenceNcbiId = ( String ) row[3];

            Object blatId = row[4];

            Object geneProductId = row[5];

            String geneProductName = ( String ) row[6];
            String geneProductAccession = ( String ) row[7];
            Object geneProductGeneId = row[8];
            String geneProductType = ( String ) row[9];

            Object geneId = row[10];

            String geneName = ( String ) row[11];
            Integer geneAccession = ( Integer ) row[12]; // NCBI

            String arrayDesignShortName = ( String ) row[13];
            BigInteger arrayDesignId = ( BigInteger ) row[14];

            CompositeSequenceMapValueObject vo;
            if ( summary.containsKey( csId ) ) {
                vo = summary.get( csId );
            } else {
                vo = new CompositeSequenceMapValueObject();
                summary.put( csId, vo );
            }

            String csDesc = ( String ) row[15];
            vo.setCompositeSequenceDescription( csDesc );

            vo.setArrayDesignId( arrayDesignId.longValue() );

            vo.setCompositeSequenceId( csId.toString() );
            vo.setCompositeSequenceName( csName );

            vo.setArrayDesignName( arrayDesignShortName );

            // fill in value object
            if ( bioSequenceName != null && vo.getBioSequenceName() == null ) {
                vo.setBioSequenceName( bioSequenceName );
            }

            // fill in value object
            if ( bioSequenceNcbiId != null && vo.getBioSequenceNcbiId() == null ) {
                vo.setBioSequenceNcbiId( bioSequenceNcbiId );
            }

            countBlatHits( blatResultCount, csId, vo, blatId );

            // fill in value object for geneProducts
            if ( geneProductId != null ) {
                Map<String, GeneProductValueObject> geneProductSet = vo.getGeneProducts();
                // if the geneProduct is already in the map, do not do anything.
                // if it isn't there, put it in the map
                if ( !geneProductSet.containsKey( geneProductId ) ) {
                    GeneProductValueObject gpVo = new GeneProductValueObject();
                    gpVo.setId( ( ( BigInteger ) geneProductId ).longValue() );
                    gpVo.setName( geneProductName );
                    gpVo.setNcbiId( geneProductAccession );
                    if ( geneProductGeneId != null ) {
                        gpVo.setGeneId( ( ( BigInteger ) geneProductGeneId ).longValue() );
                    }
                    gpVo.setType( geneProductType );
                    geneProductSet.put( ( ( BigInteger ) geneProductId ).toString(), gpVo );
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

    /**
     * Non-HQL version of the composite sequence data summary query. Returns a summary of the composite sequence data
     * and related tables.
     * 
     * @param compositeSequences
     * @return
     */
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

}
