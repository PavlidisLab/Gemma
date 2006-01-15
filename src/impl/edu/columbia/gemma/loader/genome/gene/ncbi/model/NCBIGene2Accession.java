package edu.columbia.gemma.loader.genome.gene.ncbi.model;

/**
 * <p>
 * See ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README
 * </p>
 * 
 * <pre>
 *                                   gene2accession
 *                                    ---------------------------------------------------------------------------
 *                                    This file is a comprehensive report of the accessions that are 
 *                                    related to a GeneID.  It includes sequences from the international
 *                                    sequence collaboration, Swiss-Prot, and RefSeq.
 *                                   
 *                                    This file can be considered as the logical equivalent of
 *                                   
 *                                    ftp://ftp.ncbi.nih.gov/refseq/LocusLink/loc2ref
 *                                    AND
 *                                    ftp://ftp.ncbi.nih.gov/refseq/LocusLink/loc2acc
 *                                   
 *                                    tab-delimited
 *                                    one line per genomic/RNA/protein set of sequence accessions
 *                                    ---------------------------------------------------------------------------
 *                                   
 *                                    tax_id:
 *                                    the unique identifier provided by NCBI Taxonomy
 *                                    for the species or strain/isolate
 *                                   
 *                                    GeneID:
 *                                    the unique identifier for a gene
 *                                    --note:  for genomes previously available from LocusLink,
 *                                    the identifiers are equivalent
 *                                   
 *                                    status:
 *                                    status of the RefSeq if a refseq, else '-'
 *                                   
 *                                    RNA nucleotide accession.version
 *                                    may be null (-) for some genomes
 *                                   
 *                                    RNA nucleotide gi
 *                                    the gi for an RNA nucleotide accession, '-' if not applicable
 *                                   
 *                                    protein accession.version
 *                                    will be null (-) for RNA-coding genes
 *                                   
 *                                    protein gi:
 *                                    the gi for a protein accession, '-' if not applicable
 *                                   
 *                                    genomic nucleotide accession.version
 *                                    may be null (-) 
 *                                   
 *                                    genomic nucleotide gi
 *                                    the gi for a genomic nucleotide accession, '-' if not applicable
 *                                   
 *                                    start position on the genomic accession
 *                                    position of the gene feature on the genomic accession,
 *                                    '-' if not applicable
 *                                    position 0-based
 *                                   
 *                                    end positon on the genomic accession
 *                                    position of the gene feature on the genomic accession,
 *                                    '-' if not applicable
 *                                    position 0-based
 *                                   
 *                                    orientation
 *                                    orientation of the gene feature on the genomic accession,
 *                                    '?' if not applicable
 * </pre>
 * 
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGene2Accession {

    private int taxId;
    private String geneId;
    private String status;
    private String RNANucleotideAccession;
    private String RNANucleotideGI;
    private String proteinAccession;
    private String proteinGI;
    private String GenomicNucleotideAccession;
    private String genomicNucleotideGI;
    private int startPosition;
    private int endPosition;
    private String orientation;

    private NCBIGeneInfo info;

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
    public int getEndPosition() {
        return this.endPosition;
    }

    /**
     * @param endPosition The endPosition to set.
     */
    public void setEndPosition( int endPosition ) {
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
     * @return Returns the genomicNucleotideAccessionVersion.
     */
    public String getGenomicNucleotideAccession() {
        return this.GenomicNucleotideAccession;
    }

    /**
     * @param genomicNucleotideAccession The genomicNucleotideAccession to set.
     */
    public void setGenomicNucleotideAccession( String genomicNucleotideAccession ) {
        this.GenomicNucleotideAccession = genomicNucleotideAccession;
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
     * @param proteinAccessionVersion The proteinAccessionVersion to set.
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
     * @return Returns the rNANucleotideAccessionVersion.
     */
    public String getRNANucleotideAccession() {
        return this.RNANucleotideAccession;
    }

    /**
     * @param nucleotideAccession The rNANucleotideAccession to set.
     */
    public void setRNANucleotideAccession( String nucleotideAccession ) {
        this.RNANucleotideAccession = nucleotideAccession;
    }

    /**
     * @return Returns the startPosition.
     */
    public int getStartPosition() {
        return this.startPosition;
    }

    /**
     * @param startPosition The startPosition to set.
     */
    public void setStartPosition( int startPosition ) {
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

    /**
     * @return Returns the rNANucleotideGI.
     */
    public String getRNANucleotideGI() {
        return this.RNANucleotideGI;
    }

    /**
     * @param nucleotideGI The rNANucleotideGI to set.
     */
    public void setRNANucleotideGI( String nucleotideGI ) {
        this.RNANucleotideGI = nucleotideGI;
    }
}
