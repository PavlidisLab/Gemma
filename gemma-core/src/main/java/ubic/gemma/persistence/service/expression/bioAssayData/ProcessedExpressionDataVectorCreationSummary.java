package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.Data;
import ubic.gemma.core.util.StringUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * Summary information about the creation of processed expression data vectors.
 *
 * @author poirigui
 */
@Data
class ProcessedExpressionDataVectorCreationSummary {
    /**
     * The raw quantitation type that was used to create the processed data.
     */
    QuantitationType rawQuantitationType;
    /**
     * Indicate the number of data vectors created.
     */
    int numberOfDataVectors;
    /**
     * Indicate the number of outliers that were masked.
     */
    int numberOfMaskedOutliers;
    /**
     * Indicate the number of missing values that were masked.
     */
    int numberOfMaskedMissingValues;
    /**
     * Indicate if the data was quantile-normalized.
     */
    boolean quantileNormalized;
    /**
     * Additional comment(s) on the processing.
     */
    String comment;

    public void addComment( String m ) {
        this.comment = StringUtils.appendWithDelimiter( comment, "\n", m );
    }
}
