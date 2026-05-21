/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration tests for the keyset (cursor) pagination DAO mechanism on
 * {@link ExpressionExperimentDao}. Verifies that
 * {@link ubic.gemma.persistence.service.FilteringVoEnabledDao#loadValueObjectsByCursor(
 * ubic.gemma.persistence.util.Filters, Sort, Cursor, int)} walks the result set in stable
 * id order without duplicates or skips, that cursor tokens round-trip through encode/decode,
 * and that the sort-must-end-in-id and matching-sort-spec invariants are enforced.
 *
 * @author phase3
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@WithMockUser(authorities = "GROUP_ADMIN")
public class ExpressionExperimentDaoCursorTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class ExpressionExperimentDaoCursorTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        // EE DAO now field-injects ArrayDesignDao for batched platform loads
        // (round-2 probe #8 fix). Wire the real DAO here.
        @Bean
        public ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private AclService aclService;

    private static final int N_FIXTURES = 5;

    private long[] createFixtureExperiments() {
        long[] ids = new long[N_FIXTURES];
        for ( int i = 0; i < N_FIXTURES; i++ ) {
            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            ee.setShortName( "fixture-" + i );
            ee.setName( "fixture " + i );
            ExpressionExperiment created = expressionExperimentDao.create( ee );
            // Filtering queries join through AclObjectIdentity even for admins, so the fixture is
            // invisible to the DAO without an AOI row. Create one per EE — same shape as the
            // createCharacteristic helper in the sister DAO test.
            aclService.createAcl( new AclObjectIdentity( ExpressionExperiment.class, created.getId() ) );
            ids[i] = created.getId();
        }
        return ids;
    }

    @Test
    public void testFirstPageEmitsNextCursor() {
        long[] ids = createFixtureExperiments();
        Sort byId = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );

        CursorPage<ExpressionExperimentValueObject> page1 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, null, 2 );

        assertThat( page1 ).hasSize( 2 );
        assertThat( page1.get( 0 ).getId() ).isEqualTo( ids[0] );
        assertThat( page1.get( 1 ).getId() ).isEqualTo( ids[1] );
        // first page must surface a forward token (more rows exist) but no backward token
        assertNotNull( page1.getNextCursor() );
        assertNull( page1.getPrevCursor() );
        // cursor mode does not count by default
        assertNull( page1.getTotalElements() );
    }

    @Test
    public void testCursorWalkProducesDisjointPages() {
        long[] ids = createFixtureExperiments();
        Sort byId = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );

        CursorPage<ExpressionExperimentValueObject> page1 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, null, 2 );
        Cursor afterPage1 = Cursor.decode( page1.getNextCursor() );
        CursorPage<ExpressionExperimentValueObject> page2 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, afterPage1, 2 );

        assertThat( page2 ).hasSize( 2 );
        // pages 1 and 2 share no ids
        assertThat( page2.stream().map( ExpressionExperimentValueObject::getId ) )
                .doesNotContain( ids[0], ids[1] )
                .containsExactly( ids[2], ids[3] );
        // page 2 emits both directions
        assertNotNull( page2.getNextCursor() );
        assertNotNull( page2.getPrevCursor() );
    }

    @Test
    public void testLastPageHasNoNextCursor() {
        long[] ids = createFixtureExperiments();
        Sort byId = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );

        // walk to the end: N_FIXTURES = 5, limit = 2 -> pages of [0,1], [2,3], [4]
        CursorPage<ExpressionExperimentValueObject> page1 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, null, 2 );
        Cursor c1 = Cursor.decode( page1.getNextCursor() );
        CursorPage<ExpressionExperimentValueObject> page2 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, c1, 2 );
        Cursor c2 = Cursor.decode( page2.getNextCursor() );
        CursorPage<ExpressionExperimentValueObject> page3 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, c2, 2 );

        assertThat( page3 ).hasSize( 1 );
        assertThat( page3.get( 0 ).getId() ).isEqualTo( ids[4] );
        // no forward token at the end of the collection
        assertNull( page3.getNextCursor() );
        // still has a backward token because we used a cursor to get here
        assertNotNull( page3.getPrevCursor() );
    }

    @Test
    public void testDescendingCursorWalk() {
        long[] ids = createFixtureExperiments();
        Sort byIdDesc = Sort.by( "ee", "id", Sort.Direction.DESC, Sort.NullMode.LAST, "id" );

        CursorPage<ExpressionExperimentValueObject> page1 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byIdDesc, null, 2 );
        assertThat( page1 ).hasSize( 2 );
        assertThat( page1.get( 0 ).getId() ).isEqualTo( ids[4] );
        assertThat( page1.get( 1 ).getId() ).isEqualTo( ids[3] );

        Cursor c1 = Cursor.decode( page1.getNextCursor() );
        CursorPage<ExpressionExperimentValueObject> page2 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byIdDesc, c1, 2 );
        assertThat( page2.stream().map( ExpressionExperimentValueObject::getId ) )
                .containsExactly( ids[2], ids[1] );
    }

    @Test
    public void testNonIdSortRejected() {
        // step 1b explicitly limits cursor pagination to the identifier property — anything else throws
        Sort byName = Sort.by( "ee", "name", Sort.Direction.ASC, Sort.NullMode.LAST, "name" );
        assertThatThrownBy(
                () -> expressionExperimentDao.loadValueObjectsByCursor( null, byName, null, 5 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "identifier property" );
    }

    @Test
    public void testCompoundSortRejected() {
        Sort compound = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" )
                .andThen( Sort.by( "ee", "name", Sort.Direction.ASC, Sort.NullMode.LAST, "name" ) );
        assertThatThrownBy(
                () -> expressionExperimentDao.loadValueObjectsByCursor( null, compound, null, 5 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "compound" );
    }

    @Test
    public void testMismatchedCursorSortSpecRejected() {
        createFixtureExperiments();
        Sort byIdAsc = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        // produce a cursor that says "+id" but submit it for a "-id" sort
        Cursor bogus = new Cursor( "-id", new Object[] { 1L }, Cursor.Direction.FORWARD );
        assertThatThrownBy(
                () -> expressionExperimentDao.loadValueObjectsByCursor( null, byIdAsc, bogus, 5 ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "sort spec" );
    }

    @Test
    public void testBackwardCursorReturnsPreviousPage() {
        long[] ids = createFixtureExperiments();
        Sort byId = Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );

        // forward to page 3
        CursorPage<ExpressionExperimentValueObject> page1 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, null, 2 );
        Cursor c1 = Cursor.decode( page1.getNextCursor() );
        CursorPage<ExpressionExperimentValueObject> page2 =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, c1, 2 );
        assertThat( page2.stream().map( ExpressionExperimentValueObject::getId ) ).containsExactly( ids[2], ids[3] );

        // backward from page 2 should yield page 1's rows in ascending id order
        Cursor back = Cursor.decode( page2.getPrevCursor() );
        CursorPage<ExpressionExperimentValueObject> backPage =
                expressionExperimentDao.loadValueObjectsByCursor( null, byId, back, 2 );
        assertThat( backPage.stream().map( ExpressionExperimentValueObject::getId ) ).containsExactly( ids[0], ids[1] );
    }
}
