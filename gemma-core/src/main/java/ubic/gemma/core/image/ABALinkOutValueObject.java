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
package ubic.gemma.core.image;

import java.io.Serializable;
import java.util.Collection;

/**
 * Value Object for transporting details needed from other websites to provide convenient links to them in gemma
 *
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ABALinkOutValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Collection<String> abaGeneImageUrls; // The url of a specific brain image
    private String abaGeneUrl; // The url of the details page for gene information in the allen brain atlas web site

    private String geneSymbol;

    public ABALinkOutValueObject() {
        super();
    }

    public ABALinkOutValueObject( Collection<String> abaGeneImageUrls, String abaGeneUrl, String geneSymbol ) {
        this();
        this.abaGeneImageUrls = abaGeneImageUrls;
        this.abaGeneUrl = abaGeneUrl;
        this.geneSymbol = geneSymbol;
    }

    public Collection<String> getAbaGeneImageUrls() {
        return abaGeneImageUrls;
    }

    public String getAbaGeneUrl() {
        return abaGeneUrl;
    }

    public void setAbaGeneUrl( String abaGeneUrl ) {
        this.abaGeneUrl = abaGeneUrl;
    }

    /**
     * @return the geneSymbol
     */
    public String getGeneSymbol() {
        return geneSymbol;
    }

    /**
     * @param geneSymbol the geneSymbol to set
     */
    public void setGeneSymbol( String geneSymbol ) {
        this.geneSymbol = geneSymbol;
    }

    public Collection<String> getAbaGeneImageUrl() {
        return abaGeneImageUrls;
    }

    public void setAbaGeneImageUrl( Collection<String> abaGeneImageUrls ) {
        this.abaGeneImageUrls = abaGeneImageUrls;
    }

}
