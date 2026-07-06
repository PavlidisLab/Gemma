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
package ubic.gemma.persistence.service.expression.experiment;

import java.util.Date;

/**
 * Per-section change tallies from one {@link ExpressionExperimentService#commitCuration} call, mapped by the
 * web layer to the wire {@code CurationCommitReport.changes}. Counts are the same whether the commit was
 * applied or a dry run (preflight) — a preflight computes them without writing.
 */
public class CurationCommitResult {

    private boolean basicsChanged;
    private int publicationsCreated;
    private int publicationsDeleted;
    private int publicationsUnchanged;
    /** The dataset's {@code lastUpdated} after the commit — the client's baseline for the next draft. */
    private Date newLastUpdated;

    public boolean isBasicsChanged() {
        return basicsChanged;
    }

    public void setBasicsChanged( boolean basicsChanged ) {
        this.basicsChanged = basicsChanged;
    }

    public int getPublicationsCreated() {
        return publicationsCreated;
    }

    public void setPublicationsCreated( int publicationsCreated ) {
        this.publicationsCreated = publicationsCreated;
    }

    public int getPublicationsDeleted() {
        return publicationsDeleted;
    }

    public void setPublicationsDeleted( int publicationsDeleted ) {
        this.publicationsDeleted = publicationsDeleted;
    }

    public int getPublicationsUnchanged() {
        return publicationsUnchanged;
    }

    public void setPublicationsUnchanged( int publicationsUnchanged ) {
        this.publicationsUnchanged = publicationsUnchanged;
    }

    public Date getNewLastUpdated() {
        return newLastUpdated;
    }

    public void setNewLastUpdated( Date newLastUpdated ) {
        this.newLastUpdated = newLastUpdated;
    }
}
