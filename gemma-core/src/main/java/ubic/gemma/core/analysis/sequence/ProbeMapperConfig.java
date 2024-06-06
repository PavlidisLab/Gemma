/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.core.analysis.sequence;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Holds parameters for how mapping should be done.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use, consistency
public class ProbeMapperConfig {

    public static final boolean DEFAULT_TRIM_NONCANONICAL_CHROMOSOMES = true;

    public static final boolean DEFAULT_ALLOW_PARS = false;

    public static final boolean DEFAULT_ALLOW_PREDICTED = false;

    public static final int MAX_WARNINGS = 100;
    /**
     * Sequence identity below which we throw hits away (expressed as a fraction)
     */
    public static final double DEFAULT_IDENTITY_THRESHOLD = 0.8;
    /**
     * Fraction of bases which must overlap with an annotated exon. This should probably be higher than zero, to avoid
     * "pure intron" hits, but setting it too high can cause loss of sensitivity.
     */
    public static final double DEFAULT_MINIMUM_EXON_OVERLAP_FRACTION = 0.05;
    /**
     * BLAT score threshold below which we do not consider hits. This reflects the fraction of aligned bases.
     *
     * @see Blat for the use of a similar parameter, used to determine the retention of raw Blat results.
     * @see BlatResult for how the score is computed.
     */
    public static final double DEFAULT_SCORE_THRESHOLD = 0.75;
    /**
     * Sequences which hybridize to this many or more sites in the genome are candidates to be considered non-specific.
     * This is used even if the sequence does not contain a repeat.
     */
    public static final int NON_REPEAT_NON_SPECIFIC_SITE_THRESHOLD = 10;
    /**
     * Sequences which hybridize to this many or more sites in the genome are candidates to be considered non-specific.
     * This is used in combination with the REPEAT_FRACTION_MAXIMUM. Note that many sequences which contain repeats
     * nonetheless only align to very few sites in the genome. Similarly, there are sequences that map to multiple sites
     * which are _not_ repeats. This value is also not designed to care about whether the alignments are in known genes
     * or not. Thus setting this too low could result in over-stringent filtering.
     */
    public static final int NON_SPECIFIC_SITE_THRESHOLD = 3;
    /**
     * Sequences which have more than this fraction accounted for by repeats (via repeatmasker) will not be examined if
     * they produce multiple alignments to the genome, regardless of the alignment quality.
     */
    public static final double REPEAT_FRACTION_MAXIMUM = 0.3;
    private int warnings = 0;

    /**
     * Whether "non-canonical" chromsomes such as 6_cox_hap2 should be omitted from the results if there is a mapping to
     * a canonical one. If true, we trim them; If false, we don't do anything.
     */
    private boolean trimNonCanonicalChromosomeHits = ProbeMapperConfig.DEFAULT_TRIM_NONCANONICAL_CHROMOSOMES;
    /**
     * Allow predicted genes; setting this to false overrides the effect of useAcembly, useNscan and useEnsembl.
     */
    private boolean allowPredictedGenes = ProbeMapperConfig.DEFAULT_ALLOW_PREDICTED;

    /**
     * Limit below which BLAT results are ignored. If BLAT was run with a threshold higher than this, it won't have any
     * effect.
     */
    private double blatScoreThreshold = ProbeMapperConfig.DEFAULT_SCORE_THRESHOLD;
    private double identityThreshold = ProbeMapperConfig.DEFAULT_IDENTITY_THRESHOLD;
    private double maximumRepeatFraction = ProbeMapperConfig.REPEAT_FRACTION_MAXIMUM;
    private double minimumExonOverlapFraction = ProbeMapperConfig.DEFAULT_MINIMUM_EXON_OVERLAP_FRACTION;
    private double nonRepeatNonSpecificSiteCountThreshold = ProbeMapperConfig.NON_REPEAT_NON_SPECIFIC_SITE_THRESHOLD;
    private double nonSpecificSiteCountThreshold = ProbeMapperConfig.NON_SPECIFIC_SITE_THRESHOLD;
    private boolean useEnsembl = false;
    private boolean useEsts = false;
    private boolean useKnownGene = true; // as of rn6 and rn7, cannot be used for rat genome
    private boolean useMiRNA = false; // just confuses things.
    private boolean useMrnas = false; // doesn't add much.
    private boolean useRefGene = true;

