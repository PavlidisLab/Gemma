package ubic.gemma.loader.expression.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface SimpleExpressionDataLoaderService {

    /**
     * @param metaData
     * @param matrix
     * @return ExpressionExperiment
     */
    public abstract ExpressionExperiment convert( SimpleExpressionExperimentMetaData metaData,
            DoubleMatrix<String, String> matrix );

    /**
     * @param matrix
     * @param usedDesignElements
     * @param design
     * @return
     */
    public abstract DoubleMatrix<String, String> getSubMatrixForArrayDesign( DoubleMatrix<String, String> matrix,
            Collection<Object> usedDesignElements, ArrayDesign design );

    /**
     * Parses, converts (into Gemma objects), and loads data into the database.
     * 
     * @param metaData
     * @param data tab-delimited file with row names corresponding to CompositeSequence names and column names
     *        corresponding to BioAssay names.
     * @return
     * @throws IOException
     */
    public abstract ExpressionExperiment create( SimpleExpressionExperimentMetaData metaData, InputStream data )
            throws IOException;

    /**
     * @param data
     * @return DoubleMatrixNamed
     * @throws IOException
     */
    public abstract DoubleMatrix<String, String> parse( InputStream data ) throws IOException;

}