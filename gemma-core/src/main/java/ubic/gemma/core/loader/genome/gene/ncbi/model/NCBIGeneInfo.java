package ubic.gemma.core.loader.genome.gene.ncbi.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>
 * See ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/README
 * </p>
 *
 * <pre>
 *   ===========================================================================
 *   gene_info
 *   ---------------------------------------------------------------------------
 *   This file can be considered as the logical equivalent of
 *   ftp://ftp.ncbi.nih.gov/refseq/LocusLink/LL.out
 *   tab-delimited
 *   one line per GeneID
 *   ---------------------------------------------------------------------------
 *   tax_id:
 *   the unique identifier provided by NCBI Taxonomy
 *   for the species or strain/isolate
 *   GeneID:
 *   the unique identifier for a gene
 *   ASN1:  geneid
 *   --note:  for genomes previously available from LocusLink,
 *   the identifiers are equivalent
 *   Symbol:
 *   the default symbol for the gene
 *   ASN1:  gene-&gt;locus
 *   LocusTag:
 *   the LocusTag value
 *   ASN1:  gene-&gt;locus-tag
 *   Synonyms:
 *   bar-delimited set of unoffical symbols for the gene
 *   dbXrefs:
 *   bar-delimited set of identifiers in other databases
 *   for this gene.  The unit of the set is database:value.
 *   chromosome:
 *   the chromosome on which this gene is placed
 *   map location:
 *   the map location for this gene
 *   description
 *   a descriptive name for this gene
 *   type of gene:
 *   the type assigned to the gene according to the list of options
 *   provided in http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn
 *   Symbol from nomenclature authority:
 *   when not '-', indicates that this symbol is from a
 *   a nomenclature authority
 *   Full name from nomenclature authority:
 *   when not '-', indicates that this full name is from a
 *   a nomenclature authority
 *   Nomenclature status
 *   when not '-', indicates the status of the name from the
 *   nomenclature authority (O for official, I for interim)
 * </pre>
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
@Getter
@Setter
public class NCBIGeneInfo {

    @Setter(AccessLevel.NONE)
    private final Collection<String> synonyms = new HashSet<>();
    @Setter(AccessLevel.NONE)
    private final Map<String, String> dbXrefs = new HashMap<>();
    private int taxId;
    private String geneId;
    private String defaultSymbol;
    private String locusTag;
    private String chromosome;
    private String mapLocation;
    private String description;
    private GeneType geneType;
    private boolean symbolIsFromAuthority;
    private boolean nameIsFromAuthority;
    private NomenclatureStatus nomenclatureStatus;
    private NcbiGeneHistory history;
    private String ensemblId = null;
    /**
     * NCBI gene ID that was 'discontinued' for the gene that match this symbol and taxon.
     * <p>
     * Accessed via {@link #getDiscontinuedId()} / {@link #setDiscontinuedId(String)} which handle accumulation.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String discontinuedIdForGene = null;

    /**
     * Convert string to GeneType. See
     * http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn
     *
     *
     * @param typeString type string
     * @return gene type
     */
    public static GeneType typeStringToGeneType( String typeString ) {
        switch ( typeString ) {
            case "unknown":
                return GeneType.UNKNOWN;
            case "tRNA":
                return GeneType.TRNA;
            case "rRNA":
                return GeneType.RRNA;
            case "snRNA":
                return GeneType.SNRNA;
            case "scRNA":
                return GeneType.SCRNA;
            case "snoRNA":
                return GeneType.SNORNA;
            case "protein-coding":
                return GeneType.PROTEINCODING;
            case "pseudo":
                return GeneType.PSEUDO;
            case "transposon":
                return GeneType.TRANSPOSON; // no longer used? but part of spec.
            case "miscRNA":
                return GeneType.MISCRNA;
            case "ncRNA":
                return GeneType.NCRNA;
            case "other":
                return GeneType.OTHER;
            case "biological-region":
                return GeneType.OTHER;
            default:
                throw new IllegalArgumentException( "Unknown gene type '" + typeString + "'" );
        }
    }

    public void addToDbXRefs( String dbName, String identifier ) {
        this.dbXrefs.put( dbName, identifier );
    }

    public void addToSynonyms( String synonym ) {
        this.synonyms.add( synonym );
    }

    /**
     * @return The NCBI gene ID that was 'discontinued' for the gene that match this symbol and taxon. These
     *         correspond to the lines in gene_history that have a '-' in the second column. But because we are matching
     *         only on the symbol+taxon, we have to be a bit careful using it.
     */
    public String getDiscontinuedId() {
        return discontinuedIdForGene;
    }

    public void setDiscontinuedId( String discontinuedIdForGene ) {
        if ( StringUtils.isNotBlank( this.discontinuedIdForGene ) ) {
            this.discontinuedIdForGene = this.discontinuedIdForGene + "," + discontinuedIdForGene;
        } else {
            this.discontinuedIdForGene = discontinuedIdForGene;
        }
    }

    /**
     * See http://www.ncbi.nlm.nih.gov/IEB/ToolBox/CPP_DOC/lxr/source/src/objects/entrezgene/entrezgene.asn unknown (0)
     * , 36
     */
    public enum GeneType {
        UNKNOWN, TRNA, RRNA, SNRNA, SCRNA, SNORNA, PROTEINCODING, PSEUDO, TRANSPOSON, MISCRNA, NCRNA, OTHER
    }

    public enum NomenclatureStatus {
        OFFICIAL, INTERIM, UNKNOWN
    }

}
