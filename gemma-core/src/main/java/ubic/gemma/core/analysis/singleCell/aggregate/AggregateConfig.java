package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for aggregating single-cell data.
 * @author poirigui
 */
@Getter
@Builder
public
class AggregateConfig {
    /**
     * Make the resulting QT preferred.
     */
    boolean makePreferred;
    /**
     * Perform adjustment of library sizes to better reflect the number of reads in the source sample.
     */
    boolean adjustLibrarySizes;
}
