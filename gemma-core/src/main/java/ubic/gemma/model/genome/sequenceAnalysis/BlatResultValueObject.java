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

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.genome.TaxonValueObject;

/**
 * @author paul
 */
@Getter
@Setter
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
        super();
    }

    public BlatResultValueObject( Long id ) {
        super( id );
    }

    public BlatResultValueObject( BlatResult br, Double score, Double identity ) {
        this( br.getId(), TaxonValueObject.fromEntity( br.getTargetChromosome().getTaxon() ), br.getBlockCount(),
                br.getBlockSizes(), br.getMatches(), br.getMismatches(), br.getNs(), br.getQueryEnd(),
                br.getQueryGapBases(), br.getQueryGapCount(),
                BioSequenceValueObject.fromEntity( br.getQuerySequence() ), br.getQueryStart(), br.getQueryStarts(),
                br.getRepMatches(), score, identity, br.getStrand(), br.getTargetChromosome().getName(),
                br.getSearchedDatabase().getName(), br.getTargetEnd(), br.getTargetGapBases(), br.getTargetGapCount(),
                br.getTargetStart(), br.getTargetStarts() );
    }

    public BlatResultValueObject( Long id, TaxonValueObject taxon, Integer blockCount, String blockSizes,
            Integer matches, Integer mismatches, Integer ns, Integer queryEnd, Integer queryGapBases,
            Integer queryGapCount, BioSequenceValueObject querySequence, Integer queryStart, String queryStarts,
            Integer repMatches, Double score, Double identity, String strand, String targetChromosomeName,
            String targetDatabase, Long targetEnd, Integer targetGapBases, Integer targetGapCount, Long targetStart,
            String targetStarts ) {
        super( id );
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

}
