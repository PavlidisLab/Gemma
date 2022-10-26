package ubic.gemma.core.analysis.service;

import ubic.gemma.core.analysis.preprocess.filter.FilteringException;

public class NoProcessedExpressionDataVectorsException extends FilteringException {
    public NoProcessedExpressionDataVectorsException( String message ) {
        super( message );
    }
}
