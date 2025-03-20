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

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a set of {@link BioAssay}s.
 * <p>
 * This is not associated with any actual data, and soley represents a logical grouping of "samples" that can be used
 * for any purpose. These could be a published grouping, or a subset of samples from a published study.
 * @see ExpressionExperiment
 * @see ExpressionExperimentSubSet
 */
public abstract class BioAssaySet extends Investigation {

    @Nullable
    private DatabaseEntry accession;
    private Set<BioAssay> bioAssays = new HashSet<>();

    @Nullable
    public DatabaseEntry getAccession() {
        return this.accession;
    }

    public void setAccession( @Nullable DatabaseEntry accession ) {
        this.accession = accession;
    }

    public Set<BioAssay> getBioAssays() {
        return bioAssays;
    }

    public void setBioAssays( Set<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }
}