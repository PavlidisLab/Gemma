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
package ubic.gemma.genome;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean name="compositeSequenceGeneMapperService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="geneDao" ref="geneDao"
 */
public class CompositeSequenceGeneMapperService {
    private Log log = LogFactory.getLog( this.getClass() );

    GeneService geneService = null;

    CompositeSequenceService compositeSequenceService = null;

    GeneDao geneDao = null;

    /**
     * @param officialSymbols
     * @return LinkedHashMap<Gene, Collection<CompositeSequence>>
     */
    public LinkedHashMap<Gene, Collection<CompositeSequence>> getCompositeSequencesForGenesByOfficialSymbols(
            Collection<String> officialSymbols ) {

        LinkedHashMap<String, Collection<Gene>> genesMap = findGenesByOfficialSymbols( officialSymbols );

        LinkedHashSet<String> geneOfficialSymbolKeyset = new LinkedHashSet<String>();

        LinkedHashMap<Gene, Collection<CompositeSequence>> compositeSequencesForGeneMap = new LinkedHashMap<Gene, Collection<CompositeSequence>>();

        for ( String officialSymbol : geneOfficialSymbolKeyset ) {
            log.debug( "official symbol: " + officialSymbol );
            Collection<Gene> genes = genesMap.get( officialSymbol );
            for ( Gene g : genes ) {
                Collection<CompositeSequence> compositeSequences = this.getCompositeSequencesByGeneId( g.getId() );
                compositeSequencesForGeneMap.put( g, compositeSequences );
            }
        }
        return compositeSequencesForGeneMap;
    }

    /**
     * Returns a map of gene collections, each keyed by a gene official symbol.
     * 
     * @param officialSymbols
     * @return LinkedHashMap
     */
    @SuppressWarnings("unchecked")
    public LinkedHashMap<String, Collection<Gene>> findGenesByOfficialSymbols( Collection<String> officialSymbols ) {

        LinkedHashMap<String, Collection<Gene>> geneMap = new LinkedHashMap<String, Collection<Gene>>();
        for ( String officialSymbol : officialSymbols ) {
            Collection<Gene> genes = geneService.findByOfficialSymbol( officialSymbol );
            if ( genes == null || genes.isEmpty() ) {
                log
                        .warn( "Discarding genes with official symbol " + officialSymbol
                                + " do not exist.  Discarding ... " );
                continue;
            }
            geneMap.put( officialSymbol, genes );
        }

        return geneMap;
    }

    /**
     * @param compositeSequence
     * @return Collection<Gene>
     */
    public Collection<Gene> getGenesForCompositeSequence( CompositeSequence compositeSequence ) {
        Collection<Gene> genes = null;

        if ( compositeSequence.getBiologicalCharacteristic() != null ) {
            genes = new HashSet<Gene>();
            for ( BioSequence2GeneProduct bs2gp : compositeSequence.getBiologicalCharacteristic()
                    .getBioSequence2GeneProduct() ) {
                if ( bs2gp != null ) {
                    GeneProduct geneProduct = bs2gp.getGeneProduct();
                    if ( geneProduct != null ) genes.add( geneProduct.getGene() );
                }
            }
        }
        return genes;
    }

    /**
     * @param id
     * @return Collection<CompositeSequence>
     */
    public Collection<CompositeSequence> getCompositeSequencesByGeneId( long id ) {
        // TODO change name to getCompositeSequenceByGene(Gene gene)
        return this.geneDao.getCompositeSequencesById( id );
    }

    /**
     * @param id
     * @return long
     * @throws Exception
     */
    public long getCompositeSequenceCountByGeneId( long id ) {
        return this.geneDao.getCompositeSequenceCountById( id );
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param compositeSequenceService The compositeSequenceService to set.
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

}
