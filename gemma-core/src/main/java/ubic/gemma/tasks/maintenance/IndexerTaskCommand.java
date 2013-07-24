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
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.Task;

/**
 * @author klc
 * @version $Id$
 */

public class IndexerTaskCommand extends TaskCommand {

    private static final int INDEXER_MAX_RUNTIME = 300; // Minutes

    private static final long serialVersionUID = -8994831072852393919L;

    private boolean compassOn = false;

    private boolean indexAD;

    private boolean indexBibRef;

    private boolean indexBioSequence;

    private boolean indexEE;

    private boolean indexExperimentSet = true;

    private boolean indexGene;

    private boolean indexGeneSet = true;

    private boolean indexOntologies;

    private boolean indexProbe;

    public IndexerTaskCommand() {
        super();
        this.setMaxRuntime( INDEXER_MAX_RUNTIME );
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return ( Class<? extends Task<TaskResult, ? extends TaskCommand>> ) IndexerTask.class;
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

    /**
     * @return the indexExperimentSet
     */
    public boolean isIndexExperimentSet() {
        return indexExperimentSet;
    }

    public boolean isIndexGene() {
        return indexGene;
    }

    /**
     * @return the indexGeneSet
     */
    public boolean isIndexGeneSet() {
        return indexGeneSet;
    }

    public boolean isIndexOntologies() {
        return indexOntologies;
    }

    public boolean isIndexProbe() {
        return indexProbe;
    }

    /**
     * Indexing of probes and BioSequences sometimes bails because of the size of the index created. Also their data
     * rarely changes so there is not much value in indexing it every week. Indexing of probes and biosequences can
     * still be triggered manually.
     * 
     * @param all
     */
    public void setAll( boolean all ) {
        setIndexAD( all );
        setIndexBibRef( all );
        setIndexBioSequence( false );
        setIndexEE( all );
        setIndexGene( false );
        setIndexProbe( false );
        setIndexOntologies( true );
        this.setIndexExperimentSet( all );
        this.setIndexGeneSet( all );
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

    /**
     * @param indexExperimentSet the indexExperimentSet to set
     */
    public void setIndexExperimentSet( boolean indexExperimentSet ) {
        this.indexExperimentSet = indexExperimentSet;
    }

    public void setIndexGene( boolean indexGene ) {
        this.indexGene = indexGene;
    }

    /**
     * @param indexGeneSet the indexGeneSet to set
     */
    public void setIndexGeneSet( boolean indexGeneSet ) {
        this.indexGeneSet = indexGeneSet;
    }

    public void setIndexOntologies( boolean indexOntologies ) {
        this.indexOntologies = indexOntologies;
    }

    public void setIndexOntology( boolean indexBioSequence ) {
        this.indexBioSequence = indexBioSequence;
    }

    public void setIndexProbe( boolean indexProbe ) {
        this.indexProbe = indexProbe;
    }

}
