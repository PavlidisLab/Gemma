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

import ubic.gemma.model.TaxonValueObject;

/**
 * @author paul
 * @version $Id$
 */
public class BlatResultValueObject {

    private java.lang.Integer blockCount;
    private java.lang.String blockSizes;
    private Long id;
    private Double identity;
    private java.lang.Integer matches;
    private java.lang.Integer mismatches;
    private java.lang.Integer ns;
    private java.lang.Integer queryEnd;
    private java.lang.Integer queryGapBases;
    private java.lang.Integer queryGapCount;
    private BioSequenceValueObject querySequence;
    private java.lang.Integer queryStart;
    private java.lang.String queryStarts;
    private java.lang.Integer repMatches;
    private Double score;
    private java.lang.String strand;
    private String targetChromosomeName;
    private String targetDatabase;
    private TaxonValueObject taxon;
    private java.lang.Long targetEnd;
    private java.lang.Integer targetGapBases;
    private java.lang.Integer targetGapCount;
    private java.lang.Long targetStart;
    private java.lang.String targetStarts;

    public BlatResultValueObject() {
        super();
    }

    public BlatResultValueObject( BlatResult br ) {
        this( br.getId(),
        	  TaxonValueObject.fromEntity(br.getTargetChromosome().getTaxon()),
        	  br.getBlockCount(),
        	  br.getBlockSizes(),
        	  br.getMatches(),
              br.getMismatches(),
              br.getNs(),
              br.getQueryEnd(),
              br.getQueryGapBases(),
              br.getQueryGapCount(),
              BioSequenceValueObject.fromEntity(br.getQuerySequence()),
              br.getQueryStart(),
              br.getQueryStarts(),
              br.getRepMatches(),
              br.score(),
              br.identity(),
              br.getStrand(),
              br.getTargetChromosome().getName(),
              br.getSearchedDatabase().getName(),
              br.getTargetEnd(),
              br.getTargetGapBases(),
              br.getTargetGapCount(),
              br.getTargetStart(),
              br.getTargetStarts() );
    }

    public BlatResultValueObject( Long id, TaxonValueObject taxon, Integer blockCount, String blockSizes, Integer matches,
            Integer mismatches, Integer ns, Integer queryEnd, Integer queryGapBases, Integer queryGapCount,
            BioSequenceValueObject querySequence, Integer queryStart, String queryStarts, Integer repMatches, Double score,
            Double identity, String strand, String targetChromosomeName, String targetDatabase, Long targetEnd,
            Integer targetGapBases, Integer targetGapCount, Long targetStart, String targetStarts ) {
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
     * @see java.lang.Object#equals(java.lang.Object)
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
    public java.lang.Integer getBlockCount() {
        return this.blockCount;
    }

    /**
     * 
     */
    public java.lang.String getBlockSizes() {
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
    public java.lang.Integer getMatches() {
        return this.matches;
    }

    /**
     * 
     */
    public java.lang.Integer getMismatches() {
        return this.mismatches;
    }

    /**
     * 
     */
    public java.lang.Integer getNs() {
        return this.ns;
    }

    /**
     * 
     */
    public java.lang.Integer getQueryEnd() {
        return this.queryEnd;
    }

    /**
     * 
     */
    public java.lang.Integer getQueryGapBases() {
        return this.queryGapBases;
    }

    /**
     * 
     */
    public java.lang.Integer getQueryGapCount() {
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
    public java.lang.Integer getQueryStart() {
        return this.queryStart;
    }

    /**
     * 
     */
    public java.lang.String getQueryStarts() {
        return this.queryStarts;
    }

    /**
     * 
     */
    public java.lang.Integer getRepMatches() {
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
    public java.lang.String getStrand() {
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
    public java.lang.Long getTargetEnd() {
        return this.targetEnd;
    }

    /**
     * 
     */
    public java.lang.Integer getTargetGapBases() {
        return this.targetGapBases;
    }

    /**
     * 
     */
    public java.lang.Integer getTargetGapCount() {
        return this.targetGapCount;
    }

    /**
     * 
     */
    public java.lang.Long getTargetStart() {
        return this.targetStart;
    }

    /**
     * 
     */
    public java.lang.String getTargetStarts() {
        return this.targetStarts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setBlockCount( java.lang.Integer blockCount ) {
        this.blockCount = blockCount;
    }

    public void setBlockSizes( java.lang.String blockSizes ) {
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

    public void setMatches( java.lang.Integer matches ) {
        this.matches = matches;
    }

    public void setMismatches( java.lang.Integer mismatches ) {
        this.mismatches = mismatches;
    }

    public void setNs( java.lang.Integer ns ) {
        this.ns = ns;
    }

    public void setQueryEnd( java.lang.Integer queryEnd ) {
        this.queryEnd = queryEnd;
    }

    public void setQueryGapBases( java.lang.Integer queryGapBases ) {
        this.queryGapBases = queryGapBases;
    }

    public void setQueryGapCount( java.lang.Integer queryGapCount ) {
        this.queryGapCount = queryGapCount;
    }

    /**
     * @param querySequence the querySequence to set
     */
    public void setQuerySequence( BioSequenceValueObject querySequence ) {
        this.querySequence = querySequence;
    }

    public void setQueryStart( java.lang.Integer queryStart ) {
        this.queryStart = queryStart;
    }

    public void setQueryStarts( java.lang.String queryStarts ) {
        this.queryStarts = queryStarts;
    }

    public void setRepMatches( java.lang.Integer repMatches ) {
        this.repMatches = repMatches;
    }

    /**
     * @param score the score to set
     */
    public void setScore( Double score ) {
        this.score = score;
    }

    public void setStrand( java.lang.String strand ) {
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

    public void setTargetEnd( java.lang.Long targetEnd ) {
        this.targetEnd = targetEnd;
    }

    public void setTargetGapBases( java.lang.Integer targetGapBases ) {
        this.targetGapBases = targetGapBases;
    }

    public void setTargetGapCount( java.lang.Integer targetGapCount ) {
        this.targetGapCount = targetGapCount;
    }

    public void setTargetStart( java.lang.Long targetStart ) {
        this.targetStart = targetStart;
    }

    public void setTargetStarts( java.lang.String targetStarts ) {
        this.targetStarts = targetStarts;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

    /**
     * @return the taxon
     */
    public TaxonValueObject getTaxon() {
        return taxon;
    }

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
    
}
