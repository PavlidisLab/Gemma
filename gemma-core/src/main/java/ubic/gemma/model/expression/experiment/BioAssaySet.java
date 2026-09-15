/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
 *
 */

package ubic.gemma.model.expression.experiment;

import jakarta.persistence.Entity;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Set;

/**
 * Represents a set of {@link BioAssay}s.
 * <p>
 * This is not associated with any actual data, and soley represents a logical grouping of "samples" that can be used
 * for any purpose. These could be a published grouping, or a subset of samples from a published study.
 * <p>
 * The {@code bioAssays} association is declared independently on each concrete subclass —
 * {@link ExpressionExperiment} stores them as a one-to-many on the {@code BIO_ASSAY} table, while
 * {@link ExpressionExperimentSubSet} stores them via the {@code BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET}
 * many-to-many join table. The getter is abstract here so callers can target {@code BioAssaySet}
 * polymorphically without forcing JPA to pick one mapping at the intermediate level.
 * @see ExpressionExperiment
 * @see ExpressionExperimentSubSet
 */
@Entity
public abstract class BioAssaySet extends Investigation {

    public abstract Set<BioAssay> getBioAssays();

    public abstract void setBioAssays( Set<BioAssay> bioAssays );
}
