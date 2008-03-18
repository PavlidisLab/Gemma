/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

/**
 * Given an Expression Experiment ID, will return a collection of Design Element Data Vectors and the corresponding
 * composite gene sequences.
 * 
 * @author gavin, klc
 * @version$Id$
 */

public class Probe2GeneEndpoint extends AbstractGemmaEndpoint {

    private static Log log = LogFactory.getLog( ExperimentDEDVEndpoint.class );

    
    private CompositeSequenceService compositeSequenceService;
    
    private ArrayDesignService arrayDesignService;
   

    /**
     * The local name of the expected request/response.
     */
    private static final String PROBE_LOCAL_NAME = "probe2Gene";
    private static final String DELIMITER = " ";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }
    
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @SuppressWarnings("unchecked")
    protected Element invokeInternal( Element requestElement, Document document ) throws Exception {
        setLocalName( PROBE_LOCAL_NAME );
       
        String probeid = "";
        Collection<String> probeResults = getNodeValues( requestElement, "probe_id" );
        for ( String id : probeResults ) {
            probeid = id;
        }
        
        String adid = "";
        Collection<String> adResults = getNodeValues( requestElement, "array_design_identifier" );
        for ( String id : adResults ) {
            adid = id;
        }

        //get genes, given probe (which is unique to an Array Design
        //therefore, the Array Design Identifier input is not really necessary
        CompositeSequence cs = compositeSequenceService.load( Long.parseLong( probeid ) );
        Collection<Gene> geneCol = compositeSequenceService.getGenes( cs );
        
        // start building the wrapper
        // build xml manually rather than use buildWrapper inherited from AbstractGemmeEndpoint
        String elementName1 = "geneIdList";


        //build results in the form of a collection
        Collection<String> geneIds = new HashSet<String>();
        for (Gene gene : geneCol){
            geneIds.add( gene.getId().toString() );
        }
        
        return buildWrapper(document, geneIds, elementName1 );
    }

}