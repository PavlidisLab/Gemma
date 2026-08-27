package ubic.gemma.core.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * A service method that reaches a DAO must declare its own transaction.
 * <p>
 * Spring opens the Hibernate session from {@code @Transactional}. A service method that calls a DAO without
 * one reaches the session factory with nothing bound to the thread and fails at runtime with
 * {@code "Could not obtain transaction-synchronized Session for current thread"} — a 500, on every call,
 * for a method that compiles and unit-tests perfectly.
 * <p>
 * 🛑 <b>This is invisible to the tests we normally write, in both directions.</b> A DAO test extending
 * {@code BaseDatabaseTest5} runs inside the test's own transaction, so a session always exists and the
 * missing annotation cannot manifest. A mocked web-layer test stubs the service outright, so the real
 * method never executes. Green above, green below, broken in between.
 * <p>
 * Caught in production on 2026-08-26: {@code ArrayDesignReadServiceImpl.loadOriginalPlatformValueObjectsForEE}
 * was written alongside {@code loadValueObjectsForEE}, copying the body but not the
 * {@code @Transactional(readOnly = true)} the sibling carries, and
 * {@code GET /datasets/{id}/platforms?original=true} 500'd on the live server. Annotations are not inherited
 * from the method you cloned; this rule is the thing that remembers that. On its first working run it found a
 * second instance of the identical mistake — {@code ExpressionExperimentReadServiceImpl.findByBioMaterials},
 * whose neighbour {@code findIdsByBioMaterial} is annotated and whose own class Javadoc asserted that every
 * public method was {@code readOnly}.
 * <p>
 * Scope is deliberately narrow — public methods on {@code *ReadServiceImpl} that call a {@code *Dao}
 * directly. A method that only delegates to another service is out of scope: the transaction belongs where
 * the DAO is touched, and widening this to every {@code *ServiceImpl} would flag delegation chains that are
 * correct as they stand.
 * <p>
 * A class-level {@code @Transactional} satisfies the rule, since Spring applies it to every public method.
 * <p>
 * "Calls a {@code *Dao}" over-approximates: a handful of DAO methods never reach a session (they return a
 * precomputed field, or read the JPA metamodel, which is static mapping configuration). Those carry
 * {@link SuppressArchUnit}{@code ("ReadServiceTransactional")} with a comment saying which, rather than a
 * pointless annotation that would open a transaction nothing uses.
 *
 * @author gemma
 */
@AnalyzeClasses(
        packages = "ubic.gemma.persistence.service",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ReadServiceTransactionalRuleTest {

    /** Scopes a {@link SuppressArchUnit} marker to this rule alone. */
    private static final String SUPPRESSION = "ReadServiceTransactional";

    private static final DescribedPredicate<JavaClass> READ_SERVICE_IMPL =
            new DescribedPredicate<JavaClass>( "read-service implementations" ) {
                @Override
                public boolean test( JavaClass javaClass ) {
                    return javaClass.getSimpleName().endsWith( "ReadServiceImpl" );
                }
            };

    private static boolean callsADao( JavaMethod method ) {
        for ( JavaMethodCall call : method.getMethodCallsFromSelf() ) {
            String owner = call.getTargetOwner().getSimpleName();
            if ( owner.endsWith( "Dao" ) ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransactional( JavaMethod method ) {
        return method.isAnnotatedWith( Transactional.class )
                || method.getOwner().isAnnotatedWith( Transactional.class );
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

    private static final ArchCondition<JavaMethod> DECLARE_A_TRANSACTION_WHEN_REACHING_A_DAO =
            new ArchCondition<JavaMethod>( "declare @Transactional when they reach a DAO" ) {
                @Override
                public void check( JavaMethod method, ConditionEvents events ) {
                    if ( !callsADao( method ) || isTransactional( method ) || isSuppressed( method ) ) {
                        return;
                    }
                    events.add( SimpleConditionEvent.violated( method, String.format(
                            "%s.%s reaches a DAO without @Transactional — it will fail at runtime with "
                                    + "\"Could not obtain transaction-synchronized Session for current thread\"",
                            method.getOwner().getSimpleName(), method.getName() ) ) );
                }
            };

    /**
     * 🛑 <b>The guard against a guard that checks nothing.</b>
     * <p>
     * ArchUnit reads compiled bytecode through a bundled ASM. When that ASM is older than the class file
     * version we compile to, it imports <b>zero classes and does not complain</b> — every rule then passes
     * for a reason that has nothing to do with the codebase. That is not hypothetical: archunit 1.3.0 could
     * not read Java 25 bytecode (major version 69), so this rule and
     * {@link AutowireImplRuleTest} both ran green while checking nothing at all, and removing the annotation
     * whose absence had just 500'd production changed no result. Fixed by archunit 1.4.1.
     * <p>
     * {@code allowEmptyShould} is what converts that failure into a pass, so this rule leaves it {@code false}
     * and this sentinel states the expectation directly: an empty import is a broken toolchain, never a clean
     * codebase.
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
     * {@code allowEmptyShould} stays {@code false} deliberately — see
     * {@link #classes_are_actually_imported}. If the {@code that()} clause ever stops matching, that is a
     * signal worth a red build, not something to suppress.
     */
    @ArchTest
    public static final ArchRule read_service_methods_reaching_a_dao_must_be_transactional =
            methods()
                    .that().arePublic()
                    .and().areDeclaredInClassesThat( READ_SERVICE_IMPL )
                    .should( DECLARE_A_TRANSACTION_WHEN_REACHING_A_DAO )
                    .allowEmptyShould( false );
}
