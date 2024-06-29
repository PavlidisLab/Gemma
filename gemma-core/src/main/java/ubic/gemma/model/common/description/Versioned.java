package ubic.gemma.model.common.description;

import ubic.gemma.core.lang.Nullable;

import java.net.URL;
import java.util.Date;

/**
 * Interface implemented by entities that are externally versioned.
 * <p>
 * This allows us to have a common set of attributes and audit events relating to the versioning of entities. Prominent
 * examples are {@link ExternalDatabase} and {@link ubic.gemma.model.expression.arrayDesign.ArrayDesign}.
 * <p>
 * These entities can be made auditable, in which case {@link ubic.gemma.model.common.auditAndSecurity.eventType.VersionedEvent}
 * can be used to represent events such as a new release, a genome patch update being applied, etc.
 * @author poirigui
 */
public interface Versioned {

    /**
     * The version of the release, if applicable.
     */
    @Nullable
    String getReleaseVersion();

    /**
     * External URL to the release, if applicable.
     */
    @Nullable
    URL getReleaseUrl();

    /**
     * The last updated date, if known.
     */
    @Nullable
    Date getLastUpdated();
}
