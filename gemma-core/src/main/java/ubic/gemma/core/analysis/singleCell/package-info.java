/**
 * This package contains classes for analysing single-cell expression data.
 * <ul>
 * <li>computing sparsity metrics with {@link ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics}</li>
 * <li>computing descriptive statistics with {@link ubic.gemma.core.analysis.singleCell.SingleCellDescriptive}</li>
 * <li>subsetting assays into sub-assays (and materials in sub-materials) for representing pseudo-bulks</li>
 * <li>masking cells with {@link ubic.gemma.core.analysis.singleCell.SingleCellMaskUtils}</li>
 * <li>aggregating expression data into vectors applicable to sub-assays</li>
 * </ul>
 * Once transformed in this way, the data becomes suitable for preprocessing and analysis.
 * @author poirigui
 */
@NonNullApi
package ubic.gemma.core.analysis.singleCell;

import org.springframework.lang.NonNullApi;