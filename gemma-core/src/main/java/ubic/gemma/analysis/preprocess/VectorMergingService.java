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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.service.ExpressionExperimentVectorManipulatingService;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
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
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @author pavlidis
 * @version $Id$
 * @see ExpressionDataMatrixBuilder
 */
public class VectorMergingService extends ExpressionExperimentVectorManipulatingService {

    private static Log log = LogFactory.getLog( VectorMergingService.class.getName() );

    private ExpressionExperimentService expressionExperimentService;

    private ArrayDesignService arrayDesignService;

    private BioAssayDimensionService bioAssayDimensionService;

    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

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

        Collection<BioAssayDimension> newDims = new HashSet<BioAssayDimension>();
        for ( QuantitationType type : qts ) {

            log.info( "Processing " + type );

            Collection<DesignElementDataVector> oldVectors = getVectorsForOneQuantitationType( type );

            LinkedHashSet<BioAssayDimension> oldBioAssayDims = new LinkedHashSet<BioAssayDimension>();

            Map<DesignElement, Collection<DesignElementDataVector>> deVMap = new HashMap<DesignElement, Collection<DesignElementDataVector>>();
            for ( DesignElementDataVector vector : oldVectors ) {
                oldBioAssayDims.add( vector.getBioAssayDimension() );
                if ( !deVMap.containsKey( vector.getDesignElement() ) ) {
                    deVMap.put( vector.getDesignElement(), new HashSet<DesignElementDataVector>() );
                }
                deVMap.get( vector.getDesignElement() ).add( vector );
            }
            List<BioAssayDimension> sortedOldDims = sortedBioAssayDimensions( oldBioAssayDims );

            if ( oldBioAssayDims.size() == 1 ) {
                log.info( "No merging needed for " + type + " (only one bioassaydimension already)" );
                continue;
            }

            BioAssayDimension newBioAd;
            if ( dimId != null ) {
                newBioAd = bioAssayDimensionService.load( dimId );
                if ( newBioAd == null ) {
                    throw new IllegalArgumentException( "No bioAssayDimension with id " + dimId );
                }
                log.info( "Using existing bioassaydimension" );
            } else {
                newBioAd = locateBioAssayDimension( sortedOldDims, newDims );
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

                // we go through the dimensions in the same order that we joined them up.
                for ( BioAssayDimension oldDim : sortedOldDims ) {
                    if ( oldDim.equals( newBioAd ) ) continue;
                    boolean found = false;
                    PrimitiveType representation = type.getRepresentation();
                    for ( DesignElementDataVector oldV : dedvs ) {
                        if ( oldV.getBioAssayDimension().equals( oldDim ) ) {
                            found = true;
                            convertFromBytes( data, representation, oldV );

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
            for ( BioAssayDimension oldDim : oldBioAssayDims ) {

            }
        }

    }

    private BioAssayDimension locateBioAssayDimension( Collection<BioAssayDimension> oldBioAssayDims,
            Collection<BioAssayDimension> newDims ) {
        List<BioAssayDimension> sortedOldDims = sortedBioAssayDimensions( oldBioAssayDims );

        // define a new bioAd, or get an existing one.

        return combineBioAssayDimensions( newDims, sortedOldDims );

    }

    private List<BioAssayDimension> sortedBioAssayDimensions( Collection<BioAssayDimension> oldBioAssayDims ) {
        List<BioAssayDimension> sortedOldDims = new ArrayList<BioAssayDimension>();
        sortedOldDims.addAll( oldBioAssayDims );
        Collections.sort( sortedOldDims, new Comparator<BioAssayDimension>() {
            public int compare( BioAssayDimension o1, BioAssayDimension o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );
        return sortedOldDims;
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
     * Create a new one or use an existing one.
     * 
     * @param bioAds
     * @return
     */
    private BioAssayDimension combineBioAssayDimensions( Collection<BioAssayDimension> newDims,
            List<BioAssayDimension> bioAds ) {

        List<BioAssay> bioAssays = new ArrayList<BioAssay>();
        for ( BioAssayDimension bioAd : bioAds ) {
            bioAssays.addAll( bioAd.getBioAssays() );
        }

        // first see if we already have an equivalent one.
        boolean found = true;
        for ( BioAssayDimension newDim : newDims ) {
            // size should be the same.
            List<BioAssay> assaysInExisting = ( List<BioAssay> ) newDim.getBioAssays();
            if ( assaysInExisting.size() != bioAssays.size() ) {
                continue;
            }

            for ( int i = 0; i < bioAssays.size(); i++ ) {
                if ( !assaysInExisting.get( i ).equals( bioAssays.get( i ) ) ) {
                    found = false;
                    break;
                }
            }
            if ( !found ) continue;
            log.info( "Already have a dimension created that fits the bill" );
            return newDim;
        }

        BioAssayDimension newBioAd = BioAssayDimension.Factory.newInstance();
        newBioAd.setName( "" );
        newBioAd.setDescription( "Generated by the merger of " + bioAds.size() + " dimensions: " );

        for ( BioAssayDimension bioAd : bioAds ) {
            newBioAd.setName( newBioAd.getName() + bioAd.getName() + " " );
            newBioAd.setDescription( newBioAd.getDescription() + bioAd.getName() + " " );
        }

        newBioAd.setName( StringUtils.abbreviate( newBioAd.getName(), 255 ) );
        newBioAd.setBioAssays( bioAssays );

        newBioAd = bioAssayDimensionService.create( newBioAd );
        newDims.add( newBioAd );
        log.info( "Created new bioAssayDimension" );
        return newBioAd;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void mergeVectors( ExpressionExperiment expressionExperiment ) {
        this.mergeVectors( expressionExperiment, null );

    }
}
