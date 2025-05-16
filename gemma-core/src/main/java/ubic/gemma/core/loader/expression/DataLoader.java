package ubic.gemma.core.loader.expression;

import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Base interface for loading expression data in Gemma.
 * @author poirigui
 */
public interface DataLoader extends Closeable {

    /**
     * Set the strategy used for mapping {@link BioAssay} to sample names from the data.
     */
    void setBioAssayToSampleNameMapper( BioAssayMapper bioAssayToSampleNameMatcher );

    /**
     * Ignore unmatched samples from the data.
     * <p>
     * This defaults to true.
     */
    void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples );

    /**
     * Set the strategy used for mapping {@link CompositeSequence} to gene identifiers from the data.
     */
    void setDesignElementToGeneMapper( DesignElementMapper designElementToGeneMapper );

    /**
     * Ignore unmatched design elements from the data when creating vectors.
     * <p>
     * This defaults to true.
     * <p>
     * There's a <a href="https://github.com/PavlidisLab/Gemma/issues/973">discussions to make this default in false</a>
     * in general for sequencing data.
     */
    void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements );

    /**
     * Obtain the sample names present in the data.
     */
    Set<String> getSampleNames() throws IOException;

    /**
     * Load quantitation types present in the data.
     */
    Set<QuantitationType> getQuantitationTypes() throws IOException;

    /**
     * Load experimental factors present in the data.
     * @param samples                samples to use when determining which factors to load
     * @param factorValueAssignments if non-null, the proposed assignment of factor values to samples are populated in
     *                               the mapping.
     * @return a set of factors present in the data
     */
    Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException;

    /**
     * Load samples characteristics present in the data.
     * @param samples to use when determining which characteristics to load
     * @return proposed characteristics grouped by sample
     */
    Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException;

    /**
     * Load gene identifiers present in the data.
     */
    Set<String> getGenes() throws IOException;

    /**
     * Free any resources that the loaded has setup.
     */
    @Override
    void close() throws IOException;
}
