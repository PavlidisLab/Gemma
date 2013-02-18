/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

import ubic.GEOMMTx.ParentFinder;
import ubic.GEOMMTx.evaluation.MakeHistogramData;
import ubic.basecode.dataStructure.StringToStringSetMap;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.AbstractCLIContextCLI;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to extract Gemma experiment annotations and write them to a file in R data format.
 * 
 * @author leon
 */
public class GetAnnotationMatrixCLI extends AbstractCLIContextCLI {

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        // options for:
        // * manual vrs automatic
        // * parents
        // * taxon
        // * ontology
        // * fileoutput name
        // * URI vrs label
        // * experiment tilte vrs shortname

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "GEOMMTx ", args );
        if ( err != null ) return err;

        StringToStringSetMap result = new StringToStringSetMap();
        StringToStringSetMap URIresult = new StringToStringSetMap();

        ExpressionExperimentService ees = this.getBean( ExpressionExperimentService.class );
        Collection<ExpressionExperiment> experiments = ees.loadAll();
        ParentFinder parentFinder = new ParentFinder();
        try {
            parentFinder.init();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        int c = 0;
        for ( ExpressionExperiment experiment : experiments ) {
            c++;
            log.info( "Experiment number:" + c + " of " + experiments.size() + " ID:" + experiment.getId()
                    + " Shortname:" + experiment.getShortName() );

            experiment = ees.thawLite( experiment );

            Collection<Characteristic> characters = experiment.getCharacteristics();

            Set<String> currentLabelSet = new HashSet<String>();
            Set<String> currentURISet = new HashSet<String>();

            result.put( experiment.getShortName() + "", currentLabelSet );

            URIresult.put( experiment.getShortName() + "", currentURISet );

            for ( Characteristic ch : characters ) {
                if ( ch instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) ch;

                    // is it manual annotation?
                    // if ( vc.getEvidenceCode().equals( GOEvidenceCode.IC ) ) {
                    // log.info( vc.getValue() + " " + vc.getValueUri() );
                    if ( vc.getValueUri() != null && vc.getValueUri().startsWith( "http" ) ) {
                        currentLabelSet.add( vc.getValue() );
                        currentURISet.add( vc.getValueUri() );

                        // get parents
                        // COMMENTED OUT
                        // Set<String> parentURLs = parentFinder.allParents( vc.getValueUri() );
                        // for ( String parentURI : parentURLs ) {
                        // currentURISet.add( parentURI );
                        // if ( parentFinder.getTerm( parentURI ) != null ) {
                        // String parentLabel = parentFinder.getTerm( parentURI ).getLabel();
                        // // log.info( vc.getValue() + "->" + parentLabel );
                        // if ( parentLabel != null ) currentLabelSet.add( parentLabel );
                        // }
                        // }
                    } else {
                        log.info( "NON-URI:" + vc.getValue() + " " + vc.getValueUri() );
                    }
                }

                // }
            }
        }

        DoubleMatrix<String, String> resultLabelMatrix = StringToStringSetMap.setMapToMatrix( result );
        DoubleMatrix<String, String> resultURIMatrix = StringToStringSetMap.setMapToMatrix( URIresult );

        try {
            MakeHistogramData.writeRTable( "/grp/java/workspace/GEOMMTxRefactor/ForRaymond.txt", resultLabelMatrix );
            MakeHistogramData.writeRTable( "/grp/java/workspace/GEOMMTxRefactor/ForRaymondURI.txt", resultURIMatrix );
        } catch ( Exception e ) {
            e.printStackTrace();
            System.exit( 1 );
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GetAnnotationMatrixCLI p = new GetAnnotationMatrixCLI();

        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
