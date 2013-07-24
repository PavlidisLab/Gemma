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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;

/**
 * array design short name -> return matching array design identifier
 * 
 * @author klc, gavin
 * @version$Id$
 */
public class ArrayDesignIdentifierByNameEndpoint extends AbstractGemmaEndpoint {

    private ArrayDesignService arrayDesignService;
    private static Log log = LogFactory.getLog( ArrayDesignIdentifierByNameEndpoint.class );

    /**
     * The local name of the expected request.
     */
    public static final String ARRAY_LOCAL_NAME = "arrayDesignIdentifierByName";

    /**
     * Sets the "business service" to delegate to.
     */
    public void setArrayDesignService( ArrayDesignService ads ) {
        this.arrayDesignService = ads;
    }

    /**
     * Reads the given <code>requestElement</code>, and sends a the response back.
     * 
     * @param requestElement the contents of the SOAP message as DOM elements
     * @param document a DOM document to be used for constructing <code>Node</code>s
     * @return the response element
     */
    @Override
    protected Element invokeInternal( Element requestElement, Document document ) {
        StopWatch watch = new StopWatch();
        watch.start();

        setLocalName( ARRAY_LOCAL_NAME );
        String adName = "";
        // get GO id from request
        Collection<String> adResult = getSingleNodeValue( requestElement, "ad_name" );
        for ( String ad : adResult ) {
            adName = ad;
        }
        log.info( "XML input read: platform name, " + adName );

        // using the ArrayDesignService
        ArrayDesign ad = arrayDesignService.findByShortName( adName );
        if ( ad == null ) {
            String msg = "No platform with short name, " + adName + " can be found.";
            return buildBadResponse( document, msg );
        }
        // get Array Design ID and build results in the form of a collection
        Collection<String> adId = new HashSet<String>();
        adId.add( ad.getId().toString() );

        Element wrapper = buildWrapper( document, adId, "arrayDesign_ids" );

        watch.stop();
        Long time = watch.getTime();
        log.info( "XML response for platform id result built in " + time + "ms." );
        return wrapper;
    }

}
