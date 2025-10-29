package ubic.gemma.core.visualization.cellbrowser;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;

public interface CellBrowserService {

    /**
     * Obtain a URL for the Cell Browser for a given {@link ExpressionExperiment}.
     * @param meta metadata to feature, this must correspond to a {@link CellBrowserMapping#getMetaColumnId()} from
     * {@link #getCellBrowserMapping(ExpressionExperiment, boolean)} if non-null
     */
    String getBrowserUrl( ExpressionExperiment ee, @Nullable String meta );

    /**
     * Check if a particular {@link ExpressionExperiment} has a Cell Browser dataset configured.
     */
    boolean hasBrowser( ExpressionExperiment ee );

    Collection<CellBrowserMapping> getCellBrowserMapping( ExpressionExperiment ee, boolean useRawColumnNames );
}
