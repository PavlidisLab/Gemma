package ubic.gemma.core.tasks;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.common.Identifiable;

/**
 * A simple task command that contains an entity ID and class.
 * @author poirigui
 */
public class EntityTaskCommand<T extends Identifiable> extends TaskCommand {

    private final Class<T> entityClass;
    private final Long entityId;

    public EntityTaskCommand( Class<T> entityClass, Long entityId ) {
        this.entityClass = entityClass;
        this.entityId = entityId;
    }

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public Long getEntityId() {
        return entityId;
    }
}
