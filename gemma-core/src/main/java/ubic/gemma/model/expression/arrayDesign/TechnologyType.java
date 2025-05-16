package ubic.gemma.model.expression.arrayDesign;

import java.util.EnumSet;

public enum TechnologyType {
    /**
     * Indicates this platform uses two channels and expression measurements are ratios.
     */
    TWOCOLOR,
    /**
     * Indicates this platform can be used in either a one- or two- channel mode.
     */
    DUALMODE,
    /**
     * Indicates this platform uses one channel and measurements are non-ratiometric (e.g. Affymetrix oligo arrays)
     */
    ONECOLOR,
    /**
     * Indicate the platform is based on sequencing (e.g. Illumina short reads, SAGE, OxfordNanopore). However, once we
     * process RNA-seq data it ends up on a GENELIST platform.
     */
    SEQUENCING,

    /**
     * Indicates that this "platform" is just a list of genes (we use this for RNA-seq)
     */
    GENELIST,

    /** We don't know */
    OTHER;

    /**
     * Enumeration of microarray platforms.
     */
    public static final EnumSet<TechnologyType> MICROARRAY = EnumSet.of( ONECOLOR, TWOCOLOR, DUALMODE );
}