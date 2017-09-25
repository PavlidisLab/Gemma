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

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;
import java.util.HashSet;

/**
 * This is a convenience object to hold the results of CompositeSequence mapping results.
 *
 * @author Paul
 */
@SuppressWarnings("unused") // Possibly used in front end
public class CompositeSequenceMapSummary {

    private final CompositeSequence compositeSequence;
    private Collection<BlatResult> blatResults;
    private Collection<GeneProduct> geneProducts;
    private Collection<Gene> genes;

    public CompositeSequenceMapSummary( CompositeSequence compositeSequence ) {
        super();
        this.compositeSequence = compositeSequence;
        this.blatResults = new HashSet<>();
        this.geneProducts = new HashSet<>();
        this.genes = new HashSet<>();
    }

    public static String header() {
        return "CompSeq\tBioSeq\t#BlatRes\tGeneProds\tGenes";
    }

    public Collection<BlatResult> getBlatResults() {
        return blatResults;
    }

    public void setBlatResults( Collection<BlatResult> blatResults ) {
        this.blatResults = blatResults;
    }

    public CompositeSequence getCompositeSequence() {
        return compositeSequence;
    }

    public Collection<GeneProduct> getGeneProducts() {
        return geneProducts;
    }

    public void setGeneProducts( Collection<GeneProduct> geneProducts ) {
        this.geneProducts = geneProducts;
    }

    public Collection<Gene> getGenes() {
        return genes;
    }

    public void setGenes( Collection<Gene> genes ) {
        this.genes = genes;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( compositeSequence.getName() ).append( "\t" );

        if ( compositeSequence.getBiologicalCharacteristic() != null ) {
            buf.append( compositeSequence.getBiologicalCharacteristic().getName() ).append( "\t" );
        } else {
            buf.append( "\t" );
        }

        buf.append( blatResults.size() ).append( "\t" );

        for ( GeneProduct gp : geneProducts ) {
            buf.append( gp.getName() ).append( "|" );
        }

        buf.append( "\t" );

        for ( Gene g : genes ) {
            buf.append( g.getOfficialSymbol() ).append( "|" );
        }

        return buf.toString().replaceAll( "\\|\t", "\t" ).replaceFirst( "\\|$", "" );
    }

}
