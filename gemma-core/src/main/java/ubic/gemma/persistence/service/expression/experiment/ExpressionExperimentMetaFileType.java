package ubic.gemma.persistence.service.expression.experiment;

import lombok.Getter;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.File;

/**
 * Types of metadata files that can be attached to an {@link ExpressionExperiment}.
 */
@Getter
public enum ExpressionExperimentMetaFileType {

    BASE_METADATA( 1, ".base.metadata", false, true, "Sequence analysis summary", ".seq.analysis.sum.txt", "text/plain" ),

    ALIGNMENT_METADATA( 2, ".alignment.metadata", false, true, "Alignment statistics", ".alignment.statistics.txt",
            "text/plain" ),

    /**
     * This is reserved for a future use by <a href="https://github.com/PavlidisLab/Gemma/pull/1530">#1529</a>.
     */
    DATA_PROCESSING_REPORT( 10, "dataProcessingReport.md", false, false,
            "Data Processing Report", ".data.processing.report.md", "text/markdown" ),

    // TODO: rename the directory to RNASeqPipelineMultiQCReports
    RNASEQ_PIPELINE_REPORT( 3, "MultiQCReports" + File.separatorChar + "multiqc_report.html", "RNA-Seq Pipeline Report",
            ".multiqc.report.html", 5, 6 ),
    RNASEQ_PIPELINE_REPORT_DATA( 5, "MultiQCReports" + File.separatorChar + "multiqc_data/multiqc_data.json", false, false,
            "RNA-Seq Pipeline Report (JSON metadata)",
            ".multiqc.data.json", "application/json" ),
    RNASEQ_PIPELINE_REPORT_LOG( 6, "MultiQCReports" + File.separatorChar + "multiqc_data/multiqc.log", false, false,
            "RNA-Seq Pipeline Report (log file)",
            ".multiqc.log", "text/plain" ),

    /**
     * @deprecated kept for backward-compatibility since we expose this for {@code addMetadataFile --file-type}.
     */
    @Deprecated
    MULTIQC_REPORT( 3, "MultiQCReports" + File.separatorChar + "multiqc_report.html", "RNA-Seq Pipeline Report",
            ".multiqc.report.html", 5, 6 ),

    CELL_TYPE_ANNOTATION_PIPELINE_REPORT( 7, "CellTypeAnnotationPipelineMultiQCReports" + File.separatorChar + "multiqc_report.html",
            "Cell Type Annotation Pipeline Report", ".multiqc.report.html",
            8, 9 ),
    CELL_TYPE_ANNOTATION_PIPELINE_REPORT_DATA( 8,
            "CellTypeAnnotationPipelineMultiQCReports" + File.separatorChar + "multiqc_data.json", false, false,
            "Cell Type Annotation Pipeline Report (JSON metadata)", ".multiqc.data.json", "application/json" ),
    CELL_TYPE_ANNOTATION_PIPELINE_REPORT_LOG( 9,
            "CellTypeAnnotationPipelineMultiQCReports" + File.separatorChar + "multiqc.log", false, false,
            "Cell Type Annotation Pipeline Report (log file)", ".multiqc.log", "text/plain" ),

    ADDITIONAL_PIPELINE_CONFIGURATIONS( 4, "configurations" + File.separatorChar, true, false, "Additional pipeline configuration settings",
            ".pipeline.config.txt", "text/plain" );

    private final int id;
    private final String fileName;
    private final boolean isDirectory;
    private final boolean isNamePrefixed;
    private final String displayName;
    private final String downloadName;
    private final String contentType;
    private final boolean isMultiQC;
    private final int multiqcDataTypeId;
    private final int multiqcLogTypeId;

    /**
     * @param id             the identifier of the meta file type.
     * @param fileName       the name of the file in the filesystem.
     * @param isDirectory    whether this file represents a directory.
     * @param isNamePrefixed whether the fileName has to be prefixed with the experiment accession name.
     * @param displayName    the string that will be displayed publicly to describe this file.
     * @param downloadName   the string that will be used as the file name when the user downloads it. This name will always be
     *                       prefixed with the accession (sort name) of the experiment.
     */
    ExpressionExperimentMetaFileType( int id, String fileName, boolean isDirectory, boolean isNamePrefixed, String displayName, String downloadName,
            String contentType ) {
        this.id = id;
        this.fileName = fileName;
        this.isDirectory = isDirectory;
        this.isNamePrefixed = isNamePrefixed;
        this.displayName = displayName;
        this.downloadName = downloadName;
        this.contentType = contentType;
        this.isMultiQC = false;
        this.multiqcDataTypeId = -1;
        this.multiqcLogTypeId = -1;
    }

    /**
     * Constructor for MultiQC reports.
     */
    ExpressionExperimentMetaFileType( int id, String fileName, String displayName, String downloadName,
            int multiqcDataTypeId, int multiqcLogTypeId ) {
        this.id = id;
        this.fileName = fileName;
        this.isDirectory = false;
        this.isNamePrefixed = false;
        this.displayName = displayName;
        this.downloadName = downloadName;
        this.contentType = "text/html";
        this.isMultiQC = true;
        this.multiqcDataTypeId = multiqcDataTypeId;
        this.multiqcLogTypeId = multiqcLogTypeId;
    }


    public String getFileName( ExpressionExperiment ee ) {
        return ( isNamePrefixed ? ee.getShortName() : "" ) + fileName;
    }

    public String getDownloadName( ExpressionExperiment ee ) {
        return ee.getShortName() + downloadName;
    }

    public ExpressionExperimentMetaFileType getMultiQCDataFileType() {
        Assert.isTrue( multiqcDataTypeId != -1 );
        for ( ExpressionExperimentMetaFileType type : ExpressionExperimentMetaFileType.values() ) {
            if ( type.id == multiqcDataTypeId ) {
                return type;
            }
        }
        throw new IllegalStateException();
    }

    public ExpressionExperimentMetaFileType getMultiQCLogFileType() {
        Assert.isTrue( multiqcLogTypeId != -1 );
        for ( ExpressionExperimentMetaFileType type : ExpressionExperimentMetaFileType.values() ) {
            if ( type.id == multiqcLogTypeId ) {
                return type;
            }
        }
        throw new IllegalStateException();
    }
}
