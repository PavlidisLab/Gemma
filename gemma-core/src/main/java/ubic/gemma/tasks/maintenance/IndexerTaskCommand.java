/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.tasks.maintenance;

import ubic.gemma.job.TaskCommand;

/**
 * @author klc
 * @version $Id$
 */

public class IndexerTaskCommand extends TaskCommand {

    private static final int INDEXER_MAX_RUNTIME = 120; //Minutes

    private static final long serialVersionUID = -8994831072852393919L;

    private boolean compassOn = false;

    private boolean indexAD;

    private boolean indexBibRef;

    private boolean indexBioSequence;

    private boolean indexEE;

    private boolean indexGene;

    private boolean indexProbe;

    public IndexerTaskCommand() {
        super();
        this.setMaxRuntime( INDEXER_MAX_RUNTIME );
    }

    public boolean isCompassOn() {
        return compassOn;
    }

    public boolean isIndexAD() {
        return indexAD;
    }

    public boolean isIndexBibRef() {
        return indexBibRef;
    }

    public boolean isIndexBioSequence() {
        return indexBioSequence;
    }

    public boolean isIndexEE() {
        return indexEE;
    }

    public boolean isIndexGene() {
        return indexGene;
    }

    public boolean isIndexProbe() {
        return indexProbe;
    }

    public void setAll( boolean all ) {
        setIndexAD( all );
        setIndexBibRef( all );
        setIndexBioSequence( all );
        setIndexEE( all );
        setIndexGene( all );
        setIndexProbe( all );
    }

    public void setCompassOn( boolean compassOn ) {
        this.compassOn = compassOn;
    }

    public void setIndexAD( boolean indexAD ) {
        this.indexAD = indexAD;
    }

    public void setIndexBibRef( boolean indexBibRef ) {
        this.indexBibRef = indexBibRef;
    }

    public void setIndexBioSequence( boolean indexBioSequence ) {
        this.indexBioSequence = indexBioSequence;
    }

    public void setIndexEE( boolean indexEE ) {
        this.indexEE = indexEE;
    }

    public void setIndexGene( boolean indexGene ) {
        this.indexGene = indexGene;
    }

    public void setIndexOntology( boolean indexBioSequence ) {
        this.indexBioSequence = indexBioSequence;
    }

    public void setIndexProbe( boolean indexProbe ) {
        this.indexProbe = indexProbe;
    }

}
