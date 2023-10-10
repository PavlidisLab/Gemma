package ubic.gemma.persistence.hibernate;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.EnumType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Workaround to handle existing batch effect value in the database.
 * <p>
 * FIXME: remove this once the 1.31 is out and the database has been fully migrated (see <a href="https://github.com/PavlidisLab/Gemma/issues/894">#894</a> for details).
 * @author poirigui
 */
public class BatchEffectType extends EnumType {

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, SessionImplementor session, Object owner ) throws SQLException {
        String value = rs.getString( names[0] );
        if ( value != null && value.startsWith( "This data set may have a batch artifact" ) ) {
            return ubic.gemma.model.expression.experiment.BatchEffectType.NO_BATCH_INFO;
        }
        return super.nullSafeGet( rs, names, session, owner );
    }
}
