package ubic.gemma.core.analysis.report;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface DatabaseViewGenerator {

    String VIEW_DIR =
            Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "dataFiles" + File.separatorChar;
    String VIEW_FILE_SUFFIX = ".view.txt.gz";

    void generateDatasetTissueView( int limit, Collection<ExpressionExperiment> experiments ) throws IOException;

    void generateDatasetView( int limit, Collection<ExpressionExperiment> experiments ) throws IOException;

    void generateDifferentialExpressionView( int limit, Collection<ExpressionExperiment> experiments )
            throws IOException;

    File getOutputFile( String filename );

    void runAll();

    /**
     * @param limit if null will run against every dataset in Gemma else will only do the 1st to the given limit
     */
    void runAll( Integer limit );

}