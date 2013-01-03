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
package ubic.gemma.analysis.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataFileServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionDataFileService expressionDataFileService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    private String shortName = "GSE1997";

    /**
     * 
     */
    @Test
    public void testGetOutputFile() throws Exception {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find experiment " + shortName + ".  Skipping test ..." );
            return;
        }
        ee = expressionExperimentService.thawLite( ee );

        String filename = ExpressionDataFileService.DATA_DIR + ee.getId() + "_" + shortName + "_expmat.data.txt.gz";
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( filename ) ) );
        writer.write( "File written from " + this.getClass().getName() + " on " + new Date() );
        writer.flush();
        writer.close();

        File f1 = expressionDataFileService.getOutputFile( ee, true );
        assertNotNull( f1 );

        Reader reader = new InputStreamReader( new GZIPInputStream( new FileInputStream( f1 ) ) );
        int i = reader.read();
        assertEquals( 70, i );
        reader.close();

        expressionDataFileService.deleteAllFiles( ee );
        assertTrue( !f1.exists() );
    }

    /**
     * 
     */
    @Test
    public void testGetUnfilteredOutputFile() throws Exception {

        ExpressionExperiment ee = expressionExperimentService.findByShortName( shortName );
        if ( ee == null ) {
            log.error( "Could not find experiment " + shortName + ".  Skipping test ..." );
            return;
        }
        ee = expressionExperimentService.thawLite( ee );

        String filename = ExpressionDataFileService.DATA_DIR + ee.getId() + "_" + shortName
                + "_expmat.unfilt.data.txt.gz";
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( filename ) ) );
        writer.write( "File written from " + this.getClass().getName() + " on " + new Date() );
        writer.flush();
        writer.close();

        File f1 = expressionDataFileService.getOutputFile( ee, false );
        assertNotNull( f1 );

        Reader reader = new InputStreamReader( new GZIPInputStream( new FileInputStream( f1 ) ) );
        int i = reader.read();
        assertEquals( 70, i );
        reader.close();

        expressionDataFileService.deleteAllFiles( ee );
        assertTrue( !f1.exists() );
    }
}
