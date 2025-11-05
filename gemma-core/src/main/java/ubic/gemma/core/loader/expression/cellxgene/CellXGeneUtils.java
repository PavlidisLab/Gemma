package ubic.gemma.core.loader.expression.cellxgene;

import org.apache.commons.lang3.Strings;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.Link;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;

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
    public static final String[] GENE_EXPRESSION_ASSAYS = {
            // 10x 3' transcription profiling
            "EFO:0030003",
            // 10x 3' v1
            "EFO:0009901",
            // 10x 3' v2
            "EFO:0009899",
            // 10x 3' v3
            "EFO:0009922",
            // 10x 3' v4
            "EFO:0022604",
            // 10x 5' transcription profiling
            "EFO:0030004",
            // 10x 5' v1
            "EFO:0011025",
            // 10x 5' v2
            "EFO:0009900",
            // 10x 5' v3
            "EFO:0022605",
            // 10x multiome
            "EFO:0030059",
            // 10x gene expression flex
            "EFO:0022606",
            // Smart-seq
            "EFO:0008930",
            // Smart-seq2
            "EFO:0008931",
            // Smart-seq3
            "EFO:0022488",
            // Smart-seq v4
            "EFO:0700016",
            // Visium Spatial Gene Expression V1
            "EFO:0022857",
            // Visium Spatial Gene Expression V2
            "EFO:0022858",
            // Visium CytAssist Spatial Gene Expression, 6.5mm
            "EFO:0022859",
            // Visium CytAssist Spatial Gene Expression, 11mm
            "EFO:0022860",
            // Drop-seq
            "EFO:0008722",
            // InDrops
            "EFO:0008780",
            // sci-RNA-seq3
            "EFO:0030028",
            // CEL-Seq
            "EFO:0008679",
            // CEL-seq2
            "EFO:0010010",
            // modified STRT-seq
            "EFO:0022845",
            // TruDrop
            "EFO:0700010",
            // ScaleBio single cell RNA sequencing
            "EFO:0022490",
            // Seq-Well
            "EFO:0008919",
            // Seq-Well S3
            "EFO:0030019",
            // MARS-seq
            "EFO:0008796",
            // MERFISH
            "EFO:0008992",
            // Slide-seqV2
            "EFO:0030062",
            // Patch-seq (low-throughput)
            "EFO:0008853",
            // "SPLiT-seq"
            "EFO:0009919"
    };

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
        return Strings.CI.equalsAny( a.getOntologyTermId(), GENE_EXPRESSION_ASSAYS );
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
