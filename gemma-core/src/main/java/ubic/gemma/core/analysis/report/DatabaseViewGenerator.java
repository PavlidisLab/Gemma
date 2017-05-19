package ubic.gemma.core.analysis.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ubic.gemma.persistence.util.Settings;

public interface DatabaseViewGenerator {

    public static final String VIEW_DIR = Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "dataFiles"
            + File.separatorChar;
    public static final String VIEW_FILE_SUFFIX = ".view.txt.gz";

    /**
     * @param limit
     * @throws FileNotFoundException
     * @throws IOException
     */
    public abstract void generateDatasetTissueView( int limit ) throws FileNotFoundException, IOException;

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    public abstract void generateDatasetView( int limit ) throws FileNotFoundException, IOException;

    /**
     * @param limit how many experiments to use
     * @throws FileNotFoundException
     * @throws IOException
     */
    public abstract void generateDifferentialExpressionView( int limit ) throws FileNotFoundException, IOException;

    /**
     * @param filename
     * @return
     */
    public abstract File getOutputFile( String filename );

    public abstract void runAll();

    /**
     * @param limit if null will run against every dataset in Gemma else will only do the 1st to the given limit
     */
    public abstract void runAll( Integer limit );

}