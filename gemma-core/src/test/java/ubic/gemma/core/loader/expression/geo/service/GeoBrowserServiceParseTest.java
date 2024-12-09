/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.expression.geo.service;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExceptionIsDueToNetworkIssue;

/**
 * @author paul
 */
@ContextConfiguration
public class GeoBrowserServiceParseTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class GeoBrowserServiceParseTestContextConfiguration {

        @Bean
        public GeoBrowserServiceImpl geoBrowserService() {
            return new GeoBrowserServiceImpl();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }
    }

    @Autowired
    private GeoBrowserServiceImpl serv;

    @Autowired
    private ArrayDesignService ads;

    @Mock
    @Autowired
    private ExpressionExperimentService ees;

    @Test
    @Category(SlowTest.class)
    public void testParse() {
        String response;
        try ( InputStream r = new ClassPathResource( "/data/loader/expression/geo/geo.esummary.test.xml" ).getInputStream() ) {
            response = IOUtils.toString( r, StandardCharsets.ISO_8859_1 );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
            return;
        }
        try {
            serv.formatDetails( response, "" );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
        }
        verify( ads ).findByShortName( "GPL1708" );
        verify( ees ).findByShortName( "GSE4595" );
    }

    @Test
    public void testParse2() {
        String response;
        try ( InputStream r = new ClassPathResource( "/data/loader/expression/geo/geo.esummary.test1.xml" ).getInputStream() ) {
            response = IOUtils.toString( r, StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        System.out.println( response );
        try {
            serv.formatDetails( response, "" );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
        }
        verify( ads ).findByShortName( "GPL570" );

    }

    @Test
    public void testParse3() {
        String response;
        try ( InputStream r = new ClassPathResource( "/data/loader/expression/geo/geo.esummary.test2.xml" ).getInputStream() ) {
            response = IOUtils.toString( r, StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        System.out.println( response );
        try {
            serv.formatDetails( response, "" );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
        }
        verify( ads ).findByShortName( "GPL3829" );
        verify( ees ).findByShortName( "GSE21230" );
    }

    @Test
    public void testMINiMLParse() throws Exception {
        ClassPathResource resource = new ClassPathResource( "/data/loader/expression/geo/GSE180363.miniml.xml" );
        GeoBrowserImpl serv = new GeoBrowserImpl( Settings.getString( "entrez.efetch.apikey" ) );
        GeoRecord rec = new GeoRecord();
        serv.fillSubSeriesStatus( rec, serv.parseMiniMLDocument( resource.getURL() ) );
        assertTrue( rec.isSubSeries() );
    }

    @Test
    @Category(SlowTest.class)
    public void testSampleMINiMLParse() throws Exception {
        ClassPathResource resource = new ClassPathResource( "/data/loader/expression/geo/GSE171682.xml" );
        GeoBrowserImpl serv = new GeoBrowserImpl( Settings.getString( "entrez.efetch.apikey" ) );
        GeoRecord rec = new GeoRecord();
        serv.fillLibraryStrategy( rec, serv.parseMiniMLDocument( resource.getURL() ) );
        serv.fillSampleDetails( rec, serv.parseMiniMLDocument( resource.getURL() ), GeoRetrieveConfig.DETAILED );
        assertTrue( rec.getSampleDetails().contains( "colorectal cancer" ) );
        assertTrue( rec.getSampleDetails().contains( "Large intestine" ) );
        assertEquals( "RNA-Seq", rec.getLibraryStrategy() );
    }
}
