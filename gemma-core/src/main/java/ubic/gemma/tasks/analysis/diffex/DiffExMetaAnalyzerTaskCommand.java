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
package ubic.gemma.tasks.analysis.diffex;

import java.util.Collection;

import ubic.gemma.job.TaskCommand;

/**
 * A command object to be used by spaces.
 * 
 * @author frances
 * @version $Id$
 */
public class DiffExMetaAnalyzerTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private Collection<Long> analysisResultSetIds;
    private String name;
    private String description;
    private boolean persist = false;

    public boolean isPersist() {
        return this.persist;
    }

    public void setPersist( boolean persist ) {
        this.persist = persist;
    }

    public DiffExMetaAnalyzerTaskCommand( Collection<Long> analysisResultSetIds ) {
        this.analysisResultSetIds = analysisResultSetIds;
    }

    public DiffExMetaAnalyzerTaskCommand( Collection<Long> analysisResultSetIds, String name, String description,
            boolean persist ) {
        this.analysisResultSetIds = analysisResultSetIds;
        this.name = name;
        this.description = description;
        this.persist = persist;
    }

    public Collection<Long> getAnalysisResultSetIds() {
        return this.analysisResultSetIds;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public Class getTaskClass() {
        return DiffExMetaAnalyzerTask.class;
    }
}
