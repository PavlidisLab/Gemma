package ubic.gemma.core.loader.expression.cellxgene;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.Link;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author poirigui
 */
public class CellXGeneUtils {

    /**
     * List of all known gene expression assays from CELLxGENE.
     * <p>
     * TODO: we should keep a list of supported assays in a shared location
     */
    public static final OntologyTerm[] GENE_EXPRESSION_ASSAYS = {
            new OntologyTerm( "EFO:0030003", "10x 3' transcription profiling" ),
            new OntologyTerm( "EFO:0009901", "10x 3' v1" ),
            new OntologyTerm( "EFO:0009899", "10x 3' v2" ),
            new OntologyTerm( "EFO:0009922", "10x 3' v3" ),
            new OntologyTerm( "EFO:0022604", "10x 3' v4" ),
            new OntologyTerm( "EFO:0030004", "10x 5' transcription profiling" ),
            new OntologyTerm( "EFO:0011025", "10x 5' v1" ),
            new OntologyTerm( "EFO:0009900", "10x 5' v2" ),
            new OntologyTerm( "EFO:0022605", "10x 5' v3" ),
            new OntologyTerm( "EFO:0030059", "10x multiome" ),
            new OntologyTerm( "EFO:0022606", "10x gene expression flex" ),
            new OntologyTerm( "EFO:0008930", "Smart-seq" ),
            new OntologyTerm( "EFO:0008931", "Smart-seq2" ),
            new OntologyTerm( "EFO:0022488", "Smart-seq3" ),
            new OntologyTerm( "EFO:0700016", "Smart-seq v4" ),
            new OntologyTerm( "EFO:0022857", "Visium Spatial Gene Expression V1" ),
            new OntologyTerm( "EFO:0022858", "Visium Spatial Gene Expression V2" ),
            new OntologyTerm( "EFO:0022859", "Visium CytAssist Spatial Gene Expression, 6.5mm" ),
            new OntologyTerm( "EFO:0022860", "Visium CytAssist Spatial Gene Expression, 11mm" ),
            new OntologyTerm( "EFO:0008722", "Drop-seq" ),
            new OntologyTerm( "EFO:0008780", "InDrops" ),
            new OntologyTerm( "EFO:0030028", "sci-RNA-seq3" ),
            new OntologyTerm( "EFO:0008679", "CEL-Seq" ),
            new OntologyTerm( "EFO:0010010", "CEL-Seq2" ),
            new OntologyTerm( "EFO:0022845", "modified STRT-seq" ),
            new OntologyTerm( "EFO:0700010", "TruDrop" ),
            new OntologyTerm( "EFO:0022490", "ScaleBio single cell RNA sequencing" ),
            new OntologyTerm( "EFO:0008919", "Seq-Well" ),
            new OntologyTerm( "EFO:0030019", "Seq-Well S3" ),
            new OntologyTerm( "EFO:0008796", "MARS-seq" ),
            new OntologyTerm( "EFO:0008992", "MERFISH" ),
            new OntologyTerm( "EFO:0030062", "Slide-seqV2" ),
            new OntologyTerm( "EFO:0008853", "Patch-seq" ),
            new OntologyTerm( "EFO:0009919", "SPLiT-seq" ),
            new OntologyTerm( "EFO:0700004", "BD Rhapsody Targeted mRNA Profiling" ),
            new OntologyTerm( "EFO:0700003", "BD Rhapsody Whole Transcriptome Analysis" ),
            new OntologyTerm( "EFO:0030026", "sci-Plex" ),
            new OntologyTerm( "EFO:0030002", "microwell-seq" ),
            new OntologyTerm( "EFO:0900002", "HIVE CLX Single-Cell RNAseq Solution" ),
            new OntologyTerm( "EFO:0900001", "Asteria scRNA-seq kit" ),
            new OntologyTerm( "EFO:0022601", "Parse Evercode Whole Transcriptome v2" ),
            new OntologyTerm( "EFO:0900000", "particle-templated instant partition sequencing" )
    };

    private static final Set<OntologyTerm> GENE_EXPRESSION_ASSAYS_SET = new HashSet<>( Arrays.asList( GENE_EXPRESSION_ASSAYS ) );

    /**
     * Extract GEO accessions from the given collection metadata.
     */
    public static Set<String> getGeoAccessions( CollectionMetadata cm ) {
        Assert.notNull( cm.getLinks(), "Cannot extract GEO accessions from a shallow CollectionMetadata." );
        Set<String> result = new HashSet<>();
        for ( Link link : cm.getLinks() ) {
            if ( link.getLinkUrl().startsWith( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" ) ) {
                result.add( link.getLinkUrl().substring( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=".length() ) );
            }
            if ( link.getLinkUrl().startsWith( "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" ) ) {
                result.add( link.getLinkUrl().substring( "http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=".length() ) );
            }
        }
        return result;
    }

    /**
     * Determine if the given assay corresponds to a single cell assay producing gene expression data.
     */
    public static boolean isGeneExpressionAssay( OntologyTerm a ) {
        return GENE_EXPRESSION_ASSAYS_SET.contains( a );
    }

    /**
     * Check if the given dataset asset is in AnnData format.
     * <p>
     * TODO: handle {@link FileType#RAW_H5AD}
     */
    public static boolean isAnnData( DatasetAsset a ) {
        return a.getFiletype() == FileType.H5AD;
    }
}
