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
package ubic.gemma.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.util.CompassUtils;

/**
 * @author keshav
 * @version $Id$
 */
public class IndexService {
    /*
     * IndexService has not been configured with xdoclet tags because we it needs to reside in
     * applicationContext-compass.xml so we can choose whether or not to load this part of the application context at
     * spring startup.
     */
    private Log log = LogFactory.getLog( this.getClass() );

    private CompassGpsInterfaceDevice expressionGps;
    private CompassGpsInterfaceDevice geneGps;
    private CompassGpsInterfaceDevice arrayGps;
    private CompassGpsInterfaceDevice ontologyGps;

    /**
     * Indexes expression experiments, genes, and array designs. This is a convenience method for Quartz to schedule
     * indexing of the entire database.
     */
    public void indexAll() {
        log.debug( "rebuilding compass index" );
        CompassUtils.rebuildCompassIndex( expressionGps );
        CompassUtils.rebuildCompassIndex( geneGps );
        CompassUtils.rebuildCompassIndex( arrayGps );
        CompassUtils.rebuildCompassIndex( ontologyGps );

    }

    /**
     * Indexes expression experiments.
     */
    public void indexExpressionExperiments() {
        CompassUtils.rebuildCompassIndex( expressionGps );
    }

    /**
     * Indexes genes.
     */
    public void indexGenes() {
        CompassUtils.rebuildCompassIndex( geneGps );
    }

    /**
     * Indexes array designs.
     */
    public void indexArrayDesigns() {
        CompassUtils.rebuildCompassIndex( arrayGps );
    }

    /**
     * Indexes Ontology Entries.
     */
    public void indexOntologyEntries() {
        CompassUtils.rebuildCompassIndex( ontologyGps );
    }

    /**
     * @param expressionGps The expressionGps to set.
     */
    public void setExpressionGps( CompassGpsInterfaceDevice expressionGps ) {
        this.expressionGps = expressionGps;
    }

    /**
     * @param arrayGps The arrayGps to set.
     */
    public void setArrayGps( CompassGpsInterfaceDevice arrayGps ) {
        this.arrayGps = arrayGps;
    }

    /**
     * @param geneGps The geneGps to set.
     */
    public void setGeneGps( CompassGpsInterfaceDevice geneGps ) {
        this.geneGps = geneGps;
    }

    /**
     * @param ontologyGps The ontologyGps to set.
     */
    public void setOntologyGps( CompassGpsInterfaceDevice ontologyGps ) {
        this.ontologyGps = ontologyGps;
    }

}
