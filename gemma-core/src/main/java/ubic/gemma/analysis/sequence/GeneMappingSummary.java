/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * This is a convenience value object to hold a BlatResult and its associated gene products and genes.
 * 
 * @author jsantos
 * @author paul
 * @version $Id$
 */
public class GeneMappingSummary implements Serializable {

    private static final long serialVersionUID = 8899320580201273360L;

    private BlatResult blatResult;

    private Map<GeneProduct, Gene> geneProductMap;

    // this is a bit of a hack - we need this information when displaying the blat results for a probe. Might need name
    // etc. too.
    private CompositeSequence compositeSequence;

    /*
     * These maps are maintained for javascript clients, which cannot marshal maps unless the keys are strings.
     */
    private Map<String, GeneProduct> geneProductIdMap;
    private Map<String, Gene> geneProductIdGeneMap;

    private double identity = 0.0;
    private double score = 0.0;

    // this and other ids are stored as strings to keep client side happy.
    private String blatResultId;

    public GeneMappingSummary() {
        geneProductMap = new HashMap<GeneProduct, Gene>();
        geneProductIdMap = new HashMap<String, GeneProduct>();
        geneProductIdGeneMap = new HashMap<String, Gene>();
    }

    /**
     * @return the blatResult
     */
    public BlatResult getBlatResult() {
        return blatResult;
    }

    /**
     * @param blatResult the blatResult to set
     */
    public void setBlatResult( BlatResult blatResult ) {
        this.blatResult = blatResult;
        if ( blatResult.getMatches() != null ) {
            this.identity = blatResult.identity();
            this.score = blatResult.score();
        }

        if ( blatResult.getId() != null ) this.blatResultId = blatResult.getId().toString();
    }

    /**
     * @return the geneProductMap
     */
    public Map<GeneProduct, Gene> getGeneProductMap() {
        return geneProductMap;
    }

    /**
     * @param geneProductMap the geneProductMap to set
     */
    public void setGeneProductMap( Map<GeneProduct, Gene> geneProductMap ) {
        this.geneProductMap = geneProductMap;
    }

    public Collection<GeneProduct> getGeneProducts() {
        return this.geneProductMap.keySet();
    }

    public Gene getGene( GeneProduct geneProduct ) {
        return this.geneProductMap.get( geneProduct );
    }

    /**
     * @param geneProduct
     * @param gene
     */
    public void addGene( GeneProduct geneProduct, Gene gene ) {
        geneProductIdMap.put( geneProduct.getId().toString(), geneProduct );
        geneProductMap.put( geneProduct, gene );
        geneProductIdGeneMap.put( geneProduct.getId().toString(), gene );
    }

    public Map<String, Gene> getGeneProductIdGeneMap() {
        return geneProductIdGeneMap;
    }

    public Map<String, GeneProduct> getGeneProductIdMap() {
        return geneProductIdMap;
    }

    public double getIdentity() {
        return identity;
    }

    public double getScore() {
        return score;
    }

    public String getBlatResultId() {
        return blatResultId;
    }

    public CompositeSequence getCompositeSequence() {
        return compositeSequence;
    }

    public void setCompositeSequence( CompositeSequence compositeSequence ) {
        this.compositeSequence = compositeSequence;
    }
}
