package ubic.gemma.core.loader.genome.gene.ncbi.model;

/**
 * <p>
 * See ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README
 * </p>
 * <pre>
 * gene2accession
 * ---------------------------------------------------------------------------
 * This file is a comprehensive report of the accessions that are
 * related to a GeneID.  It includes sequences from the international
 * sequence collaboration, Swiss-Prot, and RefSeq.
 * This file can be considered as the logical equivalent of
 * ftp://ftp.ncbi.nih.gov/refseq/LocusLink/loc2ref
 * AND
 * ftp://ftp.ncbi.nih.gov/refseq/LocusLink/loc2acc
 * tab-delimited
 * one line per genomic/RNA/protein set of sequence accessions
 * ---------------------------------------------------------------------------
 * tax_id:
 * the unique identifier provided by NCBI Taxonomy
 * for the species or strain/isolate
 * GeneID:
 * the unique identifier for a gene
 * --note:  for genomes previously available from LocusLink,
 * the identifiers are equivalent
 * status:
 * status of the RefSeq if a refseq, else '-'
 * RNA nucleotide accession.version
 * may be null (-) for some genomes
 * RNA nucleotide gi
 * the gi for an RNA nucleotide accession, '-' if not applicable
 * protein accession.version
 * will be null (-) for RNA-coding genes
 * protein gi:
 * the gi for a protein accession, '-' if not applicable
 * genomic nucleotide accession.version
 * may be null (-)
 * genomic nucleotide gi
 * the gi for a genomic nucleotide accession, '-' if not applicable
 * start position on the genomic accession
 * position of the gene feature on the genomic accession,
 * '-' if not applicable
 * position 0-based
 * end position on the genomic accession
 * position of the gene feature on the genomic accession,
 * '-' if not applicable
 * position 0-based
 * orientation
 * orientation of the gene feature on the genomic accession,
 * '?' if not applicable
 * </pre>
 *
 * @author pavlidis
 */
public class NCBIGene2Accession {

    private int taxId;
    private String geneId;
    private String status;
    private String rnaNucleotideAccession;
    private String rnaNucleotideAccessionVersion;
    private String rnaNucleotideGI;
    private String proteinAccession;
    private String proteinAccessionVersion;
    private String proteinGI;
    private String genomicNucleotideAccession;
    private String genomicNucleotideAccessionVersion;
    private String genomicNucleotideGI;
    private Long startPosition;
    private Long endPosition;
    private String orientation;

    private NCBIGeneInfo info;

    /**
     * @return the genomicNucleotideAccession
     */
    public String getGenomicNucleotideAccession() {
        return genomicNucleotideAccession;
    }

    /**
     * @param genomicNucleotideAccession the genomicNucleotideAccession to set
     */
    public void setGenomicNucleotideAccession( String genomicNucleotideAccession ) {
        this.genomicNucleotideAccession = genomicNucleotideAccession;
    }

    /**
     * @return the genomicNucleotideAccessionVersion
     */
    public String getGenomicNucleotideAccessionVersion() {
        return genomicNucleotideAccessionVersion;
    }

    /**
     * @param genomicNucleotideAccessionVersion the genomicNucleotideAccessionVersion to set
     */
    public void setGenomicNucleotideAccessionVersion( String genomicNucleotideAccessionVersion ) {
        this.genomicNucleotideAccessionVersion = genomicNucleotideAccessionVersion;
    }

    /**
     * @return the proteinAccessionVersion
     */
    public String getProteinAccessionVersion() {
        return proteinAccessionVersion;
    }

    /**
     * @param proteinAccessionVersion the proteinAccessionVersion to set
     */
    public void setProteinAccessionVersion( String proteinAccessionVersion ) {
        this.proteinAccessionVersion = proteinAccessionVersion;
    }

    /**
     * @return the rnaNucleotideAccession
     */
    public String getRnaNucleotideAccession() {
        return rnaNucleotideAccession;
    }

    /**
     * @param rnaNucleotideAccession the rnaNucleotideAccession to set
     */
    public void setRnaNucleotideAccession( String rnaNucleotideAccession ) {
        this.rnaNucleotideAccession = rnaNucleotideAccession;
    }

    /**
     * @return the rnaNucleotideAccessionVersion
     */
    public String getRnaNucleotideAccessionVersion() {
        return rnaNucleotideAccessionVersion;
    }

    /**
     * @param rnaNucleotideAccessionVersion the rnaNucleotideAccessionVersion to set
     */
    public void setRnaNucleotideAccessionVersion( String rnaNucleotideAccessionVersion ) {
        this.rnaNucleotideAccessionVersion = rnaNucleotideAccessionVersion;
    }

    /**
     * @return the rnaNucleotideGI
     */
    public String getRnaNucleotideGI() {
        return rnaNucleotideGI;
    }

    /**
     * @param rnaNucleotideGI the rnaNucleotideGI to set
     */
    public void setRnaNucleotideGI( String rnaNucleotideGI ) {
        this.rnaNucleotideGI = rnaNucleotideGI;
    }

    /**
     * @return Returns the info.
     */
    public NCBIGeneInfo getInfo() {
        return this.info;
    }

    /**
     * @param info The info to set.
     */
    public void setInfo( NCBIGeneInfo info ) {
        this.info = info;
    }

    /**
     * @return Returns the endPosition.
     */
    public Long getEndPosition() {
        return this.endPosition;
    }

    /**
     * @param endPosition The endPosition to set.
     */
    public void setEndPosition( Long endPosition ) {
        this.endPosition = endPosition;
    }

    /**
     * @return Returns the geneId.
     */
    public String getGeneId() {
        return this.geneId;
    }

    /**
     * @param geneId The geneId to set.
     */
    public void setGeneId( String geneId ) {
        this.geneId = geneId;
    }

    /**
     * @return Returns the genomicNucleotideGI.
     */
    public String getGenomicNucleotideGI() {
        return this.genomicNucleotideGI;
    }

    /**
     * @param genomicNucleotideGI The genomicNucleotideGI to set.
     */
    public void setGenomicNucleotideGI( String genomicNucleotideGI ) {
        this.genomicNucleotideGI = genomicNucleotideGI;
    }

    /**
     * @return Returns the orientation.
     */
    public String getOrientation() {
        return this.orientation;
    }

    /**
     * @param orientation The orientation to set.
     */
    public void setOrientation( String orientation ) {
        this.orientation = orientation;
    }

    /**
     * @return Returns the proteinAccessionVersion.
     */
    public String getProteinAccession() {
        return this.proteinAccession;
    }

    /**
     * @param proteinAccession The proteinAccessionVersion to set.
     */
    public void setProteinAccession( String proteinAccession ) {
        this.proteinAccession = proteinAccession;
    }

    /**
     * @return Returns the proteinGI.
     */
    public String getProteinGI() {
        return this.proteinGI;
    }

    /**
     * @param proteinGI The proteinGI to set.
     */
    public void setProteinGI( String proteinGI ) {
        this.proteinGI = proteinGI;
    }

    /**
     * @return Returns the startPosition.
     */
    public Long getStartPosition() {
        return this.startPosition;
    }

    /**
     * @param startPosition The startPosition to set.
     */
    public void setStartPosition( Long startPosition ) {
        this.startPosition = startPosition;
    }

    /**
     * @return Returns the status.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * @param status The status to set.
     */
    public void setStatus( String status ) {
        this.status = status;
    }

    /**
     * @return Returns the taxId.
     */
    public int getTaxId() {
        return this.taxId;
    }

    /**
     * @param taxId The taxId to set.
     */
    public void setTaxId( int taxId ) {
        this.taxId = taxId;
    }

}
