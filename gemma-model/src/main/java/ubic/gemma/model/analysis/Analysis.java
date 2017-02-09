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

package ubic.gemma.model.analysis;

/**
 * An analysis of one or more Investigations. The manner in which the analysis was done is described in the Protocol and
 * Description associations. Analyses which use more than one Investigation are meta-analyses.
 */
public abstract class Analysis extends ubic.gemma.model.common.Auditable {

    /**
     * 
     */
    private static final long serialVersionUID = -7666181528240555473L;
    private ubic.gemma.model.common.protocol.Protocol protocol;
    private ubic.gemma.model.common.description.ExternalDatabase source;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Analysis() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.protocol.Protocol getProtocol() {
        return this.protocol;
    }

    /**
     * (Optional) Where the relationship came from. For example, Gene Ontology terms associated with genes can come from
     * multiple sources The external source of the analysis.
     */
    public ubic.gemma.model.common.description.ExternalDatabase getSource() {
        return this.source;
    }

    public void setProtocol( ubic.gemma.model.common.protocol.Protocol protocol ) {
        this.protocol = protocol;
    }

    public void setSource( ubic.gemma.model.common.description.ExternalDatabase source ) {
        this.source = source;
    }

}