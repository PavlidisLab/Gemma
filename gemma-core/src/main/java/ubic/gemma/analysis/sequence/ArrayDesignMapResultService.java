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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
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
 * @author Paul
 * @version $Id$
 */
public class ArrayDesignMapResultService {

    private static Log log = LogFactory.getLog( ArrayDesignMapResultService.class.getName() );

    private BlatResultService blatResultService;
    private BlatAssociationService blatAssociationService;
    private ArrayDesignService arrayDesignService;

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

            if ( bioSequence != null ) {
                Collection<BlatResult> blats = blatResultService.findByBioSequence( bioSequence );
                summary.setBlatResults( blats );
            }

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
