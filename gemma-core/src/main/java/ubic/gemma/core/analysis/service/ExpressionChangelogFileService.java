package ubic.gemma.core.analysis.service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.util.Date;

/**
 * Service for reading and appending to changelog files.
 * <p>
 * External metadata files are not under Gemma's management, so the changes they go through is not kept track of in the
 * audit trail of an experiment. Instead, a changelog file is maintained under {@code ${gemma.appdata.home}/metadata/GSEnnnnn/CHANGELOG.md}.
 * @author poirigui
 */
public interface ExpressionChangelogFileService {

    String readChangelog( ExpressionExperiment expressionExperiment ) throws IOException;

    void appendToChangelog( ExpressionExperiment expressionExperiment, String text ) throws IOException;

    void appendToChangelog( ExpressionExperiment expressionExperiment, String text, Date date ) throws IOException;
}
