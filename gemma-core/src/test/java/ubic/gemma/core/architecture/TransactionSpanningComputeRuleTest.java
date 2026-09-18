package ubic.gemma.core.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * No transaction may be open while a long computation runs.
 * <p>
 * Gemma sets {@code hibernate.connection.handling_mode = DELAYED_ACQUISITION_AND_HOLD}, so a transaction holds
 * its pooled connection from first statement to commit, including through a stretch that issues no statements
 * at all. {@code gemma.db.hikari.maxLifetime} recycles connections at 30 minutes. A computation that runs
 * inside a transaction for longer than that holds a connection the pool expects to have turned over, and holds
 * it for nothing — the computation never uses it.
 * <p>
 * Measured on production 2026-09-17: {@code corrMat -force} on GSE260875 spent 44 minutes in quantile
 * normalization inside one {@code @Transactional(readOnly = true)} and then failed on its next statement. Four
 * services had the same shape, and the reads and writes on every one of them were cleanly separable from the
 * arithmetic between them. Paul's ruling that day: the read and the write have to be independent of the
 * compute, in general.
 * <p>
 * The correct shape is {@code @Transactional(propagation = Propagation.NEVER)} on the orchestrator, with each
 * read and write step carrying its own annotation on a bean the orchestrator calls through its proxy. That
 * idiom is not new here — {@code PreprocessorServiceImpl}, {@code DifferentialExpressionAnalyzerServiceImpl},
 * {@code OutlierFlaggingServiceImpl} and {@code DataUpdaterImpl} are among sixteen classes already using it.
 *
 * <h2>Why this searches transitively</h2>
 *
 * A direct-call rule would have caught none of the four. {@code SampleCoexpressionAnalysisServiceImpl.prepare}
 * is three hops from {@code quantileNormalize}, and two of those hops are interface calls. So the search
 * follows calls through {@code ubic.gemma} and resolves an interface call to the implementations of that
 * interface, which is where the body actually is.
 *
 * @see LongComputation
 * @see ReadServiceTransactionalRuleTest for the other half of the transaction rules, and for the sentinel
 * pattern this test copies
 */
