package ubic.gemma.persistence.hibernate;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers binary-aware HQL functions for working with BLOB-mapped columns.
 * <p>
 * Hibernate 6 strictly type-checks HQL function arguments. The built-ins {@code substring()} and
 * {@code character_length()} / {@code length()} are registered as STRING-only, so calling them on a column whose
 * Java type is a {@code byte[]} / {@code int[]} mapped to BLOB (e.g. via Gemma's {@code ByteArrayType}) now throws
 * {@code FunctionArgumentException}. Hibernate 5 was lenient and just emitted the SQL as written.
 * <p>
 * Two replacements live here, both rendered as plain SQL on every dialect Gemma targets (MySQL: {@code SUBSTRING},
 * {@code LENGTH} on a BLOB returns byte length; H2: same):
 *
 * <ul>
 *     <li>{@code bytes_substring(b, start, length)} → {@code substring(?1, ?2, ?3)} — byte-range slice of a BLOB,
 *         returning a BLOB.</li>
 *     <li>{@code bytes_length(b)} → {@code length(?1)} — number of bytes in a BLOB, returning a long.</li>
 * </ul>
 * <p>
 * Wired in via {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class BinaryFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions( FunctionContributions functionContributions ) {
        functionContributions.getFunctionRegistry().registerPattern(
                "bytes_substring",
                "substring(?1, ?2, ?3)" );
        functionContributions.getFunctionRegistry().registerPattern(
                "bytes_length",
                "length(?1)",
                functionContributions.getTypeConfiguration()
                        .getBasicTypeRegistry()
                        .resolve( StandardBasicTypes.LONG ) );
    }
}
