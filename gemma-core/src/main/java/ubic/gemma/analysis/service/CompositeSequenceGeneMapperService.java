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
package ubic.gemma.analysis.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @deprecated Methods here can be done other ways, or added to the CompositeSequenceService if need be.
 */
@Deprecated
@Component
public class CompositeSequenceGeneMapperService {
    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    GeneService geneService;

    /**
     * @param officialSymbols
     * @param arrayDesigns to look in
     * @return LinkedHashMap<Gene, Collection<CompositeSequence>>
     */
    public LinkedHashMap<Gene, Collection<CompositeSequence>> getGene2ProbeMapByOfficialSymbols(
            Collection<String> officialSymbols, Collection<ArrayDesign> arrayDesigns ) {

        LinkedHashMap<String, Collection<Gene>> genesMap = findGenesByOfficialSymbols( officialSymbols );

        Set<String> geneOfficialSymbolKeyset = genesMap.keySet();

        LinkedHashMap<Gene, Collection<CompositeSequence>> compositeSequencesForGeneMap = new LinkedHashMap<Gene, Collection<CompositeSequence>>();

        for ( String officialSymbol : geneOfficialSymbolKeyset ) {
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
     * Returns a map of gene collections, each keyed by a gene official symbol.
     * 
     * @param officialSymbols
     * @return LinkedHashMap
     */
    protected LinkedHashMap<String, Collection<Gene>> findGenesByOfficialSymbols( Collection<String> officialSymbols ) {

        LinkedHashMap<String, Collection<Gene>> geneMap = new LinkedHashMap<String, Collection<Gene>>();
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

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

}
