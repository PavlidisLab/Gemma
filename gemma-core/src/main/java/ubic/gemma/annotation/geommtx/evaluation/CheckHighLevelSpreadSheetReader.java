/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.annotation.geommtx.evaluation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFSheet;

import ubic.GEOMMTx.evaluation.DescriptionExtractor;
import ubic.GEOMMTx.filters.UninformativeFilter;
import ubic.GEOMMTx.util.HashMapStringSet;
import ubic.GEOMMTx.util.SetupParameters;
import ubic.basecode.io.excel.ExcelUtil;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class CheckHighLevelSpreadSheetReader {
    /**
     * @param args
     */
    public static void main( String[] args ) {
        CheckHighLevelSpreadSheetReader test = new CheckHighLevelSpreadSheetReader();

        Map<String, Set<String>> acceptedAnnotations = test.getAcceptedAnnotations();
        System.out.println( acceptedAnnotations.size() );
        Map<String, Set<String>> rejectedAnnotations = test.getRejectedAnnotations();
        System.out.println( rejectedAnnotations.size() );

        System.out.println( rejectedAnnotations.get( ( ( long ) 295 ) + "" ) );
    }

    /**
     * @param file
     * @param decision
     * @return
     */
    public Map<String, Set<String>> getAnnotations( String file, String decision ) {
        // CheckHighLevelSchema schema = new CheckHighLevelSchema();
        UninformativeFilter f = new UninformativeFilter();

        HSSFSheet sheet;
        try {
            sheet = ExcelUtil.getSheetFromFile( file, "Sheet1" );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        // start at one, skip the header
        int row = 2;
        int nullCount = 0;
        // int datasetPos = schema.getPosition( "Dataset" );
        // int URIPos = schema.getPosition( "URL" );
        // int rejectPos = schema.getPosition( "Reject" );
        int datasetPos = 0;
        int URIPos = 1;
        // FINAL is 7
        // both reject is 13
        // both agree is 14
        // agreement is 8
        int acceptPos = 7;
        HashMapStringSet matchesDecision = new HashMapStringSet();
        HashMapStringSet all = new HashMapStringSet();

        // if we get a blank lines in a row, then exit
        while ( nullCount == 0 ) {
            String dataset = ExcelUtil.getValue( sheet, row, datasetPos );
            String URI = ExcelUtil.getValue( sheet, row, URIPos );
            String finalDecision = ExcelUtil.getValue( sheet, row, acceptPos );

            // System.out.println( CUI );
            // System.out.println( BCUI );
            if ( dataset == null ) {
                nullCount++;
            } else {
                // =HYPERLINK("http://bioinformatics.ubc.ca/Gemma/expressionExperiment/showExpressionExperiment.html?id=137";"137")
                // turns into 137
                dataset = dataset.substring( dataset.lastIndexOf( "?id=" ) + 4, dataset.lastIndexOf( "\"," ) );
                nullCount = 0;
                all.put( dataset, URI );
                // if the final decision matches input parameter
                if ( finalDecision != null && finalDecision.equals( decision ) ) {
                    // if its not deemed uninformative/too frequent
                    if ( !f.getFrequentURLs().contains( URI ) ) {
                        matchesDecision.put( dataset, URI );
                    }
                }
            }
            row++;
        }
        // System.out.println( seen );
        // System.out.println( seen.size() );
        // log.info( "All annotations in file:" + all.size() );
        // System.out.println( all.toPrettyString() );
        // System.out.println( all.getExpandedSize() );
        // System.out.println( "Number of accepted annotations:"+accepted.getExpandedSize() );
        return matchesDecision;
    }

    public Map<String, Set<String>> getAcceptedAnnotations() {
        return getAcceptedAnnotations( SetupParameters.getString( "geommtx.annotator.highLevelSpreadsheetFile" ) );
    }

    public Map<String, Set<String>> getAcceptedAnnotations( String file ) {
        // 1.0 for accept
        return getAnnotations( file, "1.0" );
    }

    public Map<String, Set<String>> getRejectedAnnotations() {
        return getAnnotations( SetupParameters.getString( "geommtx.annotator.highLevelSpreadsheetFile" ), "0.0" );
    }

    /**
     * @param annotations
     * @param filename
     * @throws IOException
     */
    public void printSourceStats( Map<String, Set<String>> annotations, String filename ) throws IOException {
        DescriptionExtractor de = new DescriptionExtractor( filename );

        List<String> sources = new LinkedList<String>();
        for ( String dataset : annotations.keySet() ) {
            Set<String> URIs = annotations.get( dataset );
            sources.addAll( de.getDecriptionType( dataset, URIs ) );
        }

        System.out.println( "== rejects ==" );
        CompareToManualCLI.printMap( CompareToManualCLI.listToFrequencyMap( sources ) );
    }

}
