package ubic.gemma.grid.javaspaces.analysis.preprocess;

import ubic.gemma.grid.javaspaces.SpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;

/**
 * A task interface to wrap preprocessing expression data vectors.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorCreateTask extends SpacesTask {
    public TaskResult execute( ProcessedExpressionDataVectorCreateTaskCommand command );
}