@AnalyzeClasses(
        packages = "ubic.gemma",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class TransactionSpanningComputeRuleTest {

    /** Scopes a {@link SuppressArchUnit} marker to this rule alone. */
    private static final String SUPPRESSION = "TransactionSpanningCompute";

    /**
     * How many calls deep to follow. Six reaches every known instance with room to spare —
     * {@code prepare} is three hops from {@code quantileNormalize} — and keeps the search from walking the
     * whole service graph.
     */
    private static final int MAX_DEPTH = 6;

    /**
     * Whether this code unit runs inside a transaction of its own.
     * <p>
     * A class-level annotation applies to every public method, so both levels count.
     * {@link Propagation#NEVER} is the opposite declaration — it asserts there is no transaction — so it is
     * not a violation but the fix.
     */
    private static boolean isTransactional( JavaCodeUnit unit ) {
        return declaresTransaction( unit ) || declaresTransaction( unit.getOwner() );
    }

    private static boolean declaresTransaction( Object annotated ) {
        Optional<? extends com.tngtech.archunit.core.domain.JavaAnnotation<?>> a;
        if ( annotated instanceof JavaCodeUnit ) {
            a = ( ( JavaCodeUnit ) annotated ).tryGetAnnotationOfType( Transactional.class.getName() );
        } else {
            a = ( ( JavaClass ) annotated ).tryGetAnnotationOfType( Transactional.class.getName() );
        }
        if ( !a.isPresent() ) {
            return false;
        }
        Optional<Object> propagation = a.get().tryGetExplicitlyDeclaredProperty( "propagation" );
        if ( propagation.isPresent() && String.valueOf( propagation.get() ).endsWith( "NEVER" ) ) {
            return false;
        }
        return true;
    }

    /**
     * Whether this code unit asserts that no transaction is open. Called from inside one, it throws
     * {@code IllegalTransactionStateException} — which is how {@code Propagation.NEVER} spreads upward.
     */
    private static boolean declaresNever( JavaCodeUnit unit ) {
        Optional<? extends com.tngtech.archunit.core.domain.JavaAnnotation<?>> a =
                unit.tryGetAnnotationOfType( Transactional.class.getName() );
        if ( !a.isPresent() ) {
            return false;
        }
        Optional<Object> propagation = a.get().tryGetExplicitlyDeclaredProperty( "propagation" );
        return propagation.isPresent() && String.valueOf( propagation.get() ).endsWith( "NEVER" );
    }

    private static boolean isLongComputation( JavaCodeUnit unit ) {
        return unit.isAnnotatedWith( LongComputation.class )
                || unit.getOwner().isAnnotatedWith( LongComputation.class );
    }

    private static boolean isSuppressed( JavaMethod method ) {
        Optional<com.tngtech.archunit.core.domain.JavaAnnotation<JavaMethod>> marker =
                method.tryGetAnnotationOfType( SuppressArchUnit.class.getName() );
        if ( !marker.isPresent() ) {
            return false;
        }
        Optional<Object> value = marker.get().tryGetExplicitlyDeclaredProperty( "value" );
        return value.isPresent() && SUPPRESSION.equals( value.get() );
    }

    private static boolean ours( JavaClass javaClass ) {
        return javaClass.getPackageName().startsWith( "ubic.gemma" );
    }

    /**
     * The code units a call can land in.
     * <p>
     * An interface call resolves to the interface's own method, whose body is empty, so following only the
     * resolved target would stop at every service boundary in the codebase. Each implementation of that
     * interface is therefore a candidate too. Over-approximating this way can name a path through an
     * implementation the caller never uses; that is the right direction to err, and
     * {@link SuppressArchUnit} is the escape hatch when it happens.
     */
    private static List<JavaCodeUnit> targetsOf( JavaCodeUnit from, JavaClasses classes ) {
        List<JavaCodeUnit> out = new ArrayList<>();
        for ( JavaMethodCall call : from.getMethodCallsFromSelf() ) {
            JavaClass owner = call.getTargetOwner();
            if ( !ours( owner ) ) {
                continue;
            }
            for ( JavaMethod resolved : call.getTarget().resolveMember().isPresent()
                    ? java.util.Collections.singletonList( call.getTarget().resolveMember().get() )
                    : java.util.Collections.<JavaMethod>emptyList() ) {
                out.add( resolved );
                if ( owner.isInterface() ) {
                    for ( JavaClass impl : owner.getAllSubclasses() ) {
                        if ( !ours( impl ) ) {
                            continue;
                        }
                        impl.tryGetMethod( resolved.getName(), resolved.getRawParameterTypes().stream()
                                        .map( JavaClass::getName ).toArray( String[]::new ) )
                                .ifPresent( out::add );
                    }
                }
            }
        }
        for ( JavaConstructorCall call : from.getConstructorCallsFromSelf() ) {
            if ( ours( call.getTargetOwner() ) ) {
                call.getTarget().resolveMember().ifPresent( out::add );
            }
        }
        return out;
    }

    /**
     * The first {@link LongComputation} or {@link Propagation#NEVER} method this method can reach, with the call
     * path that gets there, or null.
     */
    private static List<JavaCodeUnit> pathToComputation( JavaMethod start, JavaClasses classes ) {
        Set<String> seen = new HashSet<>();
        Deque<List<JavaCodeUnit>> queue = new ArrayDeque<>();
        queue.add( new ArrayList<>( java.util.Collections.singletonList( start ) ) );
        seen.add( start.getFullName() );
        while ( !queue.isEmpty() ) {
            List<JavaCodeUnit> path = queue.poll();
            if ( path.size() > MAX_DEPTH ) {
                continue;
            }
            for ( JavaCodeUnit next : targetsOf( path.get( path.size() - 1 ), classes ) ) {
                if ( !seen.add( next.getFullName() ) ) {
                    continue;
                }
                List<JavaCodeUnit> extended = new ArrayList<>( path );
                extended.add( next );
                if ( isLongComputation( next ) || declaresNever( next ) ) {
                    return extended;
                }
                queue.add( extended );
            }
        }
        return null;
    }

    private static String describe( List<JavaCodeUnit> path ) {
        StringBuilder buf = new StringBuilder();
        for ( int i = 0; i < path.size(); i++ ) {
            if ( i > 0 ) {
                buf.append( "\n      -> " );
            }
            buf.append( path.get( i ).getOwner().getSimpleName() ).append( "." ).append( path.get( i ).getName() );
        }
        return buf.toString();
    }

    private static final ArchCondition<JavaMethod> NOT_REACH_A_LONG_COMPUTATION =
            new ArchCondition<JavaMethod>( "not hold a transaction across a long computation" ) {
                @Override
                public void check( JavaMethod method, ConditionEvents events ) {
                    if ( !isTransactional( method ) || isSuppressed( method ) || isLongComputation( method ) ) {
                        return;
                    }
                    List<JavaCodeUnit> path = pathToComputation( method, null );
                    if ( path == null ) {
                        return;
                    }
                    if ( !isLongComputation( path.get( path.size() - 1 ) ) ) {
                        events.add( SimpleConditionEvent.violated( method, String.format(
                                "%s.%s is @Transactional and reaches a Propagation.NEVER method, which throws "
                                        + "IllegalTransactionStateException when called inside a transaction. Make "
                                        + "the caller an orchestrator too: Propagation.NEVER on it, and its "
                                        + "transactional steps through its proxy.%n"
                                        + "      %s",
                                method.getOwner().getSimpleName(), method.getName(), describe( path ) ) ) );
                        return;
                    }
                    events.add( SimpleConditionEvent.violated( method, String.format(
                            "%s.%s is @Transactional and reaches a @LongComputation, so the connection is held "
                                    + "for the whole computation. Split it: read in its own transaction, compute "
                                    + "with none open (Propagation.NEVER on the orchestrator), write in its own.%n"
                                    + "      %s",
                            method.getOwner().getSimpleName(), method.getName(), describe( path ) ) ) );
                }
            };

    /**
     * 🛑 <b>The guard against a guard that checks nothing.</b>
     * <p>
     * ArchUnit reads compiled bytecode through a bundled ASM, and when that ASM is older than the class file
     * version we compile to it imports zero classes without complaining — every rule then passes for a reason
     * that has nothing to do with the codebase. That is what archunit 1.3.0 did to Java 25 bytecode. 1.3.2
     * onwards reads it and the parent pom is on 1.4.1, so this is closed; the sentinel stays because a green
     * run that checked nothing is indistinguishable from a green run that checked everything.
     */
    @ArchTest
    public static void classes_are_actually_imported( JavaClasses classes ) {
        if ( classes.isEmpty() ) {
            throw new AssertionError( "ArchUnit imported no classes at all, so every rule here is vacuous. "
                    + "Usual cause: the bundled ASM cannot read our class file version — check the archunit "
                    + "version against the compiler release level." );
        }
    }

    /**
     * {@code allowEmptyShould} stays {@code false}: if the {@code that()} clause ever stops matching any
     * method, that is a broken rule, not a clean codebase.
     * <p>
     * The scope is every class, not only {@code ..service..} and {@code core.analysis..} as it first was.
     * {@code ExpressionExperimentPlatformSwitchService} is in {@code core.loader.expression} and called
     * {@code createProcessedDataVectors} from inside its own transaction after that method became {@code NEVER};
     * the narrower rule never looked at it, and {@code ExpressionExperimentPlatformSwitchTest} failed on it.
     */
    @ArchTest
    public static final ArchRule transactional_methods_must_not_span_a_long_computation =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage( "ubic.gemma.." )
                    .should( NOT_REACH_A_LONG_COMPUTATION )
                    .allowEmptyShould( false );
}
