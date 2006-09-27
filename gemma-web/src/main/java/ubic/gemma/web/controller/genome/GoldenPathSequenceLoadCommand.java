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
package ubic.gemma.web.controller.genome;

import ubic.gemma.model.genome.Taxon;

/**
 * Hold data for Golden Path sequence loading
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathSequenceLoadCommand {

    int limit = -1;

    Taxon taxon;

    /**
     * @return the limit
     */
    public int getLimit() {
        return this.limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit( int limit ) {
        this.limit = limit;
    }

    /**
     * @return the taxon
     */
    public Taxon getTaxon() {
        return this.taxon;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

}
