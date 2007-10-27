package ubic.gemma.analysis.linkAnalysis;

import ubic.gemma.model.coexpression.Link;

/**
 * @author paul
 * @version $Id$
 */
public class GeneLink implements Link {
    Long firstGene;
    Long secondGene;
    Double score;

    public GeneLink( Long firstGeneId, Long secondGeneId, double score ) {
        this.firstGene = firstGeneId;
        this.secondGene = secondGeneId;
        this.score = score;
    }

    @Override
    public boolean equals( Object obj ) {
        GeneLink that = ( GeneLink ) obj;
        return that.getFirstGene().equals( this.firstGene ) && that.getSecondGene().equals( this.secondGene )
                && Math.signum( this.score ) == Math.signum( that.getScore() );
    }

    public Long getFirstGene() {
        return firstGene;
    }

    public Double getScore() {
        return score;
    }

    public Long getSecondGene() {
        return secondGene;
    }

    @Override
    public int hashCode() {
        return 29 * ( int ) Math.signum( this.score ) * this.firstGene.hashCode() + this.secondGene.hashCode();
    }

    public void setFirstGene( Long firstGene ) {
        this.firstGene = firstGene;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSecondGene( Long secondGene ) {
        this.secondGene = secondGene;
    }

}