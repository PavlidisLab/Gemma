package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the SQL Hibernate issues to load a {@link SampleCoexpressionMatrix} by id: the statement that selects the
 * blob joins nothing.
 * <p>
 * A joined to-many multiplies result <em>rows</em>, and every row carries every selected column. While
 * {@code bioAssayDimension} was {@code @Fetch(JOIN)}, the by-id loader was
 * <pre>
 * select ..., scm.COEXPRESSION_MATRIX from SAMPLE_COEXPRESSION_MATRIX scm
 *   join BIO_ASSAY_DIMENSION bad ... left join BIO_ASSAY_DIMENSIONS2BIO_ASSAYS ... left join BIO_ASSAY ...
 *   where scm.ID in (?, ...)
 * </pre>
 * because {@code BioAssayDimension.bioAssays} is EAGER and rides along on the join. That is one row per bioassay,
 * each with the whole n-squared LONGBLOB: for 1,090 samples, 1,090 copies of a 9.5 MB matrix on the wire to read
 * one. The storage engine reads the matrix row once, so table I/O counters show a single fetch.
 * <p>
 * The assertion is on the statement and not on a row count so that it needs no fixture; the row multiplication
 * follows from the join.
 */
@ContextConfiguration
public class SampleCoexpressionMatrixLoadSqlTest extends BaseDatabaseTest5 {

    @Test
    public void theStatementThatSelectsTheBlobJoinsNothing() {
        List<String> statements = new ArrayList<>();
        try ( Session session = sessionFactory.withOptions().statementInspector( sql -> {
            statements.add( sql );
            return sql;
        } ).openSession() ) {
            session.get( SampleCoexpressionMatrix.class, -1L );
        }
        assertThat( statements )
                .filteredOn( sql -> sql.contains( ".COEXPRESSION_MATRIX" ) )
                .hasSize( 1 )
                .allSatisfy( sql -> assertThat( sql ).doesNotContain( " join " ) );
    }

    @Configuration
    @TestComponent
    static class Config extends BaseDatabaseTestContextConfiguration {
    }
}
