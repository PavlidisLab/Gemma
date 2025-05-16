package ubic.gemma.core.analysis.service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Service for reading and appending to changelog files.
 * <p>
 * External metadata files are not under Gemma's management, so the changes they go through is not kept track of in the
 * {@link ubic.gemma.model.common.auditAndSecurity.AuditTrail} of an experiment. Instead, a changelog file is maintained
 * under {@code ${gemma.appdata.home}/metadata/GSEnnnnn/CHANGELOG.md}.
 * <p>
 * The format of the changelog follows that of <a href="https://www.gnu.org/prep/standards/html_node/Style-of-Change-Logs.html#Style-of-Change-Logs">GNU</a>.
 * @author poirigui
 */
public interface ExpressionMetadataChangelogFileService {

    /**
     * Read the content of the changelog file of the given experiment.
     * <p>
     * If no changelog file is found, an empty string is returned.
     */
    String readChangelog( ExpressionExperiment expressionExperiment ) throws IOException;

    /**
     * @see #addChangelogEntry(ExpressionExperiment, String, LocalDate)
     */
    void addChangelogEntry( ExpressionExperiment expressionExperiment, String changelogEntry ) throws IOException;

    /**
     * Add a changelog entry to the changelog file of the given experiment.
     * @param expressionExperiment the experiment to add the entry to
     * @param changelogEntry       the content of the entry, the date and author are automatically added
     * @param date                 the date of the entry. This is a local date because the changelog format does not
     *                             include a timezone.
     */
    void addChangelogEntry( ExpressionExperiment expressionExperiment, String changelogEntry, LocalDate date ) throws IOException;
}
