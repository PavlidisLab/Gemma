package ubic.gemma.core.loader.expression.sequencing;

import ubic.gemma.core.loader.expression.AbstractDelegatingDataLoader;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractDelegatingSequencingDataLoader extends AbstractDelegatingDataLoader implements SequencingDataLoader {

    private final SequencingDataLoader delegate;

    protected AbstractDelegatingSequencingDataLoader( SequencingDataLoader delegate ) {
        super( delegate );
        this.delegate = delegate;
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws IOException {
        return delegate.getSequencingMetadata( samples );
    }
}
