/*
 * The Gemma project.
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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;

import org.springframework.lang.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-MySQL counterpart of the H2-backed {@link CharacteristicDaoTest}, pinning the native-query
 * methods of {@link CharacteristicDaoImpl} against the production Spring context (real MySQL
 * {@code gemdtest} via {@link BaseIntegrationTest5}).
 * <p>
 * The native queries all project {@code select {C.*} from CHARACTERISTIC as C ... } with
 * {@code .addEntity( "C", Characteristic.class )}. Because {@link Characteristic} uses
 * {@code SINGLE_TABLE} inheritance with {@code @DiscriminatorColumn(name = "class")},
 * {@code @DiscriminatorValue("null")} on the root, and a {@link Statement} subclass, the
 * {@code {C.*}} alias expansion has to emit the discriminator {@code class} column so Hibernate
 * can choose the right {@code EntityPersister} per row. On MySQL this surfaces as
 * <pre>Unable to find column position by name: class [Column 'class' not found.]</pre>
 * which H2 (MODE=MYSQL, the {@link CharacteristicDaoTest} backend) does not reproduce because
 * H2 and MySQL diverge on how the {@code {alias.*}} discriminator expansion is rendered.
 * <p>
 * Each test seeds a mix of plain {@link Characteristic} rows (null discriminator) and at least
 * one {@link Statement} row (non-null discriminator) sharing the same probe URI / value / category,
 * then asserts the DAO call RETURNS (does not throw) and includes the {@link Statement} subclass
 * row. {@code findByUri} is expected to throw on MySQL today — that's the bug this test pins; do
 * NOT weaken the assertion to dodge it.
 * <p>
 * Class-level {@link Transactional} opens a per-test transaction Spring rolls back at end-of-test,
 * so the seeded rows never pollute the shared {@code gemdtest} schema — no manual cleanup needed.
 */
@Transactional
public class CharacteristicDaoMySqlIntegrationTest extends BaseIntegrationTest5 {

    @Autowired
    private CharacteristicDao characteristicDao;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ubic.gemma.core.util.test.TestAuthenticationUtils testAuthenticationUtils;

    @Autowired
    private org.springframework.security.acls.model.MutableAclService aclService;

    @Autowired
    private ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil tableMaintenanceUtil;

    /** Experiment carrying {@link #eeValueUri}, published to anonymous so every ACL branch sees it. */
    private Long probeExperimentId;

    /**
     * The count probes deliberately use their OWN uri / value / category rather than the ones the
     * findBy* tests above assert on. Those tests pin an exact {@code hasSize( 3 )} over the three
     * seeded characteristics, so annotating an experiment with the same uri or category would make
     * it four and break them for a reason having nothing to do with what they test.
     */
    private String eeValueUri;
    private String eeCategoryUri;
    private String eeCategory;

    /**
     * Per-run unique token so the probe URI / value / category don't collide with pre-existing
     * gemdtest rows (the @Transactional rollback keeps these out of the persistent schema, but a
     * unique token keeps the assertions exact regardless of what else is in the table).
     */
    private String token;
    private String valueUri;
    private String valuePrefix;
    private String categoryUri;
    private String category;

    @BeforeEach
    public void setUp() {
        token = UUID.randomUUID().toString();
        valueUri = "http://test/CHAR_DAO_IT/" + token;
        valuePrefix = "char_dao_it_" + token;
        categoryUri = "http://test/CHAR_DAO_IT_CAT/" + token;
        category = "char dao it cat " + token;

        // plain Characteristic (null discriminator) carrying the probe value-URI
        Characteristic plain = createCharacteristic( category, categoryUri, valuePrefix + "_plain", valueUri );
        sessionFactory.getCurrentSession().persist( plain );

        // a second plain Characteristic with the same value-URI to exercise multi-row return
        Characteristic plain2 = createCharacteristic( category, categoryUri, valuePrefix + "_plain2", valueUri );
        sessionFactory.getCurrentSession().persist( plain2 );

        // a Statement (non-null "Statement" discriminator) sharing the same value-URI / value-like
        // prefix / category — this is the row that forces the {C.*} discriminator expansion that
        // breaks on MySQL. Statement.subject == Characteristic.VALUE, subjectUri == VALUE_URI.
        Statement statement = Statement.Factory.newInstance( category, categoryUri, valuePrefix + "_stmt", valueUri );
        sessionFactory.getCurrentSession().persist( statement );

        sessionFactory.getCurrentSession().flush();

        // An experiment actually annotated with the probe URI, so the count assertions below have
        // a number to compare rather than two empty maps. gemdtest is rebuilt empty by the schema
        // reset, so without this the parity check passes vacuously and would keep passing if the
        // aggregate returned nothing at all.
        eeValueUri = "http://test/CHAR_DAO_IT_EE/" + token;
        eeCategoryUri = "http://test/CHAR_DAO_IT_EE_CAT/" + token;
        // must NOT share a 12-character prefix with `category`: testFindByCategoryLike probes
        // with category.substring( 0, 12 ) + "%", which "char dao it ..." would match.
        eeCategory = "ee probe cat " + token;

        ubic.gemma.model.genome.Taxon taxon = new ubic.gemma.model.genome.Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "EE_" + token );
        ee.setName( "char dao it ee " + token );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( createCharacteristic( eeCategory, eeCategoryUri, "char_dao_it_ee_" + token, eeValueUri ) );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        probeExperimentId = ee.getId();

