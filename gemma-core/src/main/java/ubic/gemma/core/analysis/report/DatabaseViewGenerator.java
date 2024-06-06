package ubic.gemma.core.analysis.report;

import ubic.gemma.core.config.Settings;

import java.io.File;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface DatabaseViewGenerator {

    String VIEW_DIR =
            Settings.getString( "gemma.appdata.home" ) + File.separatorChar + "dataFiles" + File.separatorChar;
    String VIEW_FILE_SUFFIX = ".view.txt.gz";

    File getOutputFile( String filename );

    void runAll();

    /**
     * @param limit if null will run against every dataset in Gemma else will only do the 1st to the given limit
     */
    void runAll( Integer limit );

}