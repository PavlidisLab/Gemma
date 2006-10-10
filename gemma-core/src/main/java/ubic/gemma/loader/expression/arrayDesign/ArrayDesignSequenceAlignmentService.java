/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat;
import ubic.gemma.apps.Blat.BlattableGenome;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Aligns sequences from array designs to the genome, using blat, and persists the blat results.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean name="arrayDesignSequenceAlignmentService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class ArrayDesignSequenceAlignmentService {

    private static Log log = LogFactory.getLog( ArrayDesignSequenceAlignmentService.class.getName() );

    BlatResultService blatResultService;

    PersisterHelper persisterHelper;

    /**
     * @param blatResultService the blatResultService to set
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param ad
     */
    public void processArrayDesign( ArrayDesign ad, Taxon taxon ) {
        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        Map<String, BioSequence> sequencesToBlat = new HashMap<String, BioSequence>();
        for ( CompositeSequence sequence : compositeSequences ) {
            BioSequence bs = sequence.getBiologicalCharacteristic();
            sequencesToBlat.put( bs.getSequenceDatabaseEntry().getAccession(), bs );
        }

        Blat blat = new Blat();
        BlattableGenome bg = BlattableGenome.MOUSE;

        if ( taxon.getCommonName().equals( "mouse" ) ) {
            bg = BlattableGenome.MOUSE;
        } else if ( taxon.getCommonName().equals( "rat" ) ) {
            bg = BlattableGenome.RAT;
        } else if ( taxon.getCommonName().equals( "human" ) ) {
            bg = BlattableGenome.HUMAN;
        }

        try {
            Map<String, Collection<BlatResult>> results = blat.blatQuery( sequencesToBlat.values(), bg );

            log.info( "Got BLAT results for " + results.keySet().size() + " query sequences" );

            for ( String key : results.keySet() ) {
                Collection<BlatResult> brs = results.get( key );
                for ( BlatResult br : brs ) {
                    String acc = br.getQuerySequence().getName();
                    assert acc != null && sequencesToBlat.containsKey( acc );

                    // FIXME: the blatter should set the taxon for us. Parser supports this.
                    br.getQuerySequence().setTaxon( taxon );
                    br.getTargetChromosome().setTaxon( taxon );
                    br.getTargetChromosome().getSequence().setTaxon( taxon );

                    br.setTargetChromosome( ( Chromosome ) persisterHelper.persist( br.getTargetChromosome() ) );
                    br.setQuerySequence( sequencesToBlat.get( acc ) );
                    br = blatResultService.create( br );
                }

            }

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