        // READ for anonymous: it sets the denormalised mask the anonymous branch reads AND
        // satisfies the "publicly available" arm the logged-in branch reads, so one grant exercises
        // all three ACL shapes against the same expected number.
        //
        // readAclById, not createAcl: Gemma's ACL advice already minted an object identity when the
        // experiment was persisted, so creating one here throws AlreadyExists.
        org.springframework.security.acls.model.MutableAcl acl = ( org.springframework.security.acls.model.MutableAcl )
                aclService.readAclById( new ubic.gemma.core.security.acl.domain.AclObjectIdentity( ExpressionExperiment.class, ee.getId() ) );
        acl.insertAce( acl.getEntries().size(), org.springframework.security.acls.domain.BasePermission.READ,
                new org.springframework.security.acls.domain.GrantedAuthoritySid(
                        org.springframework.security.access.vote.AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ), true );
        aclService.updateAcl( acl );

        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    public void testFindByUri_returnsPlainAndStatement() {
        // THE confirmed-broken path: on MySQL the {C.*} discriminator expansion renders a `class`
        // column reference the JDBC result-set lookup can't resolve. Expected to throw today.
        Collection<Characteristic> results = characteristicDao.findByUri( valueUri, null, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByValueLike_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByValueLike( valuePrefix + "%", null, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByCategoryLike_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByCategoryLike( category.substring( 0, 12 ) + "%", null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByCategoryUri_returnsPlainAndStatement() {
        Collection<Characteristic> results = characteristicDao.findByCategoryUri( categoryUri, null, true, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    @Test
    public void testFindByParentClasses_returnsPlainAndStatement() {
        // findByParentClasses runs the line-151 native query: select {C.*} from CHARACTERISTIC as C
        // where <owning-entity-constraint> [and <category-constraint>]. includeNoParents=true with
        // null parentClasses + the per-row category filter keeps the result scoped to our seeded
        // rows (all three are parent-less, so they satisfy the includeNoParents arm).
        Collection<Characteristic> results = characteristicDao.findByParentClasses( null, true, category, -1 );
        assertThat( results )
                .hasSize( 3 )
                .anyMatch( c -> c instanceof Statement )
                .anyMatch( c -> !( c instanceof Statement ) );
    }

    // ---------------------------------------------------------------------------------------
    // countExperimentsByUris — the aggregate behind /annotations/search usage counts.
    //
    // H2 cannot answer the question these tests ask. The query is a derived table over a UNION ALL
    // of per-column range scans with count(distinct) applied outside it, and the ACL restriction is
    // spliced into every arm INSIDE that derived table — three constructs whose rendering is exactly
    // where this class has already caught H2 and MySQL disagreeing once. Each ACL branch produces a
    // structurally different query, so each one needs its own execution:
    //
    //   anonymous       -> a bitwise-AND on the denormalised mask column   (dialect-rendered)
    //   logged-in       -> `and exists (select 1 from acl_object_identity ...)` correlated subquery
    //   admin           -> no restriction at all
    //
    // The assertion is agreement with the row-returning method these callers used to sum in Java,
    // not a fixed number: gemdtest's corpus is whatever it is, and a literal would either be wrong
    // or force seeding EE2C, which the transactional rollback makes awkward. Agreement is the
    // property that has to hold, and it holds whether the tally is empty or not.
    // ---------------------------------------------------------------------------------------

    /** URIs worth probing: the seeded one plus a couple that real corpora tend to carry. */
    private Collection<String> countProbeUris() {
        return Arrays.asList( eeValueUri,
                "http://purl.obolibrary.org/obo/CL_0000236",
                "http://www.ebi.ac.uk/efo/EFO_0000322" );
    }

    @Test
    public void testCountExperimentsByUrisAsAnonymous() {
        // BaseIntegrationTest5 authenticates every test as admin in a @BeforeEach, and an admin
        // gets NO acl clause at all -- so the branch has to be selected explicitly or this test
        // silently re-runs the admin one. @WithMockUser cannot do it here: this class does not
        // register WithSecurityContextTestExecutionListener, and the base @BeforeEach would
        // overwrite the context afterwards regardless.
        testAuthenticationUtils.runAsAnonymous();
        assertThat( ubic.gemma.core.security.util.SecurityUtil.isUserAnonymous() ).isTrue();
        assertThat( characteristicDao.countExperimentsByUris( countProbeUris(), true, true, true, null, Collections.emptySet() ) )
                .containsEntry( eeValueUri, 1L )
                .isEqualTo( distinctEeCountsTheOldWay( countProbeUris() ) );
    }

    @Test
    public void testCountExperimentsByUrisAsLoggedInUser() {
        // the branch that puts a correlated EXISTS over the acl_* tables inside the derived table
        testAuthenticationUtils.runAsUser( "bob", true );
        assertThat( ubic.gemma.core.security.util.SecurityUtil.isUserAdmin() ).isFalse();
        assertThat( ubic.gemma.core.security.util.SecurityUtil.isUserAnonymous() ).isFalse();
        assertThat( characteristicDao.countExperimentsByUris( countProbeUris(), true, true, true, null, Collections.emptySet() ) )
                .containsEntry( eeValueUri, 1L )
                .isEqualTo( distinctEeCountsTheOldWay( countProbeUris() ) );
    }

    @Test
    public void testCountExperimentsByUrisAsAdminWithExclusions() {
        assertThat( ubic.gemma.core.security.util.SecurityUtil.isUserAdmin() ).isTrue();
        assertThat( characteristicDao.countExperimentsByUris( countProbeUris(), true, true, true, null, Collections.emptySet() ) )
                .containsEntry( eeValueUri, 1L )
                .isEqualTo( distinctEeCountsTheOldWay( countProbeUris() ) );
        // binds the second parameter list, which changes the shape of every arm's WHERE clause
        assertThat( characteristicDao.countExperimentsByUris( countProbeUris(), true, true, true, null,
                new HashSet<>( Arrays.asList( -1L, -2L ) ) ) )
                .as( "excluding ids nothing uses cannot change the tally" )
                .isEqualTo( distinctEeCountsTheOldWay( countProbeUris() ) );
        assertThat( characteristicDao.countExperimentsByUris( countProbeUris(), true, true, true, null,
                Collections.singleton( probeExperimentId ) ) )
                .as( "excluding the only experiment using the term drops it from the tally" )
                .doesNotContainKey( eeValueUri );
    }

    /** The tally as the callers computed it before the aggregate existed. */
    private Map<String, Long> distinctEeCountsTheOldWay( Collection<String> uris ) {
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits =
                characteristicDao.findExperimentReferencesByUris( uris, true, true, true, null, -1, false );
        Map<String, Set<Long>> distinctIdsByUri = new HashMap<>();
        for ( Map<String, Set<ExpressionExperiment>> perClass : hits.values() ) {
            for ( Map.Entry<String, Set<ExpressionExperiment>> entry : perClass.entrySet() ) {
                Set<Long> bucket = distinctIdsByUri.computeIfAbsent( entry.getKey(), k -> new HashSet<>() );
                for ( ExpressionExperiment ee : entry.getValue() ) {
                    bucket.add( ee.getId() );
                }
            }
        }
        Map<String, Long> counts = new HashMap<>();
        distinctIdsByUri.forEach( ( k, v ) -> {
            if ( !v.isEmpty() ) {
                counts.put( k, ( long ) v.size() );
            }
        } );
        return counts;
    }

    private Characteristic createCharacteristic( @Nullable String category, @Nullable String categoryUri, String value, @Nullable String valueUri ) {
        Characteristic c = new Characteristic();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }
}
