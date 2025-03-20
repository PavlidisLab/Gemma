package ubic.gemma.core.loader.expression;

import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AbstractDelegatingDataLoader implements DataLoader {

    private final DataLoader delegate;

    protected AbstractDelegatingDataLoader( DataLoader delegate ) {
        this.delegate = delegate;
    }

    @Override
    public void setBioAssayToSampleNameMapper( BioAssayMapper bioAssayToSampleNameMatcher ) {
        delegate.setBioAssayToSampleNameMapper( bioAssayToSampleNameMatcher );
    }

    @Override
    public void setDesignElementToGeneMapper( DesignElementMapper designElementToGeneMapper ) {
        delegate.setDesignElementToGeneMapper( designElementToGeneMapper );
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        delegate.setIgnoreUnmatchedSamples( ignoreUnmatchedSamples );
    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {
        delegate.setIgnoreUnmatchedDesignElements( ignoreUnmatchedDesignElements );
    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        return delegate.getSampleNames();
    }

    @Override
    public Set<String> getGenes() throws IOException {
        return delegate.getGenes();
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return delegate.getQuantitationTypes();
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        return delegate.getFactors( samples, factorValueAssignments );
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        return delegate.getSamplesCharacteristics( samples );
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
