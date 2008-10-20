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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Merge the processed results with the (transient) MAGE-ML converted data set, PRIOR to persisting.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedDataMerger {

    private static Log log = LogFactory.getLog( ProcessedDataMerger.class.getName() );
    private ByteArrayConverter byteConverter = new ByteArrayConverter();

    /**
     * @param mageMlResult Result from the MAGE-ML Converter. This should NOT be persisted yet.
     * @param quantitationTypes Obtained from the MAGE-ML Converter.
     * @param processedData Result from the ProcessedDataFileParser. This is a map of CS names to QT names to values.
     * @param sampleNames Sample names as provided by the ProcessedDataFileParser, which defines the order of the values
     *        in the value vectors.
     */
    public void merge( ExpressionExperiment mageMlResult, Collection<QuantitationType> quantitationTypes,
            Map<String, Map<String, List<String>>> processedData, Object[] sampleName ) {

        Map<String, QuantitationType> qtNameMap = new HashMap<String, QuantitationType>();
        for ( QuantitationType qt : quantitationTypes ) {
            qtNameMap.put( qt.getName(), qt );
        }

        Collection<BioAssay> bioAssays = mageMlResult.getBioAssays();
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + mageMlResult );

        // match up the BioAssays with the names in the sampleNames.
        Map<String, BioAssay> nameMap = new HashMap<String, BioAssay>();
        ArrayDesign ad = null;
        for ( BioAssay assay : bioAssays ) {
            nameMap.put( assay.getName(), assay );
            ArrayDesign baAd = assay.getArrayDesignUsed();
            if ( ad != null && !ad.equals( baAd ) ) {
                throw new IllegalStateException( "Sorry, can't handle multiple array design experiments" );
            }
            ad = baAd;
        }

        if ( ad == null ) {
            throw new IllegalStateException( "No array design for any assays!" );
        }

        if ( ad.getCompositeSequences().size() == 0 ) {
            throw new IllegalStateException(
                    "The array design has not been associated with the composite sequences yet" );
        }

        Map<String, CompositeSequence> csNameMap = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            // / todo check that it isn't a duplicate, we can't handle that.
            csNameMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < sampleName.length; i++ ) {
            if ( !nameMap.containsKey( sampleName[i] ) ) {
                throw new IllegalStateException( "Sample name in Processed data " + sampleName[i]
                        + " does not match the MAGE-ML" );
            }

            bad.getBioAssays().add( nameMap.get( sampleName[i] ) );
        }

        Collection<QuantitationType> usedQuantitationTypes = new HashSet<QuantitationType>();
        int count = 0;
        for ( String csName : processedData.keySet() ) {
            Map<String, List<String>> data = processedData.get( csName );
            CompositeSequence cs = csNameMap.get( csName );
            if ( cs == null ) {
                throw new IllegalStateException( "CompositeSequence name in Processed data " + csName
                        + " does not match the MAGE-ML" );
            }

            for ( String qtName : data.keySet() ) {
                QuantitationType type = qtNameMap.get( qtName );
                if ( type == null ) {
                    throw new IllegalStateException( "QuantitationType name in Processed data " + qtName
                            + " does not match anything in the MAGE-ML" );
                }

                usedQuantitationTypes.add( type );

                List<String> rawData = data.get( qtName );

                RawExpressionDataVector dv = RawExpressionDataVector.Factory.newInstance();
                dv.setExpressionExperiment( mageMlResult );
                dv.setQuantitationType( type );
                dv.setDesignElement( cs );
                dv.setBioAssayDimension( bad );

                byte[] dat = convertData( rawData, type );
                dv.setData( dat );

                mageMlResult.getRawExpressionDataVectors().add( dv );

            }

            if ( ++count % 5000 == 0 ) {
                log.info( "Processed data for " + count + " composite sequences" );
            }

        }

        /*
         * Remove quantitation types that aren't used for anything (they showed up in the MAGE-ML but weren't in the
         * processed data).
         */
        mageMlResult.getQuantitationTypes().retainAll( usedQuantitationTypes );

        log.info( "Processed data for " + count + " composite sequences" );
    }

    /**
     * @param rawData
     * @param type
     * @return
     */
    private byte[] convertData( List<String> rawData, QuantitationType type ) {
        Object[] dataObjects = new Object[rawData.size()];
        PrimitiveType representation = type.getRepresentation();

        for ( int i = 0; i < rawData.size(); i++ ) {
            String stringVal = rawData.get( i );

            if ( representation.equals( PrimitiveType.STRING ) ) {
                dataObjects[i] = stringVal;
            } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
                dataObjects[i] = new Boolean( stringVal );
            } else if ( representation.equals( PrimitiveType.DOUBLE ) ) {
                try {
                    dataObjects[i] = new Double( stringVal );
                } catch ( NumberFormatException e ) {
                    dataObjects[i] = Double.NaN;
                }
            } else if ( representation.equals( PrimitiveType.INT ) ) {
                try {
                    dataObjects[i] = new Integer( stringVal );
                } catch ( NumberFormatException e ) {
                    dataObjects[i] = 0;
                }
            } else {
                throw new IllegalStateException( "Don't know how to convert " + representation );
            }
        }

        return byteConverter.toBytes( dataObjects );
    }
}
