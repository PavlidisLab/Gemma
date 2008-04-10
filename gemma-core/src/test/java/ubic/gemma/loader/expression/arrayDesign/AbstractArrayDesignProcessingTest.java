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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.FileNotFoundException;
import java.util.Collection;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Base test for tests that need persistent array design with sequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractArrayDesignProcessingTest extends BaseSpringContextTest {

    static ArrayDesign ad;
    ArrayDesignService arrayDesignService;
    final static String ACCESSION = "GPL140";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUp()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();
        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        ad = arrayDesignService.findByShortName( ACCESSION );

        if ( ad == null ) {

            // first load small twoc-color
            AbstractGeoService geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            try {
                final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( ACCESSION,
                        true, true, false );

                ad = ads.iterator().next();
                arrayDesignService.thawLite( ad );
            } catch ( Exception e ) {
                if ( e.getCause() instanceof FileNotFoundException ) {
                    log.warn( "problem with initializing array design for test: " + e.getCause().getMessage() );
                    return;
                }
                throw e;
            }
        }

    }

    public ArrayDesign getAd() {
        return ad;
    }

}
