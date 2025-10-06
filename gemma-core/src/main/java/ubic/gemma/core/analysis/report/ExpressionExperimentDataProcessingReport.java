package ubic.gemma.core.analysis.report;

import lombok.Builder;
import lombok.Value;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Report that describes the data processing of an {@link ExpressionExperiment} that resulted in its current processed
 * data vectors.
 * <p>
 * The report is very broad for the moment because it is assembled by pulling text from audit events.
 *
 * @author poirigui
 */
@Value
@Builder
public class ExpressionExperimentDataProcessingReport {

    /**
     * Details about the original data, prior to being loaded in Gemma.
     * <p>
     * This includes descriptions of filenames, detected data formats, etc. This is usually pulled from the
     * {@link QuantitationType} description.
     */
    public Details originalData;

    /**
     * Details of how genes from the data was mapped onto {@link CompositeSequence}.
     */
    public Details geneMapping;

    /**
     * Details of how samples from the data was mapped onto {@link BioAssay}s.
     */
    public Details sampleMapping;

    /**
     * Details of the data transformation.
     * <p>
     * This may include transposing a data matrix, filtering genes, cells or assays.
     * <p>
     * In the case of microarray, this will contain Affymetrix Power Tools output if the data was re-processed from CEL
     * data.
     */
    public Details transformation;

    /**
     * Details of how the {@link ExpressionExperiment} assays were subsetted in multiple {@link ExpressionExperimentSubSet}.
     * <p>
     * Usually only applicable to single-cell data.
     */
    @Nullable
    public Details subsetting;

    /**
     * Details of how data was aggregated.
     * <p>
     * Usually only applicable to single-cell data.
     */
    @Nullable
    public Details aggregation;

    /**
     * Details of how data was pre-processed to produce {@link ProcessedExpressionDataVector} from {@link RawExpressionDataVector}.
     * <p>
     * These transformations are mainly achieved by the {@link ubic.gemma.core.analysis.preprocess.PreprocessorService}
     * and include two-color microarray handling, quantile normalization, log2cpm transformation, etc.
     */
    public Details preprocessing;

    @Value
    public static class Details {
        String details;
        Date date;
    }
}
