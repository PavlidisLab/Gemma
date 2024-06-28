package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;

public class StatementUtils {

    /**
     * Test if the given statement has the given subject.
     */
    public static boolean hasSubject( Statement statement, Characteristic subject ) {
        return CharacteristicUtils.equals( statement.getSubject(), statement.getSubjectUri(), subject.getValue(), subject.getValueUri() );
    }
}
