/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.persistence.util;

import org.hibernate.Session;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * HB6 SQM-translator regression guard. Each test pins one of the known-fragile
 * HQL shapes that was previously rewritten in the {@code HQL_SQL_AUDIT} series
 * (residuals tracked under B2 in {@code STATUS_HQL_SQL_AUDIT_RESIDUALS.md}).
 * <p>
 * The intent is NOT to assert on row content. Each probe issues the production
 * HQL verbatim against an empty H2 schema and asserts the query parses and
 * executes — i.e. the Semantic Query Model translator accepts the shape and
 * emits valid SQL. The previous failure mode for shape 1 was a Hibernate
 * internal {@code AssertionError} thrown from
 * {@code BaseSqmToSqlAstConverter.visitTableGroup} at translation time, well
 * before any rows were touched. Empty result sets exercise the same code path.
 * <p>
 * If a future Hibernate upgrade changes SQM translation semantics in a way that
 * breaks any of these shapes, this test goes red instead of the regression
 * surfacing as a runtime exception in production.
 *
 * @see ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl
 */
@Tag("integration")
@ContextConfiguration
public class HibernateSqmFragileShapesIT extends BaseDatabaseTest5 {

    /**
     * Shape 1 (B2): multi-collection join with a correlated subquery driving
     * from {@link BioAssayDimension}.
     * <p>
     * The original HQL form ({@code from BAD b, EE e join b.bioAssays bba join
     * e.bioAssays eb where eb = bba}) was a cross-join from two roots with
     * collection joins from each — HB6's SQM translator hit an internal
     * {@code AssertionError} at {@code BaseSqmToSqlAstConverter.visitTableGroup}
     * on that shape. The rewrite at {@code ExpressionExperimentDaoImpl.java:1547-1551}
     * drives from BAD with a subquery; this probe pins the rewrite shape.
     */
    @Test
    public void probe_subqueryOverCollectionJoin() {
        Session session = sessionFactory.getCurrentSession();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( -1L );
        assertDoesNotThrow( () -> {
            //noinspection unchecked
            assertNotNull( session.createQuery( "select distinct b from BioAssayDimension b join b.bioAssays bba "
                            + "where bba.id in (select eb.id from ExpressionExperiment e join e.bioAssays eb where e = :ee)" )
                    .setParameter( "ee", ee )
                    .list() );
        } );
    }

    /**
     * Shape 2 (P7): implicit polymorphism over the unmapped
     * {@code BulkExpressionDataVector} base class. HB resolves the query
     * against every mapped concrete subclass (RawExpressionDataVector,
     * ProcessedExpressionDataVector, ...) under the hood. Translator changes
     * around polymorphic resolution would surface here.
     */
    @Test
    public void probe_implicitPolymorphismOnUnmappedBase() {
        Session session = sessionFactory.getCurrentSession();
        assertDoesNotThrow( () -> {
            //noinspection unchecked
            assertNotNull( session.createQuery( "select distinct v.bioAssayDimension from BulkExpressionDataVector v "
                            + "where v.expressionExperiment.id = :eeId and v.quantitationType.id = :qtId" )
                    .setParameter( "eeId", -1L )
                    .setParameter( "qtId", -1L )
                    .list() );
        } );
    }

    /**
     * Shape 3: multi-association entity projection with {@code group by} on
     * an entity reference. Mirrors {@code ExpressionExperimentDaoImpl.findByFactor}
     * at line 534-542 — two collection joins ({@code ee.experimentalDesign ed
     * join ed.experimentalFactors ef}) plus a {@code group by ee} that
     * groups by an entity, not a scalar. HB6 lifts entity-typed
     * {@code group by} into the underlying PK column in SQL; translator
     * changes around that lift would surface here.
     */
    @Test
    public void probe_groupByEntityOverChainedCollectionJoins() {
        Session session = sessionFactory.getCurrentSession();
        assertDoesNotThrow( () -> {
            //noinspection unchecked
            assertNotNull( session.createQuery( "select ee from ExpressionExperiment as ee "
                            + "join ee.experimentalDesign ed "
                            + "join ed.experimentalFactors ef "
                            + "where ef.id = :efId "
                            + "group by ee" )
                    .setParameter( "efId", -1L )
                    .list() );
        } );
    }

    @org.springframework.context.annotation.Configuration
    @ubic.gemma.core.context.TestComponent
    static class Config extends BaseDatabaseTestContextConfiguration {
    }
}
