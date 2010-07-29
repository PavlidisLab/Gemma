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

import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject; 
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;

/**
 * This is a convenience value object to hold a BlatResult and its associated gene products and genes.
 * 
 * @author jsantos
 * @author paul
 * @version $Id$
 */
public class GeneMappingSummary implements Serializable {

    private static final long serialVersionUID = 8899320580201273360L;

    private BlatResultValueObject blatResult;

    private Map<GeneProductValueObject, GeneValueObject> geneProductMap;

    private CompositeSequenceMapValueObject compositeSequenceMap;

    /*
     * These maps are maintained for javascript clients, which cannot marshal maps unless the keys are strings.
     */
    private Map<String, GeneProductValueObject> geneProductIdMap;
    private Map<String, GeneValueObject> geneProductIdGeneMap;

    private double identity = 0.0;
    private double score = 0.0;

    // this and other ids are stored as strings to keep client side happy.
    private String blatResultId;

    public GeneMappingSummary() {
        geneProductMap = new HashMap<GeneProductValueObject, GeneValueObject>();
        geneProductIdMap = new HashMap<String, GeneProductValueObject>();
        geneProductIdGeneMap = new HashMap<String, GeneValueObject>();
    }

    /**
     * @return the blatResult
     */
    public BlatResultValueObject getBlatResult() {
        return blatResult;
    }

    /**
     * @param blatResult2 the blatResult to set
     */
    public void setBlatResult( BlatResultValueObject blatResult2 ) {
        this.blatResult = blatResult2;
        if ( blatResult2.getMatches() != null ) {
            this.identity = blatResult2.getIdentity();
            this.score = blatResult2.getScore();
        }

        if ( blatResult2.getId() != null ) this.blatResultId = blatResult2.getId().toString();
    }

    /**
     * @return the geneProductMap
     */
    public Map<GeneProductValueObject, GeneValueObject> getGeneProductMap() {
        return geneProductMap;
    }

    /**
     * @param geneProductMap the geneProductMap to set
     */
    public void setGeneProductMap( Map<GeneProductValueObject, GeneValueObject> geneProductMap ) {
        this.geneProductMap = geneProductMap;
    }

    public Collection<GeneProductValueObject> getGeneProducts() {
        return this.geneProductMap.keySet();
    }

    public GeneValueObject getGene( GeneProductValueObject geneProduct ) {
        return this.geneProductMap.get( geneProduct );
    }

    /**
     * @param geneProduct
     * @param gene
     */
    public void addGene( GeneProductValueObject geneProduct, GeneValueObject gene ) {
        geneProductIdMap.put( geneProduct.getId().toString(), geneProduct );
        geneProductMap.put( geneProduct, gene );
        geneProductIdGeneMap.put( geneProduct.getId().toString(), gene );
    }

    public Map<String, GeneValueObject> getGeneProductIdGeneMap() {
        return geneProductIdGeneMap;
    }

    public Map<String, GeneProductValueObject> getGeneProductIdMap() {
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

    public CompositeSequenceMapValueObject getCompositeSequence() {
        return compositeSequenceMap;
    }

    public void setCompositeSequence( CompositeSequenceMapValueObject compositeSequenceMap ) {
        this.compositeSequenceMap = compositeSequenceMap;
    }
}
