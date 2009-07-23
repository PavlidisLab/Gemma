/*
 * The Gemma project
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
package ubic.gemma.image;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author kelsey
 * @version $Id Value Object for transporting details needd from other websites to provide convinient links to them in
 *          gemma
 */

public class LinkOutValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Collection<String> abaGeneImageUrls; // The url of a specific brain image
    private String abaGeneUrl; // The url of the details page for gene information in the allen brain atlas web site

    public LinkOutValueObject( Collection<String> abaGeneImageUrls, String abaGeneUrl ) {
        this();
        this.abaGeneImageUrls = abaGeneImageUrls;
        this.abaGeneUrl = abaGeneUrl;
    }

    public LinkOutValueObject() {
        super();
    }

    public Collection<String> getAbaGeneImageUrls() {
        return abaGeneImageUrls;
    }

    public void setAbaGeneImageUrl( Collection<String> abaGeneImageUrls ) {
        this.abaGeneImageUrls = abaGeneImageUrls;
    }

    public String getAbaGeneUrl() {
        return abaGeneUrl;
    }

    public void setAbaGeneUrl( String abaGeneUrl ) {
        this.abaGeneUrl = abaGeneUrl;
    }

}
