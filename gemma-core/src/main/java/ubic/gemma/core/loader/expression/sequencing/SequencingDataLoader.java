package ubic.gemma.core.loader.expression.sequencing;

import ubic.gemma.core.loader.expression.DataLoader;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * An interface for data loaders that can load sequencing data.
 * @author poirigui
 */
public interface SequencingDataLoader extends DataLoader {

    /**
     * Retrieve various sequencing metadata if counting data is present.
     */
    Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws IOException;
}
