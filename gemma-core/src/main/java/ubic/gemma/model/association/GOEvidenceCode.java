

package ubic.gemma.model.association;

import java.util.*;

/**
 * This enumeration was originally based on GO, but is used for all entities that have evidenciary aspects; Thus it has
 * been expanded to include: Terms from RGD&#160;(rat genome database)
 * <ul>
 * <li>IED = Inferred from experimental data
 * <li>IAGP = Inferred from association of genotype and phenotype
 * <li>IPM = Inferred from phenotype manipulation
 * <li>QTM = Quantitative Trait Measurement
 * </ul>
 * And our own custom code IIA which means Inferred from Imported Annotation to distinguish IEAs that we ourselves have
 * computed
 *
 * See https://geneontology.org/docs/guide-go-evidence-codes/ for documentation of GO evidence codes.
 */
public enum GOEvidenceCode {
    IC,
    IDA,
    IEA,
    IEP,
    IGI,
    IMP,
    IPI,
    ISS,
    NAS,
    ND,
    RCA,
    TAS,
    NR,
    EXP,
    ISA,
    ISM,
    /**
     * Inferred from Genomic Context; This evidence code can be used whenever information about the genomic context of a
     * gene product forms part of the evidence for a particular annotation. Genomic context includes, but is not limited
     * to, such things as identity of the genes neighboring the gene product in question (i.e. synteny), operon
     * structure, and phylogenetic or other whole genome analysis. "We recommend making an entry in the with/from column
     * when using this evidence code. In cases where operon structure or synteny are the compelling evidence, include
     * identifier(s) for the neighboring genes in the with/from column. In casees where metabolic reconstruction is the
     * compelling evidence, and there is an identifier for the pathway or system, that should be entered in the
     * with/from column. When multiple entries are placed in the with/from field, they are separated by pipes."
     */
    IGC,

    ISO,
    /**
     * Added by Gemma: Inferred from Imported Annotation. To be distinguished from IEA or IC, represents annotations
     * that were present in imported data, and which have unknown evidence in the original source (though generally put
     * there manually).
     */
    IIA,
    /**
     * A type of phylogenetic evidence whereby an aspect of a descendant is inferred through the characterization of an
     * aspect of a ancestral gene.
     */
    IBA,
    /**
     * A type of phylogenetic evidence whereby an aspect of an ancestral gene is inferred through the characterization
     * of an aspect of a descendant gene.
     */
    IBD,
    /**
     * A type of phylogenetic evidence characterized by the loss of key sequence residues. Annotating with this evidence
     * codes implies a NOT annotation. This evidence code is also referred to as IMR (inferred from Missing Residues).
     */
    IKR,
    /**
     * Inferred from Rapid Divergence. A type of phylogenetic evidence characterized by rapid divergence from ancestral
     * sequence. Annotating with this evidence codes implies a NOT annotation.
     */
    IRD,
    /**
     * Inferred from Missing Residues. Represents a NOT association. IMR is a synonym of IKR.
     */
    IMR,
    /**
     * Inferred from experimental data (RGD code)
     */
    IED,
    /**
     * Inferred from association of genotype and phenotype (RGD code)
     */
    IAGP,
    /**
     * Inferred from phenotype manipulation (RGD code)
     */
    IPM,
    /**
     * Quantitative Trait Measurement (RGD code)
     */
    QTM,
    /**
     Inferred from High Throughput Direct Assay (HDA)
     */
    HDA,
    /**
     *  Inferred from High Throughput Expression Pattern (HEP)
     */
    HEP,
    /**
     *  Inferred from High Throughput Genetic Interaction (HGI)
     */
    HGI,

    /**
     * Inferred from High Throughput Mutant Phenotype (HMP)
     */
    HMP,

    /**
     *  Inferred from High Throughput Experiment (HTP)
     */
    HTP,
    /**
     * Unsupported/unknown GO evidence code are mapped to this value.
     */
    OTHER;
}