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
package ubic.gemma.search;

import org.springframework.security.access.annotation.Secured;

/**
 * @author paul
 * @version $Id$
 */
public interface IndexService {

    @Secured("GROUP_AGENT")
    public String runAll();

    /**
     * Indexes expression experiments, genes, array designs, probes and bibliographic references. This is a convenience
     * method for Quartz to schedule indexing of the entire database.
     */
    @Secured("GROUP_AGENT")
    public void indexAll();

    /**
     * Indexes array designs.
     */
    @Secured("GROUP_AGENT")
    public void indexArrayDesigns();

    /**
     * Indexes bibliographic references.
     */
    @Secured("GROUP_AGENT")
    public void indexBibligraphicReferences();

    /**
     * Indexes sequences
     */
    @Secured("GROUP_AGENT")
    public void indexBioSequences();

    /**
     * Indexes expression experiments.
     */
    @Secured("GROUP_AGENT")
    public void indexExpressionExperiments();

    /**
     * Indexes genes.
     */
    @Secured("GROUP_AGENT")
    public void indexGenes();

    /**
     * Indexes probes.
     */
    @Secured("GROUP_AGENT")
    public void indexProbes();

    public String run( Object command, String spaceUrl, String taskName, boolean runInWebapp );

}