package ubic.gemma.core.loader.expression.cellxgene;

import org.apache.commons.lang3.Strings;
import ubic.gemma.core.loader.expression.cellxgene.model.OntologyTerm;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;

/**
 * @author poirigui
 */
public class CellXGeneUtils {

    /**
     * Determine if the given assay corresponds to a single cell assay producing gene expression data.
     */
    public static boolean isGeneExpressionAssay( OntologyTerm a ) {
        // TODO: we should keep a list of supported assays in a shared location
        return Strings.CI.equalsAny( a.getOntologyTermId(),
                // 10x 3' v1
                "EFO:0009901",
                // 10x 3' v2
                "EFO:0009899",
                // 10x 3' v3
                "EFO:0009922",
                // 10x 3' v4
                "EFO:0022604",
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
                "EFO:0008780"
        );
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