    /**
     * @return the blatScoreThreshold
     */
    public double getBlatScoreThreshold() {
        return blatScoreThreshold;
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

    /**
     * @return the identityThreshold
     */
    public double getIdentityThreshold() {
        return identityThreshold;
    }

    /**
     * @param identityThreshold the identityThreshold to set
     */
    public void setIdentityThreshold( double identityThreshold ) {
        this.identityThreshold = identityThreshold;
    }

    /**
     * @return the maximumRepeatFraction
     */
    public double getMaximumRepeatFraction() {
        return maximumRepeatFraction;
    }

    /**
     * @param maximumRepeatFraction the maximumRepeatFraction to set
     */
    public void setMaximumRepeatFraction( double maximumRepeatFraction ) {
        this.maximumRepeatFraction = maximumRepeatFraction;
    }

    public double getMinimumExonOverlapFraction() {
        return minimumExonOverlapFraction;
    }

    public void setMinimumExonOverlapFraction( double minimumExonOverlapFraction ) {
        this.minimumExonOverlapFraction = minimumExonOverlapFraction;
    }

    public double getNonRepeatNonSpecificSiteCountThreshold() {
        return nonRepeatNonSpecificSiteCountThreshold;
    }

    public void setNonRepeatNonSpecificSiteCountThreshold( double nonRepeatNonSpecificSiteCountThreshold ) {
        this.nonRepeatNonSpecificSiteCountThreshold = nonRepeatNonSpecificSiteCountThreshold;
    }

    /**
     * @return the nonSpecificSiteCountThreshold
     */
    public double getNonSpecificSiteCountThreshold() {
        return nonSpecificSiteCountThreshold;
    }

    /**
     * @param nonSpecificSiteCountThreshold the nonSpecificSiteCountThreshold to set
     */
    public void setNonSpecificSiteCountThreshold( double nonSpecificSiteCountThreshold ) {
        this.nonSpecificSiteCountThreshold = nonSpecificSiteCountThreshold;
    }

    public boolean isAllowPredictedGenes() {
        return allowPredictedGenes;
    }

    public void setAllowPredictedGenes( boolean allowPredictedGenes ) {
        this.allowPredictedGenes = allowPredictedGenes;
    }

    public boolean isTrimNonCanonicalChromosomehits() {
        return trimNonCanonicalChromosomeHits;
    }

    /**
     * @return the useEnsembl
     */
    public boolean isUseEnsembl() {
        return useEnsembl;
    }

    /**
     * @param useEnsembl the useEnsembl to set
     */
    public void setUseEnsembl( boolean useEnsembl ) {
        this.useEnsembl = useEnsembl;
    }

    /**
     * @return the useEsts
     */
    public boolean isUseEsts() {
        return useEsts;
    }

    /**
     * @param useEsts the useEsts to set
     */
    public void setUseEsts( boolean useEsts ) {
        this.useEsts = useEsts;
    }

    /**
     * @return the useKnownGene
     */
    public boolean isUseKnownGene() {
        return useKnownGene;
    }

    /**
     * @param useKnownGene the useKnownGene to set
     */
    public void setUseKnownGene( boolean useKnownGene ) {
        this.useKnownGene = useKnownGene;
    }

    /**
     * @return the useMiRNA
     */
    public boolean isUseMiRNA() {
        return useMiRNA;
    }

    /**
     * @param useMiRNA the useMiRNA to set
     */
    public void setUseMiRNA( boolean useMiRNA ) {
        this.useMiRNA = useMiRNA;
    }

    /**
     * @return the useMrnas
     */
    public boolean isUseMrnas() {
        return useMrnas;
    }

    /**
     * @param useMrnas the useMrnas to set
     */
    public void setUseMrnas( boolean useMrnas ) {
        this.useMrnas = useMrnas;
    }

    /**
     * @return the useRefGene
     */
    public boolean isUseRefGene() {
        return useRefGene;
    }

    /**
     * @param useRefGene the useRefGene to set
     */
    public void setUseRefGene( boolean useRefGene ) {
        this.useRefGene = useRefGene;
    }

    /**
     * Set to use no tracks. Obviously then nothing will be found, so it is wise to then switch some tracks on.
     */
    public void setAllTracksOff() {
        this.setUseEsts( false );
        this.setUseMrnas( false );
        this.setUseMiRNA( false );
        this.setUseEnsembl( false );
        this.setUseRefGene( false );
        this.setUseKnownGene( false );
    }

    /**
     * Set to use all tracks, including ESTs
     */
    public void setAllTracksOn() {
        this.setUseEsts( true );
        this.setUseMrnas( true );
        this.setUseMiRNA( true );
        this.setUseEnsembl( true );
        this.setUseRefGene( true );
        this.setUseKnownGene( true );
    }

    public void setTrimNonCanonicalChromosomeHits( boolean trimNonCanonicalChromosomeHits ) {
        this.trimNonCanonicalChromosomeHits = trimNonCanonicalChromosomeHits;
    }

    @Override
    public String toString() {
        return "# Configuration:\n# blatScoreThreshold=" + this.blatScoreThreshold + "\n# identityThreshold="
                + this.identityThreshold + "\n# maximumRepeatFraction=" + this.maximumRepeatFraction
                + "\n# nonSpecificSiteCountThreshold=" + this.nonSpecificSiteCountThreshold
                + "\n# nonRepeatNonSpecificSiteCountThreshold=" + this.nonRepeatNonSpecificSiteCountThreshold
                + "\n# minimumExonOverlapFraction=" + this.minimumExonOverlapFraction + "\n# useRefGene="
                + this.useRefGene + "\n# useEnsembl=" + this.useEnsembl + "\n# useMrnas=" + this.useMrnas + "\n# useMiRNA="
                + this.useMiRNA + "\n# useEsts=" + this.useEsts + "\n# useKnownGene=" + this.useKnownGene
                + "\n";

    }

    protected int getWarnings() {
        return warnings;
    }

    protected void incrementWarnings() {
        this.warnings++;
    }

}
