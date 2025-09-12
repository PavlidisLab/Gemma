package ubic.gemma.core.loader.expression.singleCell;

/**
 * Supported single-cell data types.
 * @author poirigui
 */
public enum SingleCellDataType {
    /**
     * AnnData
     * <p>
     * <a href="https://anndata.readthedocs.io/en/latest/fileformat-prose.html">AnnData on-disk format</a>
     */
    ANNDATA,
    /**
     * <a href="https://mojaveazure.github.io/seurat-disk/">SeuratDisk</a>
     */
    SEURAT_DISK,
    /**
     * <a href="https://loompy.org/">Loom</a>
     */
    LOOM,
    /**
     * MEX format
     * <p>
     * This is usually originating from a 10x Cell Ranger, but our parsing is not limited to that platform.
     * <p>
     * <a href="https://www.10xgenomics.com/support/software/cell-ranger/latest/analysis/outputs/cr-outputs-mex-matrices">Cell Ranger Feature Barcode Matrices (MEX Format)</a>
     */
    MEX,
    /**
     * This is used as an indicator that no data is being loaded.
     */
    NULL
}
