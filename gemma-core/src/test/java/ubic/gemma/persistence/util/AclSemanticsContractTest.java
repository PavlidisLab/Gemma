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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.core.util.test.TestAuthenticationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Contract harness for the planned rewrite of
 * {@link AclQueryUtils#formAclRestrictionClause(String)} (and its sibling
 * {@code formNativeAclJoinClause} / {@code formNativeAclRestrictionClause})
 * from the current row-multiplying JOIN strategy to a correlated EXISTS
 * subquery.
 * <p>
 * The intent is that every DAO callsite of the ACL clause is exercised here
 * from each of the three principals (anonymous, authenticated non-admin,
 * admin), producing a deterministic fingerprint (count + sorted id list)
 * of the result set. The test class is run twice: once against the current
 * JOIN-based ACL clause, once against the rewritten EXISTS-based clause.
 * Diff zero is the green bar. Any drift between the two runs would be a
 * silent security regression (data leak or improper restriction), which is
 * why this scaffolding ships ahead of the refactor itself.
 * <p>
 * <b>Current state — disabled.</b> The harness is intentionally skeletal:
 * tests enumerate the callsites and principals but every test is
 * {@link Disabled} pending the seeding of a mixed-ACL fixture (a small
 * curated set of EEs/ADs spanning the four ACL situations: public,
 * private-owned, private-shared, and admin-only). The fixture work and
 * the un-disabling happen in a follow-up batch. See
 * {@code project_acl_exists_refactor.md} (memory note) for the full plan.
 * <p>
 * <b>Callsite enumeration.</b> The {@link #CALLSITES} list captures every
 * place in {@code gemma-core/src/main/java} that calls one of the three
 * ACL clause-forming helpers. Each entry pairs a stable human-readable
 * description with a {@link Supplier} that, once fixture data is in place,
 * will invoke the underlying DAO method and return a result handle from
 * which a fingerprint can be computed. The list size (35) intentionally
 * exceeds the original 27-callsite estimate in the planning doc: a number
 * of {@code resolveFilterablePropertyMeta} branches and the
 * {@code populateExpressionExperimentCount} family contribute extra
 * native-SQL callsites beyond the original HQL count.
 *
 * @see AclQueryUtils#formAclRestrictionClause(String)
 * @see EE2CAclQueryUtils#formNativeAclJoinClause(String)
 */
public class AclSemanticsContractTest extends BaseIntegrationTest5 {

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * Stable identifier for the three principals the ACL clause discriminates
     * against. The expected row-count relation is
     * {@code anonymous <= authenticatedUser <= admin}; the contract is that
     * <em>each principal's count and id list is byte-identical between the
     * before- and after-refactor runs</em>, not that the three principals
     * agree among themselves.
     */
    enum Principal {
        ANONYMOUS,
        AUTHENTICATED_USER,
        ADMIN
    }

    /**
     * One ACL callsite from {@code gemma-core/src/main/java}. The
     * {@link #description} is a stable label (DAO + method + line hint) and
     * {@link #invoker} is a closure that — once the mixed-ACL fixture is
     * seeded — will exercise the underlying DAO method. For now invokers are
     * placeholders; the tests they back are {@link Disabled}.
     */
    static final class Callsite {
        final String description;
        final Supplier<Object> invoker;

        Callsite( String description, Supplier<Object> invoker ) {
            this.description = description;
            this.invoker = invoker;
        }
    }

    /**
     * Every ACL callsite under {@code gemma-core/src/main/java}, as enumerated
     * from a grep for {@code formAclRestrictionClause},
     * {@code formNativeAclJoinClause}, and {@code formNativeAclRestrictionClause}
     * (excluding the definitions in {@code AclQueryUtils} /
     * {@code EE2CAclQueryUtils} themselves). Line numbers reference the
     * source as of commit {@code a5701f0e} — they will drift; the
     * description plus the DAO + method name is the stable handle.
     */
    static final List<Callsite> CALLSITES = Collections.unmodifiableList( Arrays.asList(
            // --- BibliographicReferenceDaoImpl ---
            new Callsite( "BibliographicReferenceDaoImpl.countDistinctWithRelatedExperiments [L75]",
                    () -> null ),
            new Callsite( "BibliographicReferenceDaoImpl.countWithRelatedExperiments [L87]",
                    () -> null ),
            new Callsite( "BibliographicReferenceDaoImpl.getRelatedExperiments(int,int) [L96]",
                    () -> null ),
            new Callsite( "BibliographicReferenceDaoImpl.getRelatedExperiments(Collection) [L123]",
                    () -> null ),

            // --- CharacteristicDaoImpl ---
            new Callsite( "CharacteristicDaoImpl.findExperimentReferencesByUris (native ACL join) [L241]",
                    () -> null ),
            new Callsite( "CharacteristicDaoImpl.findExperimentReferencesByUris (native ACL restriction) [L245]",
                    () -> null ),

            // --- CompositeSequenceDaoImpl ---
            new Callsite( "CompositeSequenceDaoImpl.getFilteringQuery [L95]",
                    () -> null ),
            new Callsite( "CompositeSequenceDaoImpl.getFilteringIdQuery [L115]",
                    () -> null ),
            new Callsite( "CompositeSequenceDaoImpl.getFilteringCountQuery [L129]",
                    () -> null ),

            // --- ArrayDesignDaoImpl ---
            new Callsite( "ArrayDesignDaoImpl.countExpressionExperiments [L518]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.getPerTaxonCount [L530]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.countSwitchedExpressionExperiments [L562]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.getFilteringCountQuery [L1079]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.populateExpressionExperimentCount (native ACL join) [L1165]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.populateExpressionExperimentCount (native ACL restriction) [L1168]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.populateSwitchedExpressionExperimentCount (native ACL join) [L1191]",
                    () -> null ),
            new Callsite( "ArrayDesignDaoImpl.populateSwitchedExpressionExperimentCount (native ACL restriction) [L1196]",
                    () -> null ),

            // --- FactorValueDaoImpl ---
            new Callsite( "FactorValueDaoImpl.loadAll(int,int) [L82]",
                    () -> null ),
            new Callsite( "FactorValueDaoImpl.loadAllIds() [L97]",
                    () -> null ),
            new Callsite( "FactorValueDaoImpl.loadAllIds(int,int) data query [L109]",
                    () -> null ),
            new Callsite( "FactorValueDaoImpl.loadAllIds(int,int) count query [L124]",
                    () -> null ),
            new Callsite( "FactorValueDaoImpl.findByValueStartingWith [L134]",
                    () -> null ),

            // --- GeneDaoImpl ---
            new Callsite( "GeneDaoImpl.getCompositeSequenceCount [L228]",
                    () -> null ),
            new Callsite( "GeneDaoImpl.getCompositeSequenceCountById [L247]",
                    () -> null ),

            // --- ExpressionExperimentDaoImpl ---
            new Callsite( "ExpressionExperimentDaoImpl.loadAllIdentifiers [L240]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getCategoriesUsageFrequency (native ACL join) [L916]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getCategoriesUsageFrequency (native ACL restriction) [L934]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getAnnotationsUsageFrequencyInternal (native ACL join) [L1079]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getAnnotationsUsageFrequencyInternal (native ACL restriction) [L1118]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getTechnologyTypeUsageFrequency (native ACL join) [L1335]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getTechnologyTypeUsageFrequency (native ACL restriction) [L1337]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getOriginalPlatformsUsageFrequency (native ACL join) [L1399]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getOriginalPlatformsUsageFrequency (native ACL restriction) [L1403]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getPerTaxonCount [L1690]",
                    () -> null ),
            new Callsite( "ExpressionExperimentDaoImpl.getFilteringCountQuery [L3993]",
                    () -> null )
    ) );

    /**
     * Generates one {@link DynamicTest} per (callsite, principal) pair. With
     * 35 callsites and 3 principals this produces 105 tests. Each test
     * switches the {@link SecurityContextHolder} to the right principal,
     * invokes the callsite, and (once the fixture is in place) records the
     * fingerprint. Until then every dynamic test is wrapped to surface as
     * disabled-via-assumption so the contract is visible in the test report
     * without yet being a green bar.
     */
    @TestFactory
    Stream<DynamicTest> aclContract_acrossAllCallsitesAndPrincipals() {
        List<DynamicTest> tests = new ArrayList<>( CALLSITES.size() * Principal.values().length );
        for ( Callsite cs : CALLSITES ) {
            for ( Principal principal : Principal.values() ) {
                String name = cs.description + " :: " + principal.name();
                tests.add( DynamicTest.dynamicTest( name, () -> runOne( cs, principal ) ) );
            }
        }
        return tests.stream();
    }

    /**
     * Disabled annotation marker for an additional @Test placeholder — keeps
     * IDE / Surefire reports honest about "this class is intentionally
     * not exercising the contract yet".
     */
    @org.junit.jupiter.api.Test
    @Disabled( "fixture data not yet seeded — see project_acl_exists_refactor.md" )
    void placeholder_untilMixedAclFixtureIsSeeded() {
        // Intentionally empty.
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private void runOne( Callsite cs, Principal principal ) {
        // The contract is genuine but the data is missing: skip the dynamic
        // test via JUnit 5 abort. Surfaces as "skipped" in reports, which is
        // the right signal until the fixture lands.
        org.junit.jupiter.api.Assumptions.abort(
                "fixture data not yet seeded for ACL contract: callsite="
                        + cs.description + ", principal=" + principal
                        + " — see project_acl_exists_refactor.md" );

        // The intended shape, once the fixture is in place, is roughly:
        //
        //   switchTo( principal );
        //   Object result = cs.invoker.get();
        //   String fingerprint = fingerprint( result );
        //   assertEquals( EXPECTED_FINGERPRINTS.get( cs, principal ), fingerprint,
        //                 "ACL filtering drifted for " + cs.description );
        //
        // where fingerprint() reduces the result to "count|sortedIds" or
        // similar (count + Long-id list sorted ascending). The expected
        // fingerprints are recorded against the pre-refactor JOIN clause,
        // then the rewritten EXISTS clause must reproduce them exactly.
    }

    /**
     * Switch the current Spring Security context to the requested principal.
     * <ul>
     *   <li>{@link Principal#ANONYMOUS} clears the context.</li>
     *   <li>{@link Principal#AUTHENTICATED_USER} routes through
     *       {@link TestAuthenticationUtils#runAsUser(String, boolean)} with a
     *       fixture user (created on demand).</li>
     *   <li>{@link Principal#ADMIN} routes through
     *       {@link TestAuthenticationUtils#runAsAdmin()}.</li>
     * </ul>
     * Kept package-private so a future batch can call it directly from
     * un-disabled test bodies without disturbing the {@link TestFactory}.
     */
    void switchTo( Principal principal ) {
        switch ( principal ) {
            case ANONYMOUS:
                SecurityContextHolder.clearContext();
                break;
            case AUTHENTICATED_USER:
                testAuthenticationUtils.runAsUser( "acl-contract-user", true );
                break;
            case ADMIN:
                testAuthenticationUtils.runAsAdmin();
                break;
            default:
                throw new IllegalStateException( "unknown principal: " + principal );
        }
    }
}
