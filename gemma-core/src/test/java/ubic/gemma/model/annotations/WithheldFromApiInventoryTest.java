package ubic.gemma.model.annotations;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Enforcement for {@link WithheldFromApi}. The annotation on its own is a comment that compiles —
 * nothing stops the next person deleting it from {@code getCurrentUserIsOwner()} and shipping
 * per-caller authorization state on a cacheable response. This is what stops that.
 *
 * <h2>What it pins</h2>
 *
 * <ol>
 * <li><b>The inventory.</b> {@code withheld-from-api-inventory.txt} lists every application of the
 * annotation with its reason. Adding, removing or re-reasoning one fails until the file is updated,
 * so each change is a deliberate line in a diff rather than an invisible API change. Deleting an
 * annotation fails here — which an annotation-driven scan alone could never catch, since a deleted
 * annotation simply vanishes from the scan.</li>
 * <li><b>The suppression.</b> Every listed member is absent from Jackson's serialization view of its
 * class, checked by comparing {@link java.lang.reflect.Member}s rather than property names.</li>
 * <li><b>That the suppression is not defeated.</b> Member identity alone is not enough: if a sibling
 * accessor for the same datum carries an explicit {@code @JsonProperty}, Jackson keeps that one, the
 * ignore never bites, and no accessor is ever identical to the annotated member — so the identity
 * check passes while the data is on the wire. That is precisely what all 17 {@code GeeqValueObject}
 * per-factor score getters were doing, undetected, when this class was first written. See
 * {@link #aMemberClaimedWithheldIsActuallyWithheld()}.</li>
 * <li><b>The ratchet.</b> {@link Reason#UNTRIAGED} is migration debt, so its population may only
 * shrink. {@link #UNTRIAGED_CEILING} is a separate constant on purpose: regenerating the inventory
 * would hide a new untriaged member, and this would not.</li>
 * </ol>
 *
 * When you legitimately change a suppression, update the inventory file and — if you retired an
 * untriaged member — lower the ceiling.
 */
class WithheldFromApiInventoryTest {

    private static final String INVENTORY_RESOURCE = "/withheld-from-api-inventory.txt";
    private static final String MODEL_PACKAGE_PATH = "ubic/gemma/model";

    /**
     * Number of {@link Reason#UNTRIAGED} members left over from the {@code @GemmaWebOnly} migration.
     * This may only ever go down. Do not raise it — a new suppression has a real reason available.
     * <p>
     * The backlog is drained: all 13 were closed without a single speculative exposure — some deleted
     * once nothing turned out to write them, the rest re-reasoned once the stated reason proved wrong
     * about the data. At zero this is no longer a burn-down target but a prohibition: any new
     * {@code UNTRIAGED} fails this test, which is what the reason's javadoc asks for when it says the
     * value "should never be chosen for a new member".
     */
    private static final int UNTRIAGED_CEILING = 0;

    private static final Set<Reason> MUST_NEVER_SERIALIZE = EnumSet.of( Reason.CALLER_IDENTITY, Reason.DISCLOSURE );

    /** One application of the annotation: the member it is on, and the reason it claims. */
    private record Site(Class<?> owner, AnnotatedElementRef member, Reason reason) {
        String key() {
            return owner.getName() + "#" + member.name();
        }
    }

    /** A field or a method, uniformly addressable — Jackson exposes both as accessors. */
    private record AnnotatedElementRef(Field field, Method method) {
        String name() {
            return field != null ? field.getName() : method.getName();
        }

        boolean isSameAs( AnnotatedMember m ) {
            return field != null ? field.equals( m.getMember() ) : method.equals( m.getMember() );
        }

        /**
         * The property name(s) this member would carry if Jackson bound it. Identity-matching alone
         * misses the case that motivated {@link #aMemberClaimedWithheldIsActuallyWithheld()}: when a
         * sibling accessor for the same datum carries an explicit {@code @JsonProperty}, Jackson keeps
         * that one and the ignore never bites, so no accessor is ever identical to the annotated
         * member and an identity check finds nothing.
         */
        Set<String> impliedPropertyNames() {
            Set<String> out = new LinkedHashSet<>();
            if ( field != null ) {
                out.add( field.getName() );
                return out;
            }
            String n = method.getName();
            if ( n.startsWith( "get" ) ) {
                out.add( demangle( n.substring( 3 ) ) );
            } else if ( n.startsWith( "is" ) ) {
                out.add( demangle( n.substring( 2 ) ) );
            }
            out.add( n );
            return out;
        }

        /** Jackson's std mangling: lowercase a leading uppercase run, leave a lowercase lead alone. */
        private static String demangle( String s ) {
            if ( s.isEmpty() || Character.isLowerCase( s.charAt( 0 ) ) ) {
                return s;
            }
            StringBuilder sb = new StringBuilder( s );
            for ( int i = 0; i < sb.length() && Character.isUpperCase( sb.charAt( i ) ); i++ ) {
                sb.setCharAt( i, Character.toLowerCase( sb.charAt( i ) ) );
            }
            return sb.toString();
        }
    }

    // ---------------------------------------------------------------- scanning

    private static List<Class<?>> modelClasses() {
        Path base;
        try {
            base = Paths.get( WithheldFromApi.class.getProtectionDomain().getCodeSource().getLocation().toURI() );
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }
        assertTrue( Files.isDirectory( base ),
                "expected to scan exploded classes but got " + base + "; this guard cannot run from a jar" );
        Path pkg = base.resolve( MODEL_PACKAGE_PATH );
        assertTrue( Files.isDirectory( pkg ), "no compiled model package under " + base );

        List<Class<?>> classes = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        try ( Stream<Path> walk = Files.walk( pkg ) ) {
            for ( Path p : walk.filter( f -> f.toString().endsWith( ".class" ) ).collect( Collectors.toList() ) ) {
                String name = base.relativize( p ).toString()
                        .replace( java.io.File.separatorChar, '.' )
                        .replaceAll( "\\.class$", "" );
                if ( name.endsWith( "package-info" ) || name.endsWith( "module-info" ) ) {
                    continue; // not addressable through Class.forName, and cannot carry the annotation
                }
                try {
                    // initialize=false: reading annotations must not run static initializers
                    classes.add( Class.forName( name, false, WithheldFromApi.class.getClassLoader() ) );
                } catch ( Throwable t ) {
                    failures.add( name + " (" + t.getClass().getSimpleName() + ")" );
                }
            }
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
        // a class we cannot load is a class we cannot check — say so rather than under-reporting
        assertTrue( failures.isEmpty(), "could not load, so could not check: " + failures );
        assertFalse( classes.isEmpty(), "scanned nothing under " + pkg );
        return classes;
    }

    private static List<Site> scan() {
        List<Site> sites = new ArrayList<>();
        for ( Class<?> c : modelClasses() ) {
            for ( Field f : c.getDeclaredFields() ) {
                WithheldFromApi a = f.getAnnotation( WithheldFromApi.class );
                if ( a != null ) {
                    sites.add( new Site( c, new AnnotatedElementRef( f, null ), a.value() ) );
                }
            }
            for ( Method m : c.getDeclaredMethods() ) {
                WithheldFromApi a = m.getAnnotation( WithheldFromApi.class );
                if ( a != null ) {
                    sites.add( new Site( c, new AnnotatedElementRef( null, m ), a.value() ) );
                }
            }
        }
        return sites;
    }

    private static TreeMap<String, Reason> pinned() {
        TreeMap<String, Reason> expected = new TreeMap<>();
        try ( InputStream in = WithheldFromApiInventoryTest.class.getResourceAsStream( INVENTORY_RESOURCE ) ) {
            assertTrue( in != null, "missing " + INVENTORY_RESOURCE );
            for ( String line : new String( in.readAllBytes(), StandardCharsets.UTF_8 ).split( "\n" ) ) {
                String s = line.trim();
                if ( s.isEmpty() || s.startsWith( "#" ) ) {
                    continue;
                }
                String[] parts = s.split( "\\s+" );
                assertEquals( 2, parts.length, "malformed inventory line: " + s );
                expected.put( parts[0], Reason.valueOf( parts[1] ) );
            }
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
        return expected;
    }

    // ------------------------------------------------------------------- tests

    @Test
    void theInventoryMatchesTheTree() {
        TreeMap<String, Reason> expected = pinned();
        TreeMap<String, Reason> actual = new TreeMap<>();
        for ( Site s : scan() ) {
            assertTrue( actual.put( s.key(), s.reason() ) == null,
                    "two suppressions resolve to the same key, so the inventory cannot address them: " + s.key() );
        }

        Set<String> added = new TreeSet<>( actual.keySet() );
        added.removeAll( expected.keySet() );
        Set<String> removed = new TreeSet<>( expected.keySet() );
        removed.removeAll( actual.keySet() );
        Set<String> rereasoned = new LinkedHashSet<>();
        for ( String k : actual.keySet() ) {
            if ( expected.containsKey( k ) && expected.get( k ) != actual.get( k ) ) {
                rereasoned.add( k + ": " + expected.get( k ) + " -> " + actual.get( k ) );
            }
        }

        if ( !added.isEmpty() || !removed.isEmpty() || !rereasoned.isEmpty() ) {
            fail( "the @WithheldFromApi inventory is stale. If these changes are intended, update"
                    + " gemma-core/src/test/resources" + INVENTORY_RESOURCE + "."
                    + "\n  newly suppressed (not in inventory): " + added
                    + "\n  no longer suppressed (REMOVING ONE OF THESE MAY PUBLISH IT): " + removed
                    + "\n  reason changed: " + rereasoned );
        }
    }

    @Test
    void nothingInTheInventoryReachesTheWire() {
        ObjectMapper mapper = new ObjectMapper();
        List<String> leaked = new ArrayList<>();
        List<Site> sites = scan();
        assertFalse( sites.isEmpty(), "nothing scanned, so nothing was actually checked" );

        for ( Site s : sites ) {
            BeanDescription desc = mapper.getSerializationConfig()
                    .introspect( mapper.constructType( s.owner() ) );
            List<BeanPropertyDefinition> props = desc.findProperties();
            // guards against a vacuous pass: if Jackson saw no properties at all on this class, the
            // loop below proves nothing
            assertFalse( props.isEmpty(), "Jackson introspected no properties on " + s.owner().getName() );
            for ( BeanPropertyDefinition p : props ) {
                for ( AnnotatedMember m : new AnnotatedMember[] { p.getField(), p.getGetter() } ) {
                    if ( m != null && s.member().isSameAs( m ) ) {
                        leaked.add( s.key() + " serializes as \"" + p.getName() + "\"" );
                    }
                }
            }
        }
        assertTrue( leaked.isEmpty(), "@WithheldFromApi is not suppressing these: " + leaked );
    }

    /**
     * The invariant {@link #nothingInTheInventoryReachesTheWire()} was too weak to catch: a member
     * whose reason <em>claims</em> the data is withheld must actually be off the wire.
     * <p>
     * {@link Reason#REDUNDANT} is exempt by design — it asserts that nothing is being withheld, so a
     * property of the same name serializing elsewhere confirms the reason instead of contradicting it.
     * That is the state of the four flattened taxon / factor-value accessors, where a sibling member
     * legitimately owns the name.
     * <p>
     * Every other reason is enforced, {@link Reason#INTERNAL_ONLY} very much included: a member that
     * nothing populates, or whose shape is lossy, publishes a falsehood if it reaches the wire, so the
     * suppression there is doing real work. That split is the whole point of having the two reasons —
     * before it existed, all 62 of these sat under REDUNDANT and none of them were checked.
     */
    @Test
    void aMemberClaimedWithheldIsActuallyWithheld() {
        ObjectMapper mapper = new ObjectMapper();
        List<String> defeated = new ArrayList<>();

        for ( Site s : scan() ) {
            if ( s.reason() == Reason.REDUNDANT ) {
                continue;
            }
            BeanDescription desc = mapper.getSerializationConfig()
                    .introspect( mapper.constructType( s.owner() ) );
            List<BeanPropertyDefinition> props = desc.findProperties();
            Set<String> serializedNames = props.stream()
                    .map( BeanPropertyDefinition::getName )
                    .collect( Collectors.toCollection( LinkedHashSet::new ) );

            for ( String implied : s.member().impliedPropertyNames() ) {
                if ( serializedNames.contains( implied ) ) {
                    defeated.add( s.key() + " claims " + s.reason() + " but \"" + implied + "\" serializes" );
                }
                // a suppressed getter whose backing field serializes under ANY name — the exact shape
                // of the GeeqValueObject case, where @JsonProperty renamed the field just enough that
                // the name check above would not have noticed
                if ( s.member().method() == null ) {
                    continue;
                }
                Field backing;
                try {
                    backing = s.owner().getDeclaredField( implied );
                } catch ( NoSuchFieldException e ) {
                    continue;
                }
                for ( BeanPropertyDefinition p : props ) {
                    if ( p.getField() != null && backing.equals( p.getField().getMember() ) ) {
                        defeated.add( s.key() + " claims " + s.reason() + " but its backing field "
                                + implied + " serializes as \"" + p.getName() + "\"" );
                    }
                }
            }
        }
        assertTrue( defeated.isEmpty(), "suppression is defeated for:\n  " + String.join( "\n  ", defeated ) );
    }

    @Test
    void theCallerIdentityAndDisclosureSitesAreStillThere() {
        // the members this annotation was created for. Losing one is the failure mode that matters,
        // so name them here as well: the inventory can be regenerated, this list has to be argued with.
        Set<String> required = Set.of(
                "ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject#getCurrentUserIsOwner",
                "ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject#getCurrentUserHasWritePermission",
                "ubic.gemma.model.genome.gene.GeneSetValueObject#getCurrentUserIsOwner",
                "ubic.gemma.model.expression.biomaterial.BioMaterialValueObject#fastqHeaders" );

        Set<String> found = scan().stream()
                .filter( s -> MUST_NEVER_SERIALIZE.contains( s.reason() ) )
                .map( Site::key )
                .collect( Collectors.toCollection( TreeSet::new ) );

        assertTrue( found.containsAll( required ),
                "a caller-identity or disclosure suppression was removed or re-reasoned: "
                        + required.stream().filter( r -> !found.contains( r ) ).collect( Collectors.toList() ) );
    }

    @Test
    void theUntriagedBacklogOnlyShrinks() {
        long untriaged = scan().stream().filter( s -> s.reason() == Reason.UNTRIAGED ).count();
        assertTrue( untriaged <= UNTRIAGED_CEILING,
                "UNTRIAGED is migration debt from @GemmaWebOnly, not a reason for new suppressions:"
                        + " found " + untriaged + ", ceiling " + UNTRIAGED_CEILING
                        + ". A new member should carry a real reason; if you retired one, lower the ceiling." );
    }
}
