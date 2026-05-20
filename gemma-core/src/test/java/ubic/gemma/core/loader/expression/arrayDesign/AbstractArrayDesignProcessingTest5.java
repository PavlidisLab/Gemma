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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.FileNotFoundException;
import java.util.Collection;

/**
 * JUnit 5 parallel of {@link AbstractArrayDesignProcessingTest}, extending the
 * Jupiter {@link BaseSpringContextTest5} base. Used while the array-design
 * processing test family migrates piecewise to Jupiter.
 *
 * @author pavlidis
 */
public abstract class AbstractArrayDesignProcessingTest5 extends BaseSpringContextTest5 {

    private final static String ACCESSION = "GPL140";
    ArrayDesign ad;

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeoService geoService;

    public ArrayDesign getAd() {
        return ad;
    }

    @BeforeEach
    public void setupArrayDesign() {
        ad = arrayDesignService.findByShortName( AbstractArrayDesignProcessingTest5.ACCESSION );

        if ( ad == null ) {

            // first load small twoc-color
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            try {
                @SuppressWarnings("unchecked") final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService
                        .fetchAndLoad( AbstractArrayDesignProcessingTest5.ACCESSION, true, true, false, true, true );

                ad = ads.iterator().next();

            } catch ( Exception e ) {
                if ( e.getCause() instanceof FileNotFoundException ) {
                    log.warn( "problem with initializing array design for test: " + e.getCause().getMessage() );
                    return;
                }
                throw e;
            }
        }

        ad = arrayDesignService.thaw( ad );

    }

    @AfterEach
    public void tearDown() {
        Collection<Gene> genes = geneService.loadAll();
        for ( Gene gene : genes ) {
            try {
                geneService.remove( gene );
            } catch ( Exception ignored ) {
            }
        }

        if ( ad != null )
            try {
                arrayDesignService.remove( ad );
            } catch ( Exception ignored ) {
            }
    }

}
