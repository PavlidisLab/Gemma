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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks work that runs long enough that no transaction may be open while it runs.
 *
 * <h2>What it is for</h2>
 *
 * <p>Gemma sets {@code hibernate.connection.handling_mode = DELAYED_ACQUISITION_AND_HOLD}
 * ({@code HibernateConfig}), so a transaction holds its pooled connection from first statement to
 * commit — including through any stretch that issues no statements at all. The pool is configured
 * to recycle connections every 30 minutes ({@code gemma.db.hikari.maxLifetime}). A computation
 * that runs inside a transaction for longer than that is holding a connection the pool expects to
 * have turned over, and it is holding it for no reason: the computation does not use it.</p>
 *
 * <p>Measured on production 2026-09-17: {@code corrMat -force} on GSE260875 spent 44 minutes in
 * quantile normalization inside one {@code @Transactional(readOnly = true)}, then failed on its
 * next statement. Reads and writes on that path are cleanly separable from the arithmetic between
 * them, which is what makes holding the connection across it a defect rather than a cost.</p>
 *
 * <h2>The shape that is correct</h2>
 *
 * <p>Read in a short transaction, compute with none open, write in a short transaction. The
 * orchestrating class declares {@code @Transactional(propagation = Propagation.NEVER)} — the idiom
 * already used by {@code PreprocessorServiceImpl},
 * {@code DifferentialExpressionAnalyzerServiceImpl}, {@code OutlierFlaggingServiceImpl},
 * {@code DataUpdaterImpl} and a dozen others — and each step it calls carries its own annotation.
 * Because Spring AOP does not intercept private or self-invoked methods, the steps have to live on
 * a bean the orchestrator calls through its proxy; {@code *HelperServiceImpl} is the precedent.</p>
 *
 * <h2>Where to put it</h2>
 *
 * <p>On the method or constructor that <em>is</em> the computation, or on the class when every
 * entry point into it is one ({@code LeastSquaresFit} has eight constructors and all of them run
 * the fit). Do not put it on an orchestrator that merely reaches a computation — that is the thing
 * the rule is looking for, not a declaration.</p>
 *
 * <p>{@code TransactionSpanningComputeRuleTest} fails the build when a {@code @Transactional}
 * method can reach one of these. Retention is {@code RUNTIME} because ArchUnit reads compiled
 * bytecode.</p>
 *
 * @see SuppressArchUnit
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface LongComputation {

    /**
     * Optional note on what makes this one long — a measured duration, a complexity, the dataset
     * that showed it. Read by whoever hits the rule and has to decide where the transaction
     * boundary belongs.
     */
    String value() default "";
}
