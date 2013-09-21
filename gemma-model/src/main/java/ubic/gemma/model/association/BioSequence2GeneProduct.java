/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association;

/**
 * An association between a BioSequence and a Gene Product. This class is abstract and is variously subclassed with
 * BlatAssocation in order to capture the scores and other parameters that document why we think there is a connection
 * between a given sequence and a gene product.
 */
public abstract class BioSequence2GeneProduct extends ubic.gemma.model.association.Relationship {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2445048138750980972L;

    private Integer overlap;
    private Double score;

    private Long threePrimeDistance;

    private ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod threePrimeDistanceMeasurementMethod;

    private Double specificity;

    private ubic.gemma.model.genome.biosequence.BioSequence bioSequence;

    private ubic.gemma.model.genome.gene.GeneProduct geneProduct;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BioSequence2GeneProduct() {
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getBioSequence() {
        return this.bioSequence;
    }

    /**
     * A collection of GeneProducts that this BioSequence2GeneProduct corresponds to. A BioSequence can align to one or
     * more GeneProducts.
     */
    public ubic.gemma.model.genome.gene.GeneProduct getGeneProduct() {
        return this.geneProduct;
    }

    /**
     * Degree to which the sequence overlaps with the gene product. This is often the overlap of a DNA sequence with the
     * exons encoding the mRNA for the GeneProduct, but could have other interpretations
     */
    public Integer getOverlap() {
        return this.overlap;
    }

    /**
     * The score for the association between the biosequence and the gene product. This could be a BLAT similarity or
     * other score.
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * A measure of how specific this association is compared to others that were obtained in the same analysis. This
     * can be misleading if the same sequence was analyzed multiple times with different algorithms, databases, or
     * parameters. High values are "better" but the exactly interpretation is implementation-specific.
     */
    public Double getSpecificity() {
        return this.specificity;
    }

    /**
     * The distance from the 3' end where this BioSequence aligns with respect to the Gene Product. This is often the
     * location of the alignment with respect to an mRNA 3' end.
     */
    public Long getThreePrimeDistance() {
        return this.threePrimeDistance;
    }

    /**
     * Specifies the method used to measure the distance from the threePrimeEnd.
     */
    public ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod getThreePrimeDistanceMeasurementMethod() {
        return this.threePrimeDistanceMeasurementMethod;
    }

    public void setBioSequence( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        this.bioSequence = bioSequence;
    }

    public void setGeneProduct( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        this.geneProduct = geneProduct;
    }

    public void setOverlap( Integer overlap ) {
        this.overlap = overlap;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSpecificity( Double specificity ) {
        this.specificity = specificity;
    }

    public void setThreePrimeDistance( Long threePrimeDistance ) {
        this.threePrimeDistance = threePrimeDistance;
    }

    public void setThreePrimeDistanceMeasurementMethod(
            ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod threePrimeDistanceMeasurementMethod ) {
        this.threePrimeDistanceMeasurementMethod = threePrimeDistanceMeasurementMethod;
    }

}