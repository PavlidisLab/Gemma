/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.search.io.expression.experiment;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 */
public class ExpressionExperimentIndexer {
    private static Log log = LogFactory.getLog( ExpressionExperimentIndexer.class );

    /**
     * @param indexDir
     * @param dataDir
     * @return
     * @throws IOException
     */
    public static int index( File indexDir, Collection<ExpressionExperiment> expressionExperiments ) throws IOException {

        if ( expressionExperiments == null || expressionExperiments.size() == 0 ) {
            throw new IOException( "expression experiments do not exist" );
        }

        IndexWriter writer = new IndexWriter( indexDir, new StandardAnalyzer(), true );
        // IndexWriter writer = new IndexWriter( new RAMDirectory( indexDir ), new StandardAnalyzer(), true );
        writer.setUseCompoundFile( false );

        indexExpressionExperiments( writer, expressionExperiments );

        int numIndexed = writer.docCount();
        writer.optimize();
        writer.close();
        return numIndexed;
    }

    /**
     * @param writer
     * @param dir
     * @throws IOException
     */
    private static void indexExpressionExperiments( IndexWriter writer,
            Collection<ExpressionExperiment> expressionExperiments ) throws IOException {

        for ( ExpressionExperiment ee : expressionExperiments ) {
            indexExpressionExperiment( writer, ee );
        }
    }

    /**
     * @param writer
     * @param f
     * @throws IOException
     */
    private static void indexExpressionExperiment( IndexWriter writer, ExpressionExperiment expressionExperiment )
            throws IOException {

        if ( expressionExperiment == null ) {
            return;
        }

        Document doc = new Document();
        // doc.add( Field.Text( "contents", new FileReader( f ) ) );

        // java.lang.reflect.Field[] fields = expressionExperiment.getClass().getDeclaredFields();
        // for (java.lang.reflect.Field field: fields){
        // doc.add( Field.Keyword( field.getName(), field.get(expressionExperiment) ) );
        // }
        doc.add( Field.Keyword( "name", expressionExperiment.getName() ) );
        doc.add( Field.Keyword( "description", expressionExperiment.getDescription() ) );
        writer.addDocument( doc );
    }
}
