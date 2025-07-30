package ubic.gemma.core.visualization.cellbrowser;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface CellBrowserService {

    /**
     * Obtain a URL for the Cell Browser for a given {@link ExpressionExperiment}.
     */
    String getBrowserUrl( ExpressionExperiment ee );

    /**
     * Check if a particular {@link ExpressionExperiment} has a Cell Browser dataset configured.
     */
    boolean hasBrowser( ExpressionExperiment ee );
}
