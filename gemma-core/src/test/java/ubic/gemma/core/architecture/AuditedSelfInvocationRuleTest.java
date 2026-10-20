package ubic.gemma.core.architecture;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.core.security.audit.AuditedConditional;
import ubic.gemma.core.security.audit.AuditedOnError;
import ubic.gemma.core.security.audit.AuditedOnErrors;

import java.util.Set;
import java.util.TreeSet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * An audit annotation only records an event when the call reaches the method through its Spring proxy.
 * <p>
 * {@code AuditedAspect} matches {@code @annotation(...)}: the advice runs on a call that goes through the bean's
 * proxy, and on nothing else. A call on {@code this} bypasses the proxy, so the event is silently not written; a
 * private or static method is never proxied at all. The Phase C migration replaced imperative
 * {@code addUpdateEvent} calls, which fired whatever the entry path, with these annotations, so an entry path that
 * reaches the annotated method through {@code this} lost its event without anything failing. Two were found by
 * reading on 2026-09-18: {@code switchExperimentToMergedPlatform} calling {@code switchExperimentToArrayDesign}, and
 * the two-argument {@code createProcessedDataVectors} calling the three-argument one.
 * <p>
 * The fix is to call through a {@code @Lazy @Autowired} self-reference, or through a co-bean.
 *
 * <h2>How a call on {@code this} is told apart from a call on a self-reference</h2>
 * The bytecode records the static type the method was invoked on, not the receiver. An interface-typed
 * self-reference is therefore invisible to this rule: the call's owner is the interface. A class-typed one looks
 * exactly like {@code this}, except that the same line reads the self-reference field first; a call whose line reads
 * a field of the caller's own type is taken to be through that field.
 */
@AnalyzeClasses(
        packages = "ubic.gemma",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class AuditedSelfInvocationRuleTest {

    private static final Set<String> AUDIT_ANNOTATIONS = Set.of(
            Audited.class.getName(),
            AuditedConditional.class.getName(),
            AuditedOnError.class.getName(),
            AuditedOnErrors.class.getName() );

    private static boolean isAudited( JavaMethod method ) {
        return method.getAnnotations().stream()
                .anyMatch( a -> AUDIT_ANNOTATIONS.contains( a.getRawType().getName() ) );
    }

    /**
     * The event types a method's audit annotations record, as {@code Annotation:EventType}.
     */
    private static Set<String> auditedEvents( JavaMethod method ) {
        Set<String> events = new TreeSet<>();
        for ( JavaAnnotation<?> a : method.getAnnotations() ) {
            if ( a.getRawType().isEquivalentTo( AuditedOnErrors.class ) ) {
                Object value = a.get( "value" ).orElse( null );
                if ( value instanceof JavaAnnotation<?>[] ) {
                    for ( JavaAnnotation<?> inner : ( JavaAnnotation<?>[] ) value ) {
                        events.add( eventOf( inner ) );
                    }
                }
            } else if ( AUDIT_ANNOTATIONS.contains( a.getRawType().getName() ) ) {
                events.add( eventOf( a ) );
            }
        }
        return events;
    }

    private static String eventOf( JavaAnnotation<?> a ) {
        Object value = a.get( "value" ).orElse( null );
        String event = value instanceof JavaClass ? ( ( JavaClass ) value ).getName() : String.valueOf( value );
        return a.getRawType().getSimpleName() + ":" + event;
    }

    /**
     * Whether the call is made on the caller's own instance: invoked on the caller's class or one of its
     * superclasses, and not on a field of the caller's type read on the same line.
     */
    private static boolean isOnThis( JavaMethod caller, JavaMethodCall call ) {
        JavaClass owner = call.getTargetOwner();
        if ( owner.isInterface() || !caller.getOwner().isAssignableTo( owner.getName() ) ) {
            return false;
        }
        for ( JavaFieldAccess access : caller.getFieldAccesses() ) {
            if ( access.getLineNumber() == call.getLineNumber()
                    && access.getTarget().getRawType().isAssignableTo( owner.getName() ) ) {
                return false;
            }
        }
        return true;
    }

    @ArchTest
    public static void classes_are_actually_imported( JavaClasses classes ) {
        if ( classes.isEmpty() ) {
            throw new AssertionError( "ArchUnit imported no classes at all, so every rule here is vacuous. "
                    + "Usual cause: the bundled ASM cannot read our class file version." );
        }
    }

    @ArchTest
    public static final ArchRule audited_methods_are_not_called_on_this =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage( "ubic.gemma.." )
                    .should( new ArchCondition<JavaMethod>( "not call an audited method on this" ) {
                        @Override
                        public void check( JavaMethod caller, ConditionEvents events ) {
                            for ( JavaMethodCall call : caller.getMethodCallsFromSelf() ) {
                                call.getTarget().resolveMember().ifPresent( target -> {
                                    // a caller that records every event the target would is not losing one: the
                                    // skipped inner advice would only have written a duplicate
                                    if ( isAudited( target ) && isOnThis( caller, call )
                                            && !auditedEvents( caller ).containsAll( auditedEvents( target ) ) ) {
                                        events.add( SimpleConditionEvent.violated( caller, String.format(
                                                "%s.%s calls the audited %s on this (line %d), so %s is not written. "
                                                        + "Call it through a @Lazy @Autowired self-reference or a co-bean.",
                                                caller.getOwner().getSimpleName(), caller.getName(),
                                                target.getName(), call.getLineNumber(),
                                                auditedEvents( target ) ) ) );
                                    }
                                } );
                            }
                        }
                    } )
                    .allowEmptyShould( false );

    @ArchTest
    public static final ArchRule audited_methods_can_be_proxied =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage( "ubic.gemma.." )
                    .should( new ArchCondition<JavaMethod>( "be proxiable if audited" ) {
                        @Override
                        public void check( JavaMethod method, ConditionEvents events ) {
                            if ( isAudited( method ) && ( method.getModifiers().contains( JavaModifier.PRIVATE )
                                    || method.getModifiers().contains( JavaModifier.STATIC )
                                    || method.getModifiers().contains( JavaModifier.FINAL ) ) ) {
                                events.add( SimpleConditionEvent.violated( method, String.format(
                                        "%s.%s is audited but %s, so no proxy can intercept it and no event is written.",
                                        method.getOwner().getSimpleName(), method.getName(), method.getModifiers() ) ) );
                            }
                        }
                    } )
                    .allowEmptyShould( false );
}
