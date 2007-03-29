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
package ubic.gemma.analysis.preprocess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Tackles the problem of concatenating DesignElementDataVectors for a single experiment. This is necessary When a study
 * uses two or more similar array designs without 'replication'. Typical of the genre is GSE60 ("Diffuse large B-cell
 * lymphoma"), with 31 BioAssays on GPL174, 35 BioAssays on GPL175, and 66 biomaterials.
 * <p>
 * The algorithm for dealing with this is a preprocessing step:
 * <ol>
 * <li>Generate a merged ExpressionDataMatrix for each of the (important) quantitation types.</li>
 * <li>Create a merged BioAssayDimension</li>
 * <li>Persist the new vectors, which are now tied to a <em>single DesignElement</em>. This is, strictly speaking,
 * incorrect, but because the design elements used in the vector all point to the same sequence, there is no major
 * problem in analyzing this. However, there is a potential loss of information.
 * </ol>
 * <p>
 * 
 * @spring.bean id="vectorMergingService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @author pavlidis
 * @version $Id$
 * @see ExpressionDataMatrixBuilder
 */
public class VectorMergingService {

    private static Log log = LogFactory.getLog( VectorMergingService.class.getName() );

    private ExpressionExperimentService expressionExperimentService;

    private DesignElementDataVectorService designElementDataVectorService;

    private ArrayDesignService arrayDesignService;

    private BioAssayDimensionService bioAssayDimensionService;

    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    // ByteArrayConverter is stateless.
    ByteArrayConverter converter = new ByteArrayConverter();

    /**
     * @param expExp
     */
    @SuppressWarnings("unchecked")
    public void mergeVectors( ExpressionExperiment expExp, Long dimId ) {

        Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        if ( arrayDesigns.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot cope with more than one platform" );
        }

        ArrayDesign arrayDesign = arrayDesigns.iterator().next();
        arrayDesignService.thawLite( arrayDesign );

        log.info( qts.size() + " quantitation types" );
        for ( QuantitationType type : qts ) {

            log.info( "Processing " + type );

            Collection<DesignElementDataVector> oldVectors = getVectorsForOneQuantitationType( expExp, type );

            LinkedHashSet<BioAssayDimension> oldBioAssayDims = new LinkedHashSet<BioAssayDimension>();

            Map<DesignElement, Collection<DesignElementDataVector>> deVMap = new HashMap<DesignElement, Collection<DesignElementDataVector>>();
            for ( DesignElementDataVector vector : oldVectors ) {
                oldBioAssayDims.add( vector.getBioAssayDimension() );
                if ( !deVMap.containsKey( vector.getDesignElement() ) ) {
                    deVMap.put( vector.getDesignElement(), new HashSet<DesignElementDataVector>() );
                }
                deVMap.get( vector.getDesignElement() ).add( vector );
            }

            if ( oldBioAssayDims.size() == 1 ) {
                log.info( "No merging needed for " + type + " (only one bioassaydimension already)" );
                continue;
            }

            // define a new bioAd.
            BioAssayDimension newBioAd;
            if ( dimId != null ) {
                newBioAd = bioAssayDimensionService.load( dimId );
                if ( newBioAd == null ) {
                    throw new IllegalArgumentException( "No bioAssayDimension with id " + dimId );
                }
                log.info( "Using existing bioassaydimension" );
            } else {
                newBioAd = combineBioAssayDimensions( oldBioAssayDims );
            }

            int totalBioAssays = newBioAd.getBioAssays().size();

            Collection<DesignElementDataVector> newVectors = new HashSet<DesignElementDataVector>();
            for ( DesignElement de : deVMap.keySet() ) {

                DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
                vector.setBioAssayDimension( newBioAd );
                vector.setDesignElement( de );
                vector.setQuantitationType( type );
                vector.setExpressionExperiment( expExp );

                // merge the data in the right right order please!
                Collection<DesignElementDataVector> dedvs = deVMap.get( de );

                List<Object> data = new ArrayList<Object>();
                // these ugly nested loops are to ENSURE that we get the vector reconstructed properly. For each of the
                // old bioassayDimensions, find the designelementdatavector that uses it. If there isn't one, fill in
                // the values for that dimension with missing data.
                for ( BioAssayDimension oldDim : oldBioAssayDims ) {
                    if ( oldDim.equals( newBioAd ) ) continue;
                    boolean found = false;
                    PrimitiveType representation = type.getRepresentation();
                    for ( DesignElementDataVector oldV : dedvs ) {
                        if ( oldV.getBioAssayDimension().equals( oldDim ) ) {
                            found = true;
                            byte[] rawDat = oldV.getData();

                            if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
                                boolean[] convertedDat = converter.byteArrayToBooleans( rawDat );
                                for ( boolean b : convertedDat ) {
                                    data.add( new Boolean( b ) );
                                }
                            } else if ( representation.equals( PrimitiveType.CHAR ) ) {
                                char[] convertedDat = converter.byteArrayToChars( rawDat );
                                for ( char b : convertedDat ) {
                                    data.add( new Character( b ) );
                                }
                            } else if ( representation.equals( PrimitiveType.DOUBLE ) ) {
                                double[] convertedDat = converter.byteArrayToDoubles( rawDat );
                                for ( double b : convertedDat ) {
                                    data.add( new Double( b ) );
                                }
                            } else if ( representation.equals( PrimitiveType.INT ) ) {
                                int[] convertedDat = converter.byteArrayToInts( rawDat );
                                for ( int b : convertedDat ) {
                                    data.add( new Integer( b ) );
                                }
                            } else if ( representation.equals( PrimitiveType.LONG ) ) {
                                long[] convertedDat = converter.byteArrayToLongs( rawDat );
                                for ( long b : convertedDat ) {
                                    data.add( new Long( b ) );
                                }
                            } else if ( representation.equals( PrimitiveType.STRING ) ) {
                                String[] convertedDat = converter.byteArrayToStrings( rawDat );
                                for ( String b : convertedDat ) {
                                    data.add( b );
                                }
                            } else {
                                throw new UnsupportedOperationException( "Don't know how to handle " + representation );
                            }

                            break;
                        }
                    }
                    if ( !found ) {
                        int nullsNeeded = oldDim.getBioAssays().size();
                        for ( int i = 0; i < nullsNeeded; i++ ) {
                            // FIXME this code taken from GeoConverter
                            if ( representation.equals( PrimitiveType.DOUBLE ) ) {
                                data.add( Double.NaN );
                            } else if ( representation.equals( PrimitiveType.STRING ) ) {
                                data.add( "" );
                            } else if ( representation.equals( PrimitiveType.INT ) ) {
                                data.add( 0 );
                            } else if ( representation.equals( PrimitiveType.BOOLEAN ) ) {
                                data.add( false );
                            } else {
                                throw new UnsupportedOperationException( "Missing values in data vectors of type "
                                        + representation + " not supported (when processing " + de );
                            }

                        }
                    }
                }

                if ( data.size() != totalBioAssays ) {
                    throw new IllegalStateException( "Wrong number of values for " + de + " / " + type + ", expected "
                            + totalBioAssays + ", got " + data.size() );
                }
                byte[] newDataAr = converter.toBytes( data.toArray() );

                vector.setData( newDataAr );

                newVectors.add( vector );
            }

            // print( newVectors );

            log.info( "Creating " + newVectors.size() + " new vectors for " + type );
            designElementDataVectorService.create( newVectors );

            log.info( "Removing " + oldVectors.size() + " old vectors for " + type );
            designElementDataVectorService.remove( oldVectors );

            // FIXME can remove the old BioAssayDimensions, too.

        }

    }

    /**
     * Just for debugging.
     * 
     * @param newVectors
     */
    @SuppressWarnings("unused")
    private void print( Collection<DesignElementDataVector> newVectors ) {
        StringBuilder buf = new StringBuilder();
        ByteArrayConverter conv = new ByteArrayConverter();
        for ( DesignElementDataVector vector : newVectors ) {
            buf.append( vector.getDesignElement() );
            QuantitationType qtype = vector.getQuantitationType();
            if ( qtype.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                double[] vals = conv.byteArrayToDoubles( vector.getData() );
                for ( double d : vals ) {
                    buf.append( "\t" + d );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.INT ) ) {
                int[] vals = conv.byteArrayToInts( vector.getData() );
                for ( int i : vals ) {
                    buf.append( "\t" + i );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
                boolean[] vals = conv.byteArrayToBooleans( vector.getData() );
                for ( boolean d : vals ) {
                    buf.append( "\t" + d );
                }
            } else if ( qtype.getRepresentation().equals( PrimitiveType.STRING ) ) {
                String[] vals = conv.byteArrayToStrings( vector.getData() );
                for ( String d : vals ) {
                    buf.append( "\t" + d );
                }

            }
            buf.append( "\n" );
        }

        log.info( "\n" + buf );
    }

    /**
     * @param bioAds
     * @return
     */
    private BioAssayDimension combineBioAssayDimensions( LinkedHashSet<BioAssayDimension> bioAds ) {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "" );
        bad.setDescription( "Generated by the merger of " + bioAds.size() + " dimensions: " );

        List<BioAssay> bioAssays = new ArrayList<BioAssay>();

        for ( BioAssayDimension bioAd : bioAds ) {
            bioAssays.addAll( bioAd.getBioAssays() );
            bad.setName( bad.getName() + bioAd.getName() + " " );
            bad.setDescription( bad.getDescription() + bioAd.getName() + " " );
        }

        bad.setBioAssays( bioAssays );

        return bioAssayDimensionService.create( bad );
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * FIXME Code copied from ExpressionExperimentPlatformSwitchService
     * 
     * @param expExp
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> getVectorsForOneQuantitationType( ExpressionExperiment expExp,
            QuantitationType type ) {
        Collection<QuantitationType> oneType = new HashSet<QuantitationType>();
        oneType.add( type );
        Collection<DesignElementDataVector> vectorsForQt = expressionExperimentService.getDesignElementDataVectors(
                expExp, oneType );
        designElementDataVectorService.thaw( vectorsForQt );
        return vectorsForQt;
    }

    public void mergeVectors( ExpressionExperiment expressionExperiment ) {
        this.mergeVectors( expressionExperiment, null );

    }
}
