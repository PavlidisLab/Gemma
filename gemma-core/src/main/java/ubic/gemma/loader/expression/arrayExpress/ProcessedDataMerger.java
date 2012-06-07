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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.QuantitationTypeParameterGuesser;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
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

    private String getUnqualifiedIdentifier( String identifier ) {
        return identifier.substring( identifier.lastIndexOf( ':' ) + 1, identifier.length() );
    }

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
            qtNameMap.put( getUnqualifiedIdentifier( qt.getName() ), qt );
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
                throw new IllegalStateException( "Sorry, can't handle multi-platform experiments" );
            }

            if ( assay.getSamplesUsed().size() == 0 ) {
                throw new IllegalStateException( "Bioassays must have at least one biomaterial" );
            }

            ad = baAd;
        }

        if ( ad == null ) {
            throw new IllegalStateException( "No platform for any assays!" );
        }

        if ( ad.getCompositeSequences().size() == 0 ) {
            throw new IllegalStateException(
                    "The platform has not been associated with the composite sequences yet" );
        }

        Map<String, CompositeSequence> csNameMap = new HashMap<String, CompositeSequence>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            // / todo check that it isn't a duplicate, we can't handle that.
            csNameMap.put( cs.getName(), cs );
        }

        for ( int i = 0; i < sampleName.length; i++ ) {
            if ( !nameMap.containsKey( sampleName[i] ) ) {
                log.error( "Sample name doesn't match MAGE-ML! Available names: " );
                for ( String name : nameMap.keySet() ) {
                    log.error( name );
                }
                throw new IllegalStateException( "Sample name in Processed data " + sampleName[i]
                        + " does not match the MAGE-ML names: " + StringUtils.join( nameMap.keySet(), "," ) );
            }

            bad.getBioAssays().add( nameMap.get( sampleName[i] ) );
        }

        assert bad.getBioAssays().size() == bioAssays.size();

        Collection<QuantitationType> usedQuantitationTypes = new HashSet<QuantitationType>();
        int count = 0;

        // boolean warnedAboutMissingQt = false;
        boolean warnedAboutLackOfData = false;

        for ( String csName : processedData.keySet() ) {

            if ( StringUtils.isBlank( csName ) ) {
                continue;
            }

            Map<String, List<String>> data = processedData.get( csName );
            CompositeSequence cs = csNameMap.get( csName );
            if ( cs == null ) {
                throw new IllegalStateException( "CompositeSequence name '" + csName
                        + "' in Processed data  does not match the array design, which has names like "
                        + csNameMap.keySet().iterator().next() );
            }

            /*
             * Quantitation types...
             */
            for ( String qtName : data.keySet() ) {
                QuantitationType type = locateOrGenerateQuantitationType( mageMlResult, qtNameMap, qtName );

                usedQuantitationTypes.add( type );
                List<String> rawData = data.get( qtName );

                if ( rawData == null ) {
                    if ( !warnedAboutLackOfData ) {
                        log.warn( "No raw data for quantitation type name '" + qtName + "' at cs=" + csName
                                + "; additional warnings suppressed" );
                        warnedAboutLackOfData = true;
                    }
                    continue;
                }

                if ( rawData.size() != bad.getBioAssays().size() ) {
                    throw new IllegalStateException( "Expected " + bad.getBioAssays().size()
                            + " values per data vector, got " + rawData.size() );
                }

                RawExpressionDataVector dv = RawExpressionDataVector.Factory.newInstance();
                dv.setExpressionExperiment( mageMlResult );
                dv.setQuantitationType( type );
                dv.setDesignElement( cs );
                dv.setBioAssayDimension( bad );

                byte[] dat = convertData( rawData, type );

                if ( dat == null || dat.length == 0 ) {
                    // blank line or some other such crap.
                    continue;
                }

                dv.setData( dat );

                mageMlResult.getRawExpressionDataVectors().add( dv );
            }

            if ( usedQuantitationTypes.size() == 0 ) {

                throw new IllegalStateException(
                        "No quantitation types were matched between the processed data and the MAGE-ML" );
            }

            if ( ++count % 10000 == 0 ) {
                log.info( "Processed data for " + count + " composite sequences" );
            }

        }

        if ( mageMlResult.getRawExpressionDataVectors().size() == 0 ) {
            throw new IllegalStateException( "No data vectors were found. Check the logs for warnings." );
        }

        /*
         * Remove quantitation types that aren't used for anything (they showed up in the MAGE-ML but weren't in the
         * processed data).
         */
        mageMlResult.getQuantitationTypes().retainAll( usedQuantitationTypes );

        log.info( "Processed data for " + count + " composite sequences, got "
                + mageMlResult.getRawExpressionDataVectors().size() + " raw data vectors." );

    }

    /**
     * Try to find the qt from the mage-ml otherwise just make one up.
     * 
     * @param mageMlResult
     * @param qtNameMap
     * @param qtName
     * @return
     */
    private QuantitationType locateOrGenerateQuantitationType( ExpressionExperiment mageMlResult,
            Map<String, QuantitationType> qtNameMap, String qtName ) {
        QuantitationType type = qtNameMap.get( qtName );
        if ( type == null ) {

            /*
             * Try alternative.
             */
            type = qtNameMap.get( getUnqualifiedIdentifier( qtName ) );

            if ( type == null ) {

                // try even harder.
                boolean foundUniqueMatch = true;
                for ( String qtmapkey : qtNameMap.keySet() ) {
                    if ( qtmapkey.contains( qtName ) ) {
                        if ( type == null ) {
                            log.debug( "Found possible approximate match to " + qtName + ": " + qtmapkey );
                            type = qtNameMap.get( qtmapkey );
                        } else {
                            foundUniqueMatch = false;
                        }
                    }
                }

                if ( type == null || foundUniqueMatch == false ) {

                    /*
                     * Damn, just make one up.
                     */
                    QuantitationType qt = QuantitationType.Factory.newInstance();
                    qt.setName( qtName );
                    QuantitationTypeParameterGuesser.guessQuantitationTypeParameters( qt, qtName, null );
                    mageMlResult.getQuantitationTypes().add( qt );
                    type = qt;
                    //
                    // if ( !warnedAboutMissingQt ) {
                    // log.warn( "QuantitationType name in Processed data '" + qtName
                    // + "' does not uniquely match anything in the MAGE-ML, so one was made up to suit" );
                    //
                    // warnedAboutMissingQt = true;
                    // }
                }
                // continue;
            }
        }
        return type;
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
