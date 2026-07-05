package ubic.gemma.model.common.description;

/**
 * Enumerates various globally available {@link ExternalDatabase} by name.
 *
 * @author poirigui
 */
public final class ExternalDatabases {

    public static final String
            GEO = "GEO",
            SRA = "SRA",
            ARRAY_EXPRESS = "ArrayExpress",
            BIO_STUDIES = "BioStudies",
            GENE = "gene",
            GO = "go",
            MULTIFUNCTIONALITY = "multifunctionality",
            GENE2CS = "gene2cs",
            PUBMED = "PubMed",
            ARXIV = "arXiv",
            BIORXIV = "bioRxiv",
            CELLXGENE = "CELLxGENE",
            GENBANK = "GenBank",
            UCSC_CELL_BROWSER = "UCSC Cell Browser",
            SYNAPSE = "Synapse",
            ZENODO = "Zenodo",
            // Generic DOI namespace — the external database under which DOI-identified references are
            // stored (e.g. CrossRef-resolved preprints/articles not indexed by PubMed). Using one
            // deterministic name keeps DOI lookups idempotent regardless of the underlying source.
            DOI = "DOI";
}
