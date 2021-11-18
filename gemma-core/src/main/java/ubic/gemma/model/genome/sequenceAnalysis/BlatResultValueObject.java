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

import org.hibernate.Hibernate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.genome.TaxonValueObject;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BlatResultValueObject extends IdentifiableValueObject<BlatResult> {

    private Integer blockCount;
    private String blockSizes;
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

    /**
     * Required when using the class as a spring bean.
     */
    public BlatResultValueObject() {
    }

    public BlatResultValueObject( Long id ) {
        super( id );
    }

    public BlatResultValueObject( BlatResult br ) {
        super( br.getId() );
        this.setTaxon( TaxonValueObject.fromEntity( br.getTargetChromosome().getTaxon() ) );
        this.blockCount = br.getBlockCount();
        this.blockSizes = br.getBlockSizes();
        this.matches = br.getMatches();
        this.mismatches = br.getMismatches();
        this.ns = br.getNs();
        this.queryEnd = br.getQueryEnd();
        this.queryGapBases = br.getQueryGapBases();
        this.queryGapCount = br.getQueryGapCount();
        this.querySequence = BioSequenceValueObject.fromEntity( br.getQuerySequence() );
        this.queryStart = br.getQueryStart();
        this.queryStarts = br.getQueryStarts();
        this.repMatches = br.getRepMatches();
        this.score = br.score();
        this.identity = br.identity();
        this.strand = br.getStrand();
        this.targetChromosomeName = br.getTargetChromosome().getName();
        // FIXME: this is supposed to be initialized in BlatResultDaoImpl#thaw(Collection), but it's not always the case
        if ( Hibernate.isInitialized( br.getSearchedDatabase() ) ) {
            this.targetDatabase = br.getSearchedDatabase().getName();
        }
        this.targetEnd = br.getTargetEnd();
        this.targetGapBases = br.getTargetGapBases();
        this.targetGapCount = br.getTargetGapCount();
        this.targetStart = br.getTargetStart();
        this.targetStarts = br.getTargetStarts();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        BlatResultValueObject other = ( BlatResultValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public Integer getBlockCount() {
        return this.blockCount;
    }

    public void setBlockCount( Integer blockCount ) {
        this.blockCount = blockCount;
    }

    public String getBlockSizes() {
        return this.blockSizes;
    }

    public void setBlockSizes( String blockSizes ) {
        this.blockSizes = blockSizes;
    }

    public Double getIdentity() {
        return identity;
    }

    /**
     * @param identity the identity to set (0-1)
     */
    public void setIdentity( Double identity ) {
        this.identity = identity;
    }

    public Integer getMatches() {
        return this.matches;
    }

    public void setMatches( Integer matches ) {
        this.matches = matches;
    }

    public Integer getMismatches() {
        return this.mismatches;
    }

    public void setMismatches( Integer mismatches ) {
        this.mismatches = mismatches;
    }

    public Integer getNs() {
        return this.ns;
    }

    public void setNs( Integer ns ) {
        this.ns = ns;
    }

    public Integer getQueryEnd() {
        return this.queryEnd;
    }

    public void setQueryEnd( Integer queryEnd ) {
        this.queryEnd = queryEnd;
    }

    public Integer getQueryGapBases() {
        return this.queryGapBases;
    }

    public void setQueryGapBases( Integer queryGapBases ) {
        this.queryGapBases = queryGapBases;
    }

    public Integer getQueryGapCount() {
        return this.queryGapCount;
    }

    public void setQueryGapCount( Integer queryGapCount ) {
        this.queryGapCount = queryGapCount;
    }

    /**
     * @return the querySequence
     */
    public BioSequenceValueObject getQuerySequence() {
        return querySequence;
    }

    /**
     * @param querySequence the querySequence to set
     */
    public void setQuerySequence( BioSequenceValueObject querySequence ) {
        this.querySequence = querySequence;
    }

    public Integer getQueryStart() {
        return this.queryStart;
    }

    public void setQueryStart( Integer queryStart ) {
        this.queryStart = queryStart;
    }

    public String getQueryStarts() {
        return this.queryStarts;
    }

    public void setQueryStarts( String queryStarts ) {
        this.queryStarts = queryStarts;
    }

    public Integer getRepMatches() {
        return this.repMatches;
    }

    public void setRepMatches( Integer repMatches ) {
        this.repMatches = repMatches;
    }

    public Double getScore() {
        return score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public String getStrand() {
        return this.strand;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public String getTargetChromosomeName() {
        return targetChromosomeName;
    }

    public void setTargetChromosomeName( String targetChromosomeName ) {
        this.targetChromosomeName = targetChromosomeName;
    }

    public String getTargetDatabase() {
        return targetDatabase;
    }

    public void setTargetDatabase( String targetDatabase ) {
        this.targetDatabase = targetDatabase;
    }

    public Long getTargetEnd() {
        return this.targetEnd;
    }

    public void setTargetEnd( Long targetEnd ) {
        this.targetEnd = targetEnd;
    }

    public Integer getTargetGapBases() {
        return this.targetGapBases;
    }

    public void setTargetGapBases( Integer targetGapBases ) {
        this.targetGapBases = targetGapBases;
    }

    public Integer getTargetGapCount() {
        return this.targetGapCount;
    }

    public void setTargetGapCount( Integer targetGapCount ) {
        this.targetGapCount = targetGapCount;
    }

    public Long getTargetStart() {
        return this.targetStart;
    }

    public void setTargetStart( Long targetStart ) {
        this.targetStart = targetStart;
    }

    public String getTargetStarts() {
        return this.targetStarts;
    }

    public void setTargetStarts( String targetStarts ) {
        this.targetStarts = targetStarts;
    }

    public TaxonValueObject getTaxon() {
        return taxon;
    }

    public void setTaxon( TaxonValueObject taxon ) {
        this.taxon = taxon;
    }

}
