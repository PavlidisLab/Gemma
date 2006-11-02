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

package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.diff.ExpressionDataManager;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author xiangwan
 */
public class ExpressionDataLoader {

    final String actualExperimentsPath = "C:/TestData/";
    final String analysisResultsPath = "C:/Results/";

    protected ExpressionExperiment experiment = null;

    protected String experimentName = null;

    protected static final Log log = LogFactory.getLog( ExpressionDataManager.class );

    protected GeneAnnotations geneAnnotations = null;

    protected int uniqueItems = 0;

    protected Collection<DesignElementDataVector> designElementDataVectors = null;

    public ExpressionDataLoader( ExpressionExperiment paraExperiment, String paraGOFile ) {
        this.experiment = paraExperiment;
        if ( this.experiment != null ) {
            this.getValidDesignmentDataVector();
            this.experimentName = this.experiment.getName();
        }
        Set rowsToUse = new HashSet( this.getActiveProbeIdSet() );
        try {
            this.geneAnnotations = new GeneAnnotations( this.actualExperimentsPath + paraGOFile, rowsToUse, null, null );
        } catch ( IOException e ) {
            log.error( "Error in reading GO File" );
        }

    }

    private Collection<String> getActiveProbeIdSet() {
        Collection probeIdSet = new HashSet<String>();
        for ( DesignElementDataVector dataVector : this.designElementDataVectors ) {
            DesignElement designElement = dataVector.getDesignElement();
            String probeId = ( ( CompositeSequence ) designElement ).getName();
            probeIdSet.add( probeId );
        }

        return probeIdSet;
    }

    private void getValidDesignmentDataVector() {
        Collection<DesignElementDataVector> dataVectors = this.experiment.getDesignElementDataVectors();
        this.designElementDataVectors = new HashSet<DesignElementDataVector>();
        for ( DesignElementDataVector dataVector : dataVectors ) {
            if ( dataVector.getQuantitationType().getName().trim().equals( "VALUE" )
                    && dataVector.getQuantitationType().getRepresentation().toString().trim().equals( "DOUBLE" ) )
                this.designElementDataVectors.add( dataVector );
        }
    }

    public void writeExpressionDataToFile( String paraFileName ) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( this.analysisResultsPath + paraFileName ) );
        } catch ( IOException e ) {
            log.error( "File for output expression data " + this.analysisResultsPath + paraFileName
                    + "could not be opened" );
        }
        Collection<DesignElementDataVector> dataVectors = this.experiment.getDesignElementDataVectors();

        try {
            writer.write( "Experiment Name: " + this.experimentName + "\n" );
            writer.write( "Accession: " + this.experiment.getAccession().getAccession() + "\n" );
            writer.write( "Name: " + this.experiment.getName() + "\n" );
            writer.write( "Description: " + this.experiment.getDescription() + "\n" );
            writer.write( "Source: " + this.experiment.getSource() + "\n" );

            for ( DesignElementDataVector dataVector : this.designElementDataVectors ) {
                DesignElement designElement = dataVector.getDesignElement();
                CompositeSequence compSequence = ( CompositeSequence ) designElement;
                String probId = ( ( CompositeSequence ) designElement ).getName();
                byte[] expressionByteData = dataVector.getData();
                ByteArrayConverter byteConverter = new ByteArrayConverter();
                double[] expressionData = byteConverter.byteArrayToDoubles( expressionByteData );
                writer.write( probId + "\t" );
                for ( int i = 0; i < expressionData.length; i++ )
                    writer.write( expressionData[i] + "\t" );
                writer.write( dataVector.getQuantitationType().getName() + "\t" );
                writer.write( dataVector.getQuantitationType().getRepresentation() + "\t" );
                writer.write( dataVector.getQuantitationType().getScale().getValue() + "\t" );
                writer.write( dataVector.getQuantitationType().getType().getValue() + "\t" );
                writer.write( "\n" );
            }
            writer.close();
        } catch ( IOException e ) {
            log.error( "Error in write data into file" );
        }
    }
}