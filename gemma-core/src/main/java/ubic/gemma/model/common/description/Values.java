package ubic.gemma.model.common.description;

/**
 * Commonly used values across Gemma.
 *
 * @author poirigui
 * @see Value
 * @see Categories
 */
public class Values {

    // from OBI
    public static final Value SINGLE_NUCLEUS_RNA_SEQUENCING_ASSAY = new Value(
            "single-nucleus RNA sequencing assay", "http://purl.obolibrary.org/obo/OBI_0003109" );
    public static final Value SINGLE_CELL_RNA_SEQUENCING_ASSAY = new Value(
            "single-cell RNA sequencing assay", "http://purl.obolibrary.org/obo/OBI_0002631" );

    // from EFO
    /**
     * TODO: find a replacement for this from OBI
     */
    public static final Value RNASEQ_OF_CODING_RNA_FROM_SINGLE_CELLS = new Value(
            "RNA-seq of coding RNA from single cells", "http://www.ebi.ac.uk/efo/EFO_0005684" );
    /**
     * @deprecated use {@link #SINGLE_CELL_RNA_SEQUENCING_ASSAY} instead
     */
    @Deprecated
    public static final Value SINGLE_CELL_RNA_SEQUENCING = new Value(
            "single-cell RNA sequencing", "http://www.ebi.ac.uk/efo/EFO_0008913" );
    /**
     * @deprecated use {@link #SINGLE_CELL_RNA_SEQUENCING_ASSAY} instead
     */
    @Deprecated
    public static final Value SINGLE_NUCLEUS_RNA_SEQUENCING = new Value(
            "single nucleus RNA sequencing", "http://www.ebi.ac.uk/efo/EFO_0009809" );
    public static final Value FLUORESCENCE_ACTIVATED_CELL_SORTING = new Value(
            "fluorescence-activated cell sorting", "http://www.ebi.ac.uk/efo/EFO_0009108" );
}
