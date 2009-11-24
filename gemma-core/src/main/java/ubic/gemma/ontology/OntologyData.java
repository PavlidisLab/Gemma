/*
 * The GemmaOnt project
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

import java.io.Serializable;

/**
 * @author klc Used as a Datapack for sending ajax data back via the dwr remote call
 */

public class OntologyData implements Serializable {

    private static final String NA = "NA";

    /**
     * 
     */
    private static final long serialVersionUID = 4007478680683944730L;

    int id;
    String term;
    String comment;

    /**
     * @return the data
     */

    public OntologyData() {
        super();
        id = 1;
        term = NA;
        comment = NA;

    }

    public OntologyData( int id, String term, String comment ) {
        this();
        this.id = id;
        this.term = term;
        this.comment = comment;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment( String comment ) {
        this.comment = comment;
    }

    /**
     * @param id the id to set
     */
    public void setId( int id ) {
        this.id = id;
    }

    /**
     * @param term the term to set
     */
    public void setTerm( String term ) {
        this.term = term;
    }

}
