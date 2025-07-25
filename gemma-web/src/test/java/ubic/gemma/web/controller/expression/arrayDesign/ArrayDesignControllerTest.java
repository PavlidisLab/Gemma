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
package ubic.gemma.web.controller.expression.arrayDesign;

import org.apache.commons.io.file.PathUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.web.controller.util.DownloadUtil;
import ubic.gemma.web.util.BaseWebTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author keshav
 */
@ContextConfiguration
public class ArrayDesignControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    static class CC extends BaseWebTest.BaseWebTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "tomcat.sendfile.enabled=false", "gemma.support.email=pavlab-support@msl.ubc.ca" );
        }

        @Bean
        public ArrayDesignController arrayDesignController() {
            return new ArrayDesignController();
        }

        @Bean
        public ArrayDesignMapResultService arrayDesignMapResultService() {
            return mock();
        }

        @Bean
        public ArrayDesignReportService arrayDesignReportService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public TaskRunningService taskRunningService() {
            return mock();
        }

        @Bean
        public ArrayDesignAnnotationService annotationFileService() {
            return mock();
        }

        @Bean
        public DownloadUtil downloadUtil() {
            return new DownloadUtil();
        }
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignAnnotationService annotationFileService;

    private Path annotationDir;

    @Before
    public void setUp() throws IOException {
        annotationDir = Files.createTempDirectory( "annotationDirectory" );
        when( annotationFileService.getAnnotDataDir() ).thenReturn( annotationDir );
    }

    @After
    public void removeAnnotationDir() throws IOException {
        PathUtils.deleteDirectory( annotationDir );
    }

    @After
    public void resetMocks() {
        reset( arrayDesignService );
    }

    @Test
    public void testShowAllArrayDesigns() throws Exception {
        perform( get( "/arrays/showAllArrayDesigns.html" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "arrayDesigns" ) );
    }

    @Test
    public void testDownloadAnnotationsWhenFileCannotBeCreated() throws Exception {
        ArrayDesign ad = new ArrayDesign();
        when( arrayDesignService.load( 1L ) ).thenReturn( ad );
        perform( get( "/arrays/downloadAnnotationFile.html" )
                .param( "id", "1" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attributeExists( "exception" ) );
        verify( arrayDesignService ).load( 1L );
        // no file will be created because it's just a mock
        verify( annotationFileService ).create( ad, true, false );
    }
}
