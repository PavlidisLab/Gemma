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
package ubic.gemma.core.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.architecture.SuppressArchUnit;

import java.util.List;
import java.util.Optional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noConstructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Phase 3 build hygiene: forbid declaring Spring-injected dependencies with
 * an {@code Impl}-typed reference in production code.
 *
 * <p>When Spring's AOP machinery proxies a bean (the most common cause being
 * a {@code @Transactional} method, but {@code @Cacheable}, {@code @Async},
 * {@code @PreAuthorize} and similar advice all do the same), the resulting
 * proxy implements the bean's interfaces but is <em>not</em> a subclass of
 * the underlying {@code Impl}. A field declared with the {@code Impl} type
 * cannot be satisfied at injection time and Spring fails with
 * {@code BeanNotOfRequiredTypeException} (JDK dynamic proxy) or causes
 * subtle aliasing bugs under CGLIB. The cure is to declare the field with
 * the interface type — Spring then injects the proxy transparently.
 *
 * <p>This rule was prompted by the AspectJ deeper recce
 * (commit {@code b16450a5e8} on the unmerged {@code worktree-aspectj-deeper}
 * branch), which identified one historical instance of the pattern
 * ({@code EeWriteServiceImpl.persisterHelper}) that has since been
 * remediated. The rule locks the codebase down against re-introduction.
 *
 * <p>Two complementary rules ship together:
 * <ul>
 *   <li>{@link #autowired_fields_must_not_be_impl_typed} — field injection
 *       via {@code @Autowired}.</li>
 *   <li>{@link #constructor_parameters_must_not_be_impl_typed} — constructor
 *       injection (the preferred DI style for Java-config Spring 6).</li>
 * </ul>
 *
 * <p>Scope: production classes only (tests excluded via
 * {@link ImportOption.DoNotIncludeTests}). Mocks and test doubles
 * legitimately need concrete types.
 */
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(
        packages = "ubic.gemma",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class AutowireImplRuleTest {

    private static final DescribedPredicate<JavaClass> IMPL_TYPED =
            new DescribedPredicate<JavaClass>( "have a simple name ending with 'Impl'" ) {
                @Override
                public boolean test( JavaClass javaClass ) {
                    return javaClass.getSimpleName().endsWith( "Impl" );
                }
            };

    /**
     * Fields explicitly marked {@code @SuppressArchUnit("AutowireImpl")} are
     * exempt: the marker documents an intentional, justified exception (e.g.
     * {@code @Lazy @Autowired ImplType} fields used to break a Spring DI
     * cycle where the Impl reference is required to reach a non-interface
     * method). The value attribute scopes the suppression to this rule
     * alone — a marker for any other rule does NOT exempt the field here.
     */
    private static final DescribedPredicate<JavaField> SUPPRESSED_AUTOWIRE_IMPL =
            new DescribedPredicate<JavaField>( "annotated with @SuppressArchUnit(\"AutowireImpl\")" ) {
                @Override
                public boolean test( JavaField field ) {
                    Optional<JavaAnnotation<JavaField>> marker =
                            field.tryGetAnnotationOfType( SuppressArchUnit.class.getName() );
                    if ( !marker.isPresent() ) {
                        return false;
                    }
                    Optional<Object> value = marker.get().tryGetExplicitlyDeclaredProperty( "value" );
                    return value.isPresent() && "AutowireImpl".equals( value.get() );
                }
            };

    @ArchTest
    public static final ArchRule autowired_fields_must_not_be_impl_typed =
            noFields()
                    .that().areAnnotatedWith( Autowired.class )
                    .and( DescribedPredicate.not( SUPPRESSED_AUTOWIRE_IMPL ) )
                    .should().haveRawType( IMPL_TYPED )
                    .because( "@Autowired on Impl-typed fields breaks when Spring wraps the bean in a "
                            + "JDK dynamic proxy (BeanNotOfRequiredTypeException) or causes aliasing "
                            + "bugs under CGLIB. Declare the field with the interface type instead. "
                            + "Targeted exceptions (e.g. @Lazy circular-DI bridges) must carry "
                            + "@SuppressArchUnit(\"AutowireImpl\") with a Javadoc rationale." );

    @ArchTest
    public static final ArchRule constructor_parameters_must_not_be_impl_typed =
            noConstructors()
                    .that().areAnnotatedWith( Autowired.class )
                    .or().areDeclaredInClassesThat().areAnnotatedWith( "org.springframework.stereotype.Service" )
                    .or().areDeclaredInClassesThat().areAnnotatedWith( "org.springframework.stereotype.Component" )
                    .or().areDeclaredInClassesThat().areAnnotatedWith( "org.springframework.stereotype.Repository" )
                    .or().areDeclaredInClassesThat().areAnnotatedWith( "org.springframework.stereotype.Controller" )
                    .should().haveRawParameterTypes( anyParameterMatches( IMPL_TYPED ) )
                    .because( "Constructor-injected dependencies typed as Impl break when the source bean is "
                            + "wrapped in an AOP proxy. Declare the parameter with the interface type." );

    private static DescribedPredicate<List<JavaClass>> anyParameterMatches( DescribedPredicate<JavaClass> inner ) {
        return new DescribedPredicate<List<JavaClass>>( "any parameter " + inner.getDescription() ) {
            @Override
            public boolean test( List<JavaClass> params ) {
                for ( JavaClass p : params ) {
                    if ( inner.test( p ) ) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
