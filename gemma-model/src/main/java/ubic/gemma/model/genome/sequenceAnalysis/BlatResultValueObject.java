/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.TaxonValueObject;

/**
 * @author paul
 * @version $Id$
 */
public class BlatResultValueObject {

    /**
     * @param blatResults
     * @return
     */
    public static Collection<BlatResultValueObject> convert2ValueObjects( Collection<BlatResult> blatResults ) {
        Collection<BlatResultValueObject> result = new HashSet<BlatResultValueObject>();
        for ( BlatResult br : blatResults ) {
            result.add( new BlatResultValueObject( br ) );

        }
        return result;
    }

    private Integer blockCount;
    private String blockSizes;
    private Long id;
    private Double identity;
    private Integer matches;
    private Integer mismatches;
    private Integer ns;
    private Integer queryEnd;
    private Integer queryGapBases;
    private Integer queryGapCount;
    private BioSequenceValueObject querySequence;
    private Integer queryStart;
    private String queryStarts;
    private Integer repMatches;
    private Double score;
    private String strand;
    private String targetChromosomeName;
    private String targetDatabase;
    private TaxonValueObject taxon;
    private Long targetEnd;
    private Integer targetGapBases;
    private Integer targetGapCount;
    private Long targetStart;

    private String targetStarts;

    public BlatResultValueObject() {
        super();
    }

    public BlatResultValueObject( BlatResult br ) {
        this( br.getId(), TaxonValueObject.fromEntity( br.getTargetChromosome().getTaxon() ), br.getBlockCount(), br
                .getBlockSizes(), br.getMatches(), br.getMismatches(), br.getNs(), br.getQueryEnd(), br
                .getQueryGapBases(), br.getQueryGapCount(), BioSequenceValueObject.fromEntity( br.getQuerySequence() ),
                br.getQueryStart(), br.getQueryStarts(), br.getRepMatches(), br.score(), br.identity(), br.getStrand(),
                br.getTargetChromosome().getName(), br.getSearchedDatabase().getName(), br.getTargetEnd(), br
                        .getTargetGapBases(), br.getTargetGapCount(), br.getTargetStart(), br.getTargetStarts() );
    }

