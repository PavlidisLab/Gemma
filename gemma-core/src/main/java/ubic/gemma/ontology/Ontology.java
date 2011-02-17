/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.ontology;

import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class Ontology {

    private ExternalDatabase externalDatabase;

    public Ontology( ExternalDatabase database ) {
        this.externalDatabase = database;
    }

    public String getDescription() {
        return externalDatabase.getDescription();
    }

    public String getName() {
        return externalDatabase.getName();
    }

    public String getWebUri() {
        return externalDatabase.getWebUri();
    }

    @Override
    public String toString() {
        if ( this.getName() != null )
            return this.getName();
        return this.getWebUri();
    }

}
