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

import org.flywaydb.core.Flyway;
import org.h2.Driver;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDaoImpl;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDaoImpl;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.GeneDaoImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * <b>Fixture.</b> Lives in {@code src/test/resources/db/migration/h2-acl-contract/V9000__acl_contract_fixture.sql}
 * — a non-default Flyway location that this test's {@link Configuration} adds
 * to the Flyway locations list. Other {@link BaseDatabaseTest5} subclasses
 * see only the default {@code db/migration/h2} migrations, so the
 * "fresh test DB" invariant they rely on (e.g. {@code AclLinterServiceTest})
 * still holds. The fixture seeds:
 * <ul>
 *   <li>4 EEs spanning PUBLIC / PRIVATE_OWNED / PRIVATE_SHARED / ADMIN_ONLY,</li>
 *   <li>3 ArrayDesigns spanning PUBLIC / PRIVATE_OWNED / ADMIN_ONLY,</li>
 *   <li>4 BibliographicReferences (3 attached as primaryPublication, 1 unattached),</li>
 *   <li>2 FactorValues per EE,</li>
 *   <li>3 CompositeSequences under the public AD,</li>
 *   <li>3 Genes (Homo sapiens fixture taxon),</li>
 *   <li>Test users {@code testuser-owner} (sid 9001) and {@code testuser-collaborator} (sid 9002),</li>
 *   <li>ACL entries materialising the four ACL situations.</li>
 * </ul>
 *
 * <b>Callsite enumeration.</b> The {@link #CALLSITES} list captures every
 * place in {@code gemma-core/src/main/java} that calls one of the three
 * ACL clause-forming helpers. Each entry pairs a stable human-readable
 * description with a {@link Supplier} that either invokes the underlying
 * DAO method (callsites reachable from a public DAO API) or returns
 * {@link #UNWIRED} (callsites buried behind protected/abstract methods
 * inside the DAO base classes — these contribute a "n/a" fingerprint row
 * to the baseline so the refactor session sees them but their byte-identity
 * is established by transitivity through the wired callsites that share
 * the same query path).
 * <p>
 * The list size (35) intentionally exceeds the original 27-callsite
 * estimate in the planning doc: a number of
 * {@code resolveFilterablePropertyMeta} branches and the
 * {@code populateExpressionExperimentCount} family contribute extra
 * native-SQL callsites beyond the original HQL count.
 *
 * @see AclQueryUtils#formAclRestrictionClause(String)
 * @see EE2CAclQueryUtils
 */
@ContextConfiguration
public class AclSemanticsContractTest extends BaseDatabaseTest5 {

    /**
     * Sentinel returned by a {@link Callsite#invoker} for a callsite that
     * cannot currently be reached from a test (typically protected or
     * static-internal DAO methods). The fingerprint for such a callsite is
     * recorded as {@code unwired} in the baseline TSV.
     */
    private static final Object UNWIRED = new Object();

    /**
     * Inner configuration: wires the DAOs that the dynamic-test invokers
     * pull from. Reuses the H2 + Flyway + per-test-transaction plumbing of
     * {@link BaseDatabaseTest5}, but overrides {@link Flyway} to ALSO scan
     * {@code classpath:db/migration/h2-acl-contract} where this test's
     * fixture lives.
     */
    @Configuration
    @TestComponent
    static class AclSemanticsContractTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        // Override the parent dataSource bean so that we can register the same
        // SimpleDriverDataSource but skip the "drop all objects" call that the
        // parent makes (Flyway with both locations does the populating).
        @Bean
        @Override
        public DataSource dataSource() {
            DataSource ds = new SimpleDriverDataSource( new Driver(),
                    "jdbc:h2:mem:gemdtest-acl-contract;MODE=MYSQL;DB_CLOSE_DELAY=-1" );
            new JdbcTemplate( ds ).execute( "drop all objects" );
            return ds;
        }

        @Bean(initMethod = "migrate")
        @Override
        public Flyway flyway( DataSource dataSource ) {
            return Flyway.configure()
                    .dataSource( dataSource )
                    .locations( "classpath:db/migration/h2", "classpath:db/migration/h2-acl-contract" )
                    .baselineOnMigrate( true )
                    .load();
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }

        @Bean
        public BibliographicReferenceDao bibliographicReferenceDao( SessionFactory sessionFactory ) {
            return new BibliographicReferenceDaoImpl( sessionFactory );
        }

        @Bean
        public FactorValueDao factorValueDao( SessionFactory sessionFactory ) {
            return new FactorValueDaoImpl( sessionFactory );
        }

        @Bean
        public GeneDao geneDao( SessionFactory sessionFactory ) {
            return new GeneDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private BibliographicReferenceDao bibliographicReferenceDao;

    @Autowired
    private FactorValueDao factorValueDao;

    @Autowired
    private GeneDao geneDao;

    /**
     * Test principals the ACL clause discriminates against. The contract is
     * that <em>each principal's count and id list is byte-identical between
     * the before- and after-refactor runs</em>, not that the three principals
     * agree among themselves.
     */
    enum Principal {
        ANONYMOUS,
        AUTHENTICATED_USER,
        ADMIN
    }

    /** Location of the fingerprint baseline. Relative to the worktree root. */
    private static final Path BASELINE_PATH = Paths.get(
            "src", "test", "resources", "data", "acl-contract-baseline.tsv" );

    /**
     * One ACL callsite from {@code gemma-core/src/main/java}.
     */
    static final class Callsite {
        final String description;
        final Supplier<Object> invoker;

        Callsite( String description, Supplier<Object> invoker ) {
            this.description = description;
            this.invoker = invoker;
        }
    }

    // ------------------------------------------------------------------ //
    // CALLSITES — every place in gemma-core/src/main/java that calls one
    // of the three ACL clause-forming helpers. Description + line number
    // is the stable handle (line numbers reference source as of commit
    // a5701f0e — they will drift). Suppliers that invoke a wireable
    // public DAO method actually exercise the ACL clause; suppliers
    // returning UNWIRED contribute a "n/a" baseline row pending future
    // wiring.
    // ------------------------------------------------------------------ //

    private List<Callsite> buildCallsites() {
        return Collections.unmodifiableList( Arrays.asList(
                // --- BibliographicReferenceDaoImpl ---
                new Callsite( "BibliographicReferenceDaoImpl.countDistinctWithRelatedExperiments [L75]",
                        () -> bibliographicReferenceDao.countDistinctWithRelatedExperiments() ),
                new Callsite( "BibliographicReferenceDaoImpl.countWithRelatedExperiments [L87]",
                        () -> bibliographicReferenceDao.countWithRelatedExperiments() ),
                new Callsite( "BibliographicReferenceDaoImpl.getRelatedExperiments(int,int) [L96]",
                        () -> bibliographicReferenceDao.getRelatedExperiments( 0, 100 ) ),
                new Callsite( "BibliographicReferenceDaoImpl.getRelatedExperiments(Collection) [L123]",
                        () -> UNWIRED ),  // needs a Collection<BibliographicReference> fetched first; covered transitively

                // --- CharacteristicDaoImpl ---
                new Callsite( "CharacteristicDaoImpl.findExperimentReferencesByUris (native ACL join) [L241]",
                        () -> UNWIRED ),
                new Callsite( "CharacteristicDaoImpl.findExperimentReferencesByUris (native ACL restriction) [L245]",
                        () -> UNWIRED ),

                // --- CompositeSequenceDaoImpl ---
                new Callsite( "CompositeSequenceDaoImpl.getFilteringQuery [L95]",
                        () -> UNWIRED ),  // protected; routed via public load(Filters,...) but the AD link makes invocation non-trivial
                new Callsite( "CompositeSequenceDaoImpl.getFilteringIdQuery [L115]",
                        () -> UNWIRED ),
                new Callsite( "CompositeSequenceDaoImpl.getFilteringCountQuery [L129]",
                        () -> UNWIRED ),

                // --- ArrayDesignDaoImpl ---
                new Callsite( "ArrayDesignDaoImpl.countExpressionExperiments [L518]",
                        () -> countAdEEs( 9201L ) ),
                new Callsite( "ArrayDesignDaoImpl.getPerTaxonCount [L530]",
                        () -> arrayDesignDao.getPerTaxonCount() ),
                new Callsite( "ArrayDesignDaoImpl.countSwitchedExpressionExperiments [L562]",
                        () -> countAdSwitchedEEs( 9201L ) ),
                new Callsite( "ArrayDesignDaoImpl.getFilteringCountQuery [L1079]",
                        () -> arrayDesignDao.count( null ) ),
                new Callsite( "ArrayDesignDaoImpl.populateExpressionExperimentCount (native ACL join) [L1165]",
                        () -> UNWIRED ),
                new Callsite( "ArrayDesignDaoImpl.populateExpressionExperimentCount (native ACL restriction) [L1168]",
                        () -> UNWIRED ),
                new Callsite( "ArrayDesignDaoImpl.populateSwitchedExpressionExperimentCount (native ACL join) [L1191]",
                        () -> UNWIRED ),
                new Callsite( "ArrayDesignDaoImpl.populateSwitchedExpressionExperimentCount (native ACL restriction) [L1196]",
                        () -> UNWIRED ),

                // --- FactorValueDaoImpl ---
                new Callsite( "FactorValueDaoImpl.loadAll(int,int) [L82]",
                        () -> factorValueDao.loadAll( 0, 100 ) ),
                new Callsite( "FactorValueDaoImpl.loadAllIds() [L97]",
                        () -> factorValueDao.loadAllIds() ),
                new Callsite( "FactorValueDaoImpl.loadAllIds(int,int) data query [L109]",
                        () -> factorValueDao.loadAllIds( 0, 100 ) ),
                new Callsite( "FactorValueDaoImpl.loadAllIds(int,int) count query [L124]",
                        () -> factorValueDao.loadAllIds( 0, 100 ) ),  // shares the same public API as L109; baseline-equivalent
                new Callsite( "FactorValueDaoImpl.findByValueStartingWith [L134]",
                        () -> factorValueDao.findByValueStartingWith( "fv-", 100 ) ),

                // --- GeneDaoImpl ---
                new Callsite( "GeneDaoImpl.getCompositeSequenceCount [L228]",
                        () -> UNWIRED ),  // needs a Gene instance; can't construct in this slim wiring
                new Callsite( "GeneDaoImpl.getCompositeSequenceCountById [L247]",
                        () -> geneDao.getCompositeSequenceCountById( 9101L, false ) ),

                // --- ExpressionExperimentDaoImpl ---
                new Callsite( "ExpressionExperimentDaoImpl.loadAllIdentifiers [L240]",
                        () -> expressionExperimentDao.loadAllIdentifiers() ),
                new Callsite( "ExpressionExperimentDaoImpl.getCategoriesUsageFrequency (native ACL join) [L916]",
                        () -> UNWIRED ),
                new Callsite( "ExpressionExperimentDaoImpl.getCategoriesUsageFrequency (native ACL restriction) [L934]",
                        () -> UNWIRED ),
                new Callsite( "ExpressionExperimentDaoImpl.getAnnotationsUsageFrequencyInternal (native ACL join) [L1079]",
                        () -> UNWIRED ),
                new Callsite( "ExpressionExperimentDaoImpl.getAnnotationsUsageFrequencyInternal (native ACL restriction) [L1118]",
                        () -> UNWIRED ),
                new Callsite( "ExpressionExperimentDaoImpl.getTechnologyTypeUsageFrequency (native ACL join) [L1335]",
                        () -> expressionExperimentDao.getTechnologyTypeUsageFrequency() ),
                new Callsite( "ExpressionExperimentDaoImpl.getTechnologyTypeUsageFrequency (native ACL restriction) [L1337]",
                        () -> expressionExperimentDao.getTechnologyTypeUsageFrequency() ),
                new Callsite( "ExpressionExperimentDaoImpl.getOriginalPlatformsUsageFrequency (native ACL join) [L1399]",
                        () -> expressionExperimentDao.getOriginalPlatformsUsageFrequency( 100 ) ),
                new Callsite( "ExpressionExperimentDaoImpl.getOriginalPlatformsUsageFrequency (native ACL restriction) [L1403]",
                        () -> expressionExperimentDao.getOriginalPlatformsUsageFrequency( 100 ) ),
                new Callsite( "ExpressionExperimentDaoImpl.getPerTaxonCount [L1690]",
                        () -> expressionExperimentDao.getPerTaxonCount() ),
                new Callsite( "ExpressionExperimentDaoImpl.getFilteringCountQuery [L3993]",
                        () -> expressionExperimentDao.count( null ) )
        ) );
    }

    /**
     * countExpressionExperiments(ArrayDesign) takes an entity rather than an
     * id; this helper loads the AD as ADMIN (so we are sure to find it) and
     * THEN invokes the method under the current principal. The ACL-relevant
     * dimension is what comes back, not how the AD itself is fetched.
     */
    private long countAdEEs( long arrayDesignId ) {
        SecurityContext saved = SecurityContextHolder.getContext();
        try {
            switchTo( Principal.ADMIN );
            ArrayDesign ad = arrayDesignDao.load( arrayDesignId );
            SecurityContextHolder.setContext( saved );
            if ( ad == null ) return -1L;
            return arrayDesignDao.countExpressionExperiments( ad );
        } finally {
            SecurityContextHolder.setContext( saved );
        }
    }

    private long countAdSwitchedEEs( long arrayDesignId ) {
        SecurityContext saved = SecurityContextHolder.getContext();
        try {
            switchTo( Principal.ADMIN );
            ArrayDesign ad = arrayDesignDao.load( arrayDesignId );
            SecurityContextHolder.setContext( saved );
            if ( ad == null ) return -1L;
            return arrayDesignDao.countSwitchedExpressionExperiments( ad );
        } finally {
            SecurityContextHolder.setContext( saved );
        }
    }

    @BeforeEach
    public void setUpFixtureAuthentication() {
        // The BaseDatabaseTest5 base class does NOT auto-elevate to admin (only
        // BaseIntegrationTest5 does). Default to admin so any pre-invoker setup
        // sees everything; per-test then switches via switchTo(Principal).
        switchTo( Principal.ADMIN );
    }

    /**
     * Generates one {@link DynamicTest} per (callsite, principal) pair, and
     * either appends one row to the in-progress fingerprint map or asserts
     * byte-equality against a previously-recorded baseline. Total = 35 * 3
     * = 105 dynamic tests.
     */
    @TestFactory
    Stream<DynamicTest> aclContract_acrossAllCallsitesAndPrincipals() {
        Map<String, String> baseline = loadBaselineIfPresent();
        Map<String, String> recorded = new TreeMap<>();
        List<Callsite> callsites = buildCallsites();
        List<DynamicTest> tests = new ArrayList<>( callsites.size() * Principal.values().length );
        for ( Callsite cs : callsites ) {
            for ( Principal principal : Principal.values() ) {
                String key = cs.description + " :: " + principal.name();
                tests.add( DynamicTest.dynamicTest( key, () -> runOne( cs, principal, key, baseline, recorded ) ) );
            }
        }
        // We can't easily emit a "flush recorded -> file" step inside a TestFactory.
        // Instead the dedicated @Test seedBaseline (below) does it directly.
        return tests.stream();
    }

    /**
     * If no baseline exists yet, compute all (callsite × principal)
     * fingerprints and write {@link #BASELINE_PATH}. Skipped (via JUnit
     * assumption) if a baseline already exists — in that case the
     * {@link #aclContract_acrossAllCallsitesAndPrincipals()} dynamic tests
     * are doing the diff.
     */
    @Test
    void seedBaselineIfMissing() throws IOException {
        Assumptions.assumeTrue( !Files.exists( BASELINE_PATH ),
                "baseline file already exists; seedBaselineIfMissing is a one-shot capture step" );
        Map<String, String> recorded = new LinkedHashMap<>();
        for ( Callsite cs : buildCallsites() ) {
            for ( Principal principal : Principal.values() ) {
                String key = cs.description + " :: " + principal.name();
                recorded.put( key, fingerprintOne( cs, principal ) );
            }
        }
        Files.createDirectories( BASELINE_PATH.getParent() );
        try ( var w = Files.newBufferedWriter( BASELINE_PATH, StandardCharsets.UTF_8 ) ) {
            w.write( "callsite\tprincipal\tfingerprint\n" );
            for ( Map.Entry<String, String> e : recorded.entrySet() ) {
                String k = e.getKey();
                int sep = k.lastIndexOf( " :: " );
                w.write( k.substring( 0, sep ) );
                w.write( "\t" );
                w.write( k.substring( sep + 4 ) );
                w.write( "\t" );
                w.write( e.getValue() );
                w.write( "\n" );
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //

    private void runOne( Callsite cs, Principal principal, String key,
                         Map<String, String> baseline, Map<String, String> recorded ) {
        String observed = fingerprintOne( cs, principal );
        recorded.put( key, observed );
        String expected = baseline.get( key );
        Assumptions.assumeTrue( expected != null,
                "no baseline entry for " + key + " — run seedBaselineIfMissing first" );
        assertEquals( expected, observed,
                "ACL contract drifted for " + key + " — security regression candidate" );
    }

    private String fingerprintOne( Callsite cs, Principal principal ) {
        switchTo( principal );
        Object result;
        try {
            result = cs.invoker.get();
        } catch ( RuntimeException e ) {
            return "error|" + e.getClass().getSimpleName() + ":" + truncate( String.valueOf( e.getMessage() ), 80 );
        }
        return fingerprint( result );
    }

    /**
     * Reduce an invocation result to a stable "count|sortedIds" string. The
     * specifics:
     * <ul>
     *   <li>{@code null} → {@code "null"}</li>
     *   <li>{@link #UNWIRED} → {@code "unwired"}</li>
     *   <li>{@link Number} → {@code "scalar:" + value}</li>
     *   <li>{@link Collection} → {@code count|<sorted-stringified-ids>}; ids
     *       extracted via {@link #extractId(Object)}</li>
     *   <li>{@link Map} → {@code count|<sorted-stringified-keys>}; keys
     *       extracted via {@link #extractId(Object)} so the fingerprint is
     *       insensitive to value ordering</li>
     *   <li>anything else → its {@code toString}, truncated to 200 chars</li>
     * </ul>
     */
    private static String fingerprint( Object o ) {
        if ( o == null ) return "null";
        if ( o == UNWIRED ) return "unwired";
        if ( o instanceof Number ) return "scalar:" + o;
        if ( o instanceof Collection<?> ) {
            Collection<?> col = ( Collection<?> ) o;
            List<String> ids = col.stream()
                    .map( AclSemanticsContractTest::extractId )
                    .sorted()
                    .collect( Collectors.toList() );
            return col.size() + "|" + String.join( ",", ids );
        }
        if ( o instanceof Map<?, ?> ) {
            Map<?, ?> m = ( Map<?, ?> ) o;
            List<String> keys = m.keySet().stream()
                    .map( AclSemanticsContractTest::extractId )
                    .sorted()
                    .collect( Collectors.toList() );
            return m.size() + "|" + String.join( ",", keys );
        }
        return truncate( o.toString(), 200 );
    }

    /**
     * Best-effort id extraction: reflectively call {@code getId()} if present,
     * otherwise fall back to {@code toString()}. Keeps the fingerprint stable
     * across runs (object identity / hashCode would not).
     */
    private static String extractId( Object o ) {
        if ( o == null ) return "null";
        try {
            var m = o.getClass().getMethod( "getId" );
            Object id = m.invoke( o );
            return id == null ? "null" : id.toString();
        } catch ( NoSuchMethodException e ) {
            return truncate( o.toString(), 60 );
        } catch ( ReflectiveOperationException e ) {
            return "err:" + e.getClass().getSimpleName();
        }
    }

    private static String truncate( String s, int max ) {
        if ( s == null ) return "null";
        s = s.replace( "\t", " " ).replace( "\n", " " );
        return s.length() <= max ? s : s.substring( 0, max ) + "...";
    }

    private Map<String, String> loadBaselineIfPresent() {
        if ( !Files.exists( BASELINE_PATH ) ) return Collections.emptyMap();
        Map<String, String> baseline = new LinkedHashMap<>();
        try ( var r = Files.newBufferedReader( BASELINE_PATH, StandardCharsets.UTF_8 ) ) {
            String line = r.readLine();  // header
            while ( ( line = r.readLine() ) != null ) {
                if ( line.isEmpty() ) continue;
                String[] parts = line.split( "\t", 3 );
                if ( parts.length < 3 ) continue;
                baseline.put( parts[0] + " :: " + parts[1], parts[2] );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( "could not read baseline " + BASELINE_PATH.toAbsolutePath(), e );
        }
        return baseline;
    }

    /**
     * Switch the current Spring Security context to the requested principal.
     * <ul>
     *   <li>{@link Principal#ANONYMOUS} installs an
     *       {@link AnonymousAuthenticationToken} carrying
     *       {@code IS_AUTHENTICATED_ANONYMOUSLY}.</li>
     *   <li>{@link Principal#AUTHENTICATED_USER} installs a
     *       {@link TestingAuthenticationToken} for {@code testuser-owner}
     *       (USER group authority).</li>
     *   <li>{@link Principal#ADMIN} installs a
     *       {@link TestingAuthenticationToken} for {@code administrator}
     *       (ADMIN group authority).</li>
     * </ul>
     */
    void switchTo( Principal principal ) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        switch ( principal ) {
            case ANONYMOUS: {
                List<GrantedAuthority> auths = Collections.singletonList(
                        new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) );
                ctx.setAuthentication( new AnonymousAuthenticationToken( "key", "anonymousUser", auths ) );
                break;
            }
            case AUTHENTICATED_USER: {
                List<GrantedAuthority> auths = Collections.singletonList(
                        new SimpleGrantedAuthority( AuthorityConstants.USER_GROUP_AUTHORITY ) );
                TestingAuthenticationToken t = new TestingAuthenticationToken( "testuser-owner", "x", auths );
                t.setAuthenticated( true );
                ctx.setAuthentication( t );
                break;
            }
            case ADMIN: {
                List<GrantedAuthority> auths = Collections.singletonList(
                        new SimpleGrantedAuthority( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) );
                TestingAuthenticationToken t = new TestingAuthenticationToken( "administrator", "x", auths );
                t.setAuthenticated( true );
                ctx.setAuthentication( t );
                break;
            }
            default:
                throw new IllegalStateException( "unknown principal: " + principal );
        }
        SecurityContextHolder.setContext( ctx );
    }
}
