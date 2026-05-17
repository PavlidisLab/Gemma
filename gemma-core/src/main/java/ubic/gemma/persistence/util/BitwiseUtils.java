package ubic.gemma.persistence.util;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;

/**
 * Helpers for emitting portable native-SQL bitwise operations.
 * <p>
 * MySQL uses the {@code a & b} operator; H2 (in MODE=MYSQL or otherwise) needs the
 * {@code BITAND(a, b)} function — {@code &} either doesn't parse or behaves as concatenation in
 * some modes. Gemma is MySQL-only in production via {@code MySQL57Dialect}, but tests use H2 with
 * {@code MODE=MYSQL}, so native-SQL ACL queries that mask on permission bits need to render
 * differently per dialect.
 * <p>
 * Phase 2 background: pre-Phase-2 this routed through {@code Dialect.getSqlFunctionRegistry()},
 * which changed shape in Hibernate 6. Phase 2 Step 3 inlined {@code (a & b)} as MySQL-only; this
 * util restores dialect-awareness without going back through Hibernate's function registry.
 *
 * @author poirigui (Phase 2)
 */
public final class BitwiseUtils {

    private BitwiseUtils() {}

    /**
     * Emit a native-SQL bitwise-AND expression appropriate for the given Hibernate {@link Dialect}.
     *
     * @param dialect the runtime database dialect (e.g. {@code MySQL57InnoDBDialect}, {@code H2Dialect})
     * @param left    left-hand SQL expression (column reference, parameter, etc.)
     * @param right   right-hand SQL expression
     * @return SQL fragment, e.g. {@code (a & 1)} on MySQL or {@code BITAND(a, 1)} on H2
     */
    public static String bitand( Dialect dialect, String left, String right ) {
        if ( dialect instanceof H2Dialect ) {
            return "BITAND(" + left + ", " + right + ")";
        }
        // MySQL and forward-compat default
        return "(" + left + " & " + right + ")";
    }
}
