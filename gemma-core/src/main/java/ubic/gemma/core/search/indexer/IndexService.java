/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.core.search.indexer;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.core.tasks.maintenance.IndexerTaskCommand;

/**
 * @author paul
 */
@SuppressWarnings("unused") // Used by scheduler, possible external use
public interface IndexService {

    /**
     * @param command command
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String index( IndexerTaskCommand command );

    /**
     * Indexes expression experiments, genes, array designs, probes and bibliographic references. This is a convenience
     * method for Quartz to schedule indexing of the entire database.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexAll();

    /**
     * Indexes array designs.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexArrayDesigns();

    /**
     * Indexes bibliographic references.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexBibligraphicReferences();

    /**
     * Indexes sequences
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexBioSequences();

    /**
     * Indexes expression experiments.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexExpressionExperiments();

    /**
     * Indexes genes.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexGenes();

    /**
     * Indexes probes.
     * 
     * @return taskId
     */
    @Secured("GROUP_AGENT")
    String indexProbes();

}