package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.File;

/**
 * Types of metadata files that can be attached to an {@link ExpressionExperiment}.
 */
public enum ExpressionExperimentMetaFileType {

    BASE_METADATA( 1, ".base.metadata", "Sequence analysis summary", ".seq.analysis.sum.txt", false, true, "text/plain" ),

    ALIGNMENT_METADATA( 2, ".alignment.metadata", "Alignment statistics", ".alignment.statistics.txt", false,
            true, "text/plain" ),

    MULTIQC_REPORT( 3, "MultiQCReports" + File.separatorChar + "multiqc_report.html", "MultiQC Report",
            ".multiqc.report.html", false, false, "text/html" ),
    MULTIQC_DATA( 5, "MultiQCReports" + File.separatorChar + "multiqc_data.json", "MultiQC Report (JSON metadata)",
            ".multiqc.data.json", false, false, "application/json" ),
    MULTIQC_LOG( 6, "MultiQCReports" + File.separatorChar + "multiqc.log", "MultiQC Report (log file)",
            ".multiqc.log", false, false, "text/plain" ),

    ADDITIONAL_PIPELINE_CONFIGURATIONS( 4, "configurations" + File.separatorChar, "Additional pipeline configuration settings",
            ".pipeline.config.txt", true, false, "text/plain" );

    private final int id;
    private final String fileName;
    private final String displayName;
    private final String downloadName;
    private final boolean isDirectory;
    private final boolean isNamePrefixed;
    private final String contentType;

    /**
     * @param id             the identifier of the meta file type.
     * @param fileName       the name of the file in the filesystem.
     * @param downloadName   the string that will be used as the file name when the user downloads it. This name will always be
     *                       prefixed with the accession (sort name) of the experiment.
     * @param displayName    the string that will be displayed publicly to describe this file.
     * @param isDirectory    whether this file represents a directory.
     * @param isNamePrefixed whether the fileName has to be prefixed with the experiment accession name.
     */
    ExpressionExperimentMetaFileType( int id, String fileName, String displayName, String downloadName, Boolean isDirectory,
            Boolean isNamePrefixed, String contentType ) {
        this.id = id;
        this.displayName = displayName;
        this.fileName = fileName;
        this.downloadName = downloadName;
        this.isDirectory = isDirectory;
        this.isNamePrefixed = isNamePrefixed;
        this.contentType = contentType;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileName( ExpressionExperiment ee ) {
        return ( isNamePrefixed ? ee.getShortName() : "" ) + fileName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getContentType() {
        return contentType;
    }

    public String getDownloadName( ExpressionExperiment ee ) {
        return ee.getShortName() + downloadName;
    }
}
