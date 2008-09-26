package ubic.gemma.grid.javaspaces.analysis.preprocess;

import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.SpacesTask;

/**
 * A task interface to wrap preprocessing expression data vectors.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorCreateTask extends SpacesTask {

    public SpacesResult execute( SpacesProcessedExpressionDataVectorCreateCommand processedVectorCreateCommand );

}
