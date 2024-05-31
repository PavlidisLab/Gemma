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
package ubic.gemma.core.analysis.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author keshav
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
@Component
public class CompositeSequenceGeneMapperService {
    private final Log log = LogFactory.getLog( this.getClass() );
    @Autowired
    private GeneService geneService;

    /**
     * @param arrayDesigns    to look in
     * @param officialSymbols official symbols
     * @return map of gene to composite sequences
     */
    public LinkedHashMap<Gene, Collection<CompositeSequence>> getGene2ProbeMapByOfficialSymbols(
            Collection<String> officialSymbols, Collection<ArrayDesign> arrayDesigns ) {

        LinkedHashMap<String, Collection<Gene>> genesMap = this.findGenesByOfficialSymbols( officialSymbols );

        Set<String> geneOfficialSymbolKeySet = genesMap.keySet();

        LinkedHashMap<Gene, Collection<CompositeSequence>> compositeSequencesForGeneMap = new LinkedHashMap<>();

        for ( String officialSymbol : geneOfficialSymbolKeySet ) {
            log.debug( "official symbol: " + officialSymbol );
            Collection<Gene> genes = genesMap.get( officialSymbol );
            for ( Gene g : genes ) {
                Collection<CompositeSequence> compositeSequences = geneService.getCompositeSequencesById( g.getId() );
                for ( CompositeSequence sequence : compositeSequences ) {
                    if ( arrayDesigns.contains( sequence.getArrayDesign() ) ) {
                        if ( compositeSequencesForGeneMap.get( g ) == null ) {
                            compositeSequencesForGeneMap.put( g, new HashSet<CompositeSequence>() );
                        }
                        compositeSequencesForGeneMap.get( g ).add( sequence );
                    }
                }
            }
        }
        return compositeSequencesForGeneMap;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * Returns a map of gene collections, each keyed by a gene official symbol.
     *
     * @param officialSymbols official symbols
     * @return map of genes
     */
    private LinkedHashMap<String, Collection<Gene>> findGenesByOfficialSymbols( Collection<String> officialSymbols ) {

        LinkedHashMap<String, Collection<Gene>> geneMap = new LinkedHashMap<>();
        for ( String officialSymbol : officialSymbols ) {
            Collection<Gene> genes = geneService.findByOfficialSymbol( officialSymbol );
            if ( genes == null || genes.isEmpty() ) {
                log.warn( "Gene with official symbol " + officialSymbol + " does not exist.  Discarding ... " );
                continue;
            }
            geneMap.put( officialSymbol, genes );
        }

        return geneMap;
    }

}