    public BlatResultValueObject( Long id, TaxonValueObject taxon, Integer blockCount, String blockSizes,
            Integer matches, Integer mismatches, Integer ns, Integer queryEnd, Integer queryGapBases,
            Integer queryGapCount, BioSequenceValueObject querySequence, Integer queryStart, String queryStarts,
            Integer repMatches, Double score, Double identity, String strand, String targetChromosomeName,
            String targetDatabase, Long targetEnd, Integer targetGapBases, Integer targetGapCount, Long targetStart,
            String targetStarts ) {
        super();
        this.id = id;
        this.setTaxon( taxon );
        this.blockCount = blockCount;
        this.blockSizes = blockSizes;
        this.matches = matches;
        this.mismatches = mismatches;
        this.ns = ns;
        this.queryEnd = queryEnd;
        this.queryGapBases = queryGapBases;
        this.queryGapCount = queryGapCount;
        this.querySequence = querySequence;
        this.queryStart = queryStart;
        this.queryStarts = queryStarts;
        this.repMatches = repMatches;
        this.score = score;
        this.identity = identity;
        this.strand = strand;
        this.targetChromosomeName = targetChromosomeName;
        this.targetDatabase = targetDatabase;
        this.targetEnd = targetEnd;
        this.targetGapBases = targetGapBases;
        this.targetGapCount = targetGapCount;
        this.targetStart = targetStart;
        this.targetStarts = targetStarts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        BlatResultValueObject other = ( BlatResultValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    /**
     * 
     */
    public Integer getBlockCount() {
        return this.blockCount;
    }

    /**
     * 
     */
    public String getBlockSizes() {
        return this.blockSizes;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the identity
     */
    public Double getIdentity() {
        return identity;
    }

    /**
     * 
     */
    public Integer getMatches() {
        return this.matches;
    }

    /**
     * 
     */
    public Integer getMismatches() {
        return this.mismatches;
    }

    /**
     * 
     */
    public Integer getNs() {
        return this.ns;
    }

    /**
     * 
     */
    public Integer getQueryEnd() {
        return this.queryEnd;
    }

    /**
     * 
     */
    public Integer getQueryGapBases() {
        return this.queryGapBases;
    }

    /**
     * 
     */
    public Integer getQueryGapCount() {
        return this.queryGapCount;
    }

    /**
     * @return the querySequence
     */
    public BioSequenceValueObject getQuerySequence() {
        return querySequence;
    }

    /**
     * 
     */
    public Integer getQueryStart() {
        return this.queryStart;
    }

    /**
     * 
     */
    public String getQueryStarts() {
        return this.queryStarts;
    }

    /**
     * 
     */
    public Integer getRepMatches() {
        return this.repMatches;
    }

    /**
     * @return the score
     */
    public Double getScore() {
        return score;
    }

    /**
     * 
     */
    public String getStrand() {
        return this.strand;
    }

    /**
     * @return the targetChromosomeName
     */
    public String getTargetChromosomeName() {
        return targetChromosomeName;
    }

    /**
     * @return the targetDatabase
     */
    public String getTargetDatabase() {
        return targetDatabase;
    }

    /**
     * 
     */
    public Long getTargetEnd() {
        return this.targetEnd;
    }

    /**
     * 
     */
    public Integer getTargetGapBases() {
        return this.targetGapBases;
    }

    /**
     * 
     */
    public Integer getTargetGapCount() {
        return this.targetGapCount;
    }

    /**
     * 
     */
    public Long getTargetStart() {
        return this.targetStart;
    }

    /**
     * 
     */
    public String getTargetStarts() {
        return this.targetStarts;
    }

    /**
     * @return the taxon
     */
    public TaxonValueObject getTaxon() {
        return taxon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setBlockCount( Integer blockCount ) {
        this.blockCount = blockCount;
    }

    public void setBlockSizes( String blockSizes ) {
        this.blockSizes = blockSizes;
    }

    /**
     * @param id the id to set
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @param identity the identity to set
     */
    public void setIdentity( Double identity ) {
        this.identity = identity;
    }

    public void setMatches( Integer matches ) {
        this.matches = matches;
    }

    public void setMismatches( Integer mismatches ) {
        this.mismatches = mismatches;
    }

    public void setNs( Integer ns ) {
        this.ns = ns;
    }

    public void setQueryEnd( Integer queryEnd ) {
        this.queryEnd = queryEnd;
    }

    public void setQueryGapBases( Integer queryGapBases ) {
        this.queryGapBases = queryGapBases;
    }

    public void setQueryGapCount( Integer queryGapCount ) {
        this.queryGapCount = queryGapCount;
    }

    /**
     * @param querySequence the querySequence to set
     */
    public void setQuerySequence( BioSequenceValueObject querySequence ) {
        this.querySequence = querySequence;
    }

    public void setQueryStart( Integer queryStart ) {
        this.queryStart = queryStart;
    }

    public void setQueryStarts( String queryStarts ) {
        this.queryStarts = queryStarts;
    }

    public void setRepMatches( Integer repMatches ) {
        this.repMatches = repMatches;
    }

    /**
     * @param score the score to set
     */
    public void setScore( Double score ) {
        this.score = score;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    /**
     * @param targetChromosomeName the targetChromosomeName to set
     */
    public void setTargetChromosomeName( String targetChromosomeName ) {
        this.targetChromosomeName = targetChromosomeName;
    }

    /**
     * @param targetDatabase the targetDatabase to set
     */
    public void setTargetDatabase( String targetDatabase ) {
        this.targetDatabase = targetDatabase;
    }

    public void setTargetEnd( Long targetEnd ) {
        this.targetEnd = targetEnd;
    }

    public void setTargetGapBases( Integer targetGapBases ) {
        this.targetGapBases = targetGapBases;
    }

    public void setTargetGapCount( Integer targetGapCount ) {
        this.targetGapCount = targetGapCount;
    }

    public void setTargetStart( Long targetStart ) {
        this.targetStart = targetStart;
    }

    public void setTargetStarts( String targetStarts ) {
        this.targetStarts = targetStarts;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

}
