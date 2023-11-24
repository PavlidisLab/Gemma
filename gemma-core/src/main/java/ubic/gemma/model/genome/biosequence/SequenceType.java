package ubic.gemma.model.genome.biosequence;

public enum SequenceType {
    /**
     * Represents the target sequence provided by the manufacturer.
     */
    AFFY_TARGET,
    /**
     * Represents a single probe sequence for Affymetrix reporters
     */
    AFFY_PROBE,
    EST,
    mRNA,
    REFSEQ,
    BAC,
    WHOLE_GENOME,
    WHOLE_CHROMOSOME,
    /**
     * Generic DNA sequence of any other type not representable by another value
     */
    DNA,

    OTHER,

    ORF,
    /**
     * Represents Affymetrix probe sequences that have been "collapsed" or combined into a single sequence.
     */
    AFFY_COLLAPSED,
    /**
     * Represents a (synthetic) oligonucleotide.
     */
    OLIGO,

    /**
     * A placeholder element used for annotation associations for RNA-seq
     */
    DUMMY;
}