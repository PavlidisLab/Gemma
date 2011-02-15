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
package ubic.gemma.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.CancellationException;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.protocol.ProtocolApplication;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.biomaterial.CompoundService;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;

/**
 * @author pavlidis
 * @version $Id$
 */
abstract public class ExpressionPersister extends ArrayDesignPersister {

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private CompoundService compoundService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private FactorValueService factorValueService;

    Map<String, BioAssayDimension> bioAssayDimensionCache = new HashMap<String, BioAssayDimension>();

    public ExpressionPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        if ( entity == null ) return null;

        Object result;

        if ( entity instanceof ExpressionExperiment ) {
            clearCache();
            result = persistExpressionExperiment( ( ExpressionExperiment ) entity );
            return result;
        } else if ( entity instanceof BioAssayDimension ) {
            return persistBioAssayDimension( ( BioAssayDimension ) entity );
        } else if ( entity instanceof BioMaterial ) {
            return persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof BioAssay ) {
            return persistBioAssay( ( BioAssay ) entity );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return persistExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) entity );
        }
        return super.persist( entity );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        return super.persistOrUpdate( entity );
    }

    /**
     * @param bioAssayDimensionService The bioAssayDimensionService to set.
     */
    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * @param bioAssayService The bioAssayService to set.
     */
    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    /**
     * @param bioMaterialService The bioMaterialService to set.
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param compoundService The compoundService to set.
     */
    public void setCompoundService( CompoundService compoundService ) {
        this.compoundService = compoundService;
    }

    /**
     * @param experimentalDesignService the experimentalDesignService to set
     */
    public void setExperimentalDesignService( ExperimentalDesignService experimentalDesignService ) {
        this.experimentalDesignService = experimentalDesignService;
    }

    public void setExperimentalFactorService( ExperimentalFactorService experimentalFactorService ) {
        this.experimentalFactorService = experimentalFactorService;
    }

    /**
     * @param expressionExperimentService The expressionExperimentService to set.
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param factorValueService The factorValueService to set.
     */
    public void setFactorValueService( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

    /**
     * @param bioAssayDimensionCache
     * @param vect
     */
    private BioAssayDimension checkBioAssayDimensionCache( DesignElementDataVector vect ) {
        if ( !isTransient( vect.getBioAssayDimension() ) ) return vect.getBioAssayDimension();
        assert bioAssayDimensionCache != null;
        String dimensionName = vect.getBioAssayDimension().getName();
        if ( bioAssayDimensionCache.containsKey( dimensionName ) ) {
            vect.setBioAssayDimension( bioAssayDimensionCache.get( dimensionName ) );
        } else {
            BioAssayDimension bAd = persistBioAssayDimension( vect.getBioAssayDimension() );
            bioAssayDimensionCache.put( dimensionName, bAd );
            vect.setBioAssayDimension( bAd );
        }
        return bioAssayDimensionCache.get( dimensionName );
    }

    /**
     * If there are factorvalues, check if they are setup right and if they are used by biomaterials.
     * 
     * @param expExp
     */
    private void checkExperimentalDesign( ExpressionExperiment expExp ) {

        if ( expExp == null ) {
            return;
        }

        if ( expExp.getExperimentalDesign() == null ) {
            log.warn( "No experimental design!" );
            return;
        }

        Collection<ExperimentalFactor> efs = expExp.getExperimentalDesign().getExperimentalFactors();

        if ( efs.size() == 0 ) return;

        log.info( "Checking experimental design for valid setup" );

        Collection<BioAssay> bioAssays = expExp.getBioAssays();

        /*
         * note this is very inefficient but it doesn't matter.
         */
        for ( ExperimentalFactor ef : efs ) {
            log.info( "Checking: " + ef + ", " + ef.getFactorValues().size() + " factor values to check..." );

            for ( FactorValue fv : ef.getFactorValues() ) {

                if ( fv.getExperimentalFactor() == null || !fv.getExperimentalFactor().equals( ef ) ) {
                    throw new IllegalStateException( "Factor value " + fv + " should have had experimental factor "
                            + ef + ", it had " + fv.getExperimentalFactor() );
                }

                boolean found = false;
                // make sure there is at least one bioassay using it.
                for ( BioAssay ba : bioAssays ) {
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        for ( FactorValue fvb : bm.getFactorValues() ) {

                            // they should be persistent already at this point.
                            if ( ( fvb.getId() != null || fv.getId() != null ) && fvb.equals( fv ) && fvb == fv ) {
                                // Note we use == because they should be the same objects.
                                found = true;
                            }
                        }
                    }
                }
                if ( !found ) {
                    /*
                     * Basically this means there is factorvalue but no biomaterial is associated with it. This can
                     * happen...especially with test objects, so we just warn.
                     */
                    // throw new IllegalStateException( "Unused factorValue: No bioassay..biomaterial association with "
                    // + fv );
                    log.warn( "Unused factorValue: No bioassay..biomaterial association with " + fv );
                }
            }

        }
    }

    private void clearCache() {
        bioAssayDimensionCache.clear();
        clearArrayDesignCache();
        clearCommonCache();
    }

    /**
     * @param assay
     */
    private void fillInBioAssayAssociations( BioAssay assay ) {

        ArrayDesign arrayDesign = assay.getArrayDesignUsed();
        assert arrayDesign != null;

        arrayDesign = cacheArrayDesign( arrayDesign );
        assert arrayDesign.getId() != null;
        assay.setArrayDesignUsed( arrayDesign );
        assert assay.getArrayDesignUsed().getId() != null;

        for ( BioMaterial material : assay.getSamplesUsed() ) {
            for ( FactorValue factorValue : material.getFactorValues() ) {
                // factors are not compositioned in any more, but by association with the ExperimentalFactor.
                fillInFactorValueAssociations( factorValue );
                factorValue = persistFactorValue( factorValue );
            }
        }
        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( assay.getAccession() != null ) {
            assay.getAccession().setExternalDatabase(
                    persistExternalDatabase( assay.getAccession().getExternalDatabase() ) );
        }

        if ( log.isDebugEnabled() ) log.debug( assay.getSamplesUsed().size() + " bioMaterials for " + assay );

        /*
         * BioMaterials
         */
        persistCollectionElements( assay.getSamplesUsed() );

        if ( assay.getRawDataFile() != null ) {
            assay.setRawDataFile( persistLocalFile( assay.getRawDataFile() ) );
        }

        for ( LocalFile file : assay.getDerivedDataFiles() ) {
            file = persistLocalFile( file );
        }

    }

    /**
     * @param vect
     */
    private BioAssayDimension fillInDesignElementDataVectorAssociations( DesignElementDataVector vect ) {
        CompositeSequence designElement = ( CompositeSequence ) vect.getDesignElement();

        assert designElement != null;

        ArrayDesign ad = designElement.getArrayDesign();
        assert ad != null : designElement + " does not have an array design";

        ad = cacheArrayDesign( ad );

        String key = designElement.getName() + DESIGN_ELEMENT_KEY_SEPARATOR + ad.getName();
        String seqName = null;

        if ( designElement.getBiologicalCharacteristic() != null ) {
            seqName = designElement.getBiologicalCharacteristic().getName();
        }

        if ( log.isDebugEnabled() ) log.debug( "Seeking design element matching key=" + key );
        if ( getDesignElementCache().containsKey( key ) ) {
            designElement = getDesignElementCache().get( key );
            if ( log.isDebugEnabled() ) log.debug( "Found " + designElement + " with key=" + key );
        } else {
            /*
             * because the names of design elements can change, we should try to go by the _sequence_.
             */
            if ( StringUtils.isNotBlank( seqName ) && getDesignElementSequenceCache().containsKey( seqName ) ) {
                if ( log.isDebugEnabled() ) log.debug( "Using sequence name " + seqName + " to identify sequence" );
                designElement = getDesignElementSequenceCache().get( seqName );
                if ( log.isDebugEnabled() ) log.debug( "Found " + designElement + " with sequence key=" + seqName );
            } else {
                /*
                 * No sequence, or the sequence name isn't provided. Of course, if there is no sequence it isn't going
                 * to be very useful.
                 */
                log.warn( "Adding new probe to existing array design " + ad.getShortName() + ": " + designElement
                        + " bioseq=" + designElement.getBiologicalCharacteristic() );

                // throw new IllegalStateException( "Adding new probe to existing array design " + ad.getShortName()
                // + ": " + designElement + " bioseq=" + designElement.getBiologicalCharacteristic()
                // + ": not supported, sorry" );
                designElement = addNewDesignElementToPersistentArrayDesign( ad, designElement );
            }
        }

        assert designElement != null && designElement.getId() != null;
        vect.setDesignElement( designElement ); // use the persistent one.

        BioAssayDimension baDim = checkBioAssayDimensionCache( vect );

        assert vect.getQuantitationType() != null;
        vect.setQuantitationType( persistQuantitationType( vect.getQuantitationType() ) );

        return baDim;
    }

    /**
     * @param experimentalFactor
     * @return
     */
    private ExperimentalFactor fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null ) return null;
        if ( !isTransient( experimentalFactor ) ) return experimentalFactor;

        persistCollectionElements( experimentalFactor.getAnnotations() );

        return experimentalFactor;
    }

    /**
     * @param entity
     */
    private Collection<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment entity ) {
        log.info( "Filling in DesignElementDataVectors..." );

        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();

        int count = 0;
        for ( DesignElementDataVector vect : entity.getRawExpressionDataVectors() ) {
            BioAssayDimension baDim = fillInDesignElementDataVectorAssociations( vect );
            bioAssays.addAll( baDim.getBioAssays() );

            if ( ++count % 10000 == 0 ) {
                log.info( "Filled in " + count + " DesignElementDataVectors" );
            }

            if ( Thread.interrupted() ) {
                log.info( "Cancelled" );
                return null;
            }
        }

        log.info( "Filled in total of " + count + " DesignElementDataVectors, " + bioAssays.size() + " bioassays" );
        return bioAssays;
    }

    /**
     * @param factorValue
     */
    private void fillInFactorValueAssociations( FactorValue factorValue ) {

        fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor() );

        factorValue.setExperimentalFactor( persistExperimentalFactor( factorValue.getExperimentalFactor() ) );

        if ( factorValue.getCharacteristics().size() > 0 ) {
            if ( factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getCharacteristics().size() > 0 ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        }

        // measurement will cascade, but not unit.
        if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
            factorValue.getMeasurement().setUnit( persistUnit( factorValue.getMeasurement().getUnit() ) );
        }

    }

    /**
     * @param assay
     */
    private BioAssay persistBioAssay( BioAssay assay ) {

        if ( assay == null ) return null;
        if ( !isTransient( assay ) ) {
            return assay;
        }

        fillInBioAssayAssociations( assay );
        if ( log.isDebugEnabled() ) log.debug( "Persisting " + assay );

        /*
         * PP changed this to use 'create', as we don't want BioAssays associated with two ExpressionExperiments.
         * BioAssays don't exist on their own so this wouldn't get called in any conceivable situation where
         * findOrCreate would be appropriate (?)
         */
        return bioAssayService.create( assay );
    }

    /**
     * @param bioAssayDimension
     * @return
     */
    private BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) return null;
        if ( !isTransient( bioAssayDimension ) ) return bioAssayDimension;

        List<BioAssay> persistedBioAssays = new ArrayList<BioAssay>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            persistedBioAssays.add( persistBioAssay( bioAssay ) );
        }
        assert persistedBioAssays.size() > 0;
        bioAssayDimension.setBioAssays( persistedBioAssays );
        return bioAssayDimensionService.findOrCreate( bioAssayDimension );
    }

    /**
     * @param entity
     */
    private BioMaterial persistBioMaterial( BioMaterial entity ) {
        if ( entity == null ) return null;
        log.debug( "Persisting " + entity );
        if ( !isTransient( entity ) ) return entity;

        assert entity.getSourceTaxon() != null;

        fillInDatabaseEntry( entity.getExternalAccession() );
        entity.setSourceTaxon( persistTaxon( entity.getSourceTaxon() ) );

        for ( Treatment treatment : entity.getTreatments() ) {

            Characteristic action = treatment.getAction();
            log.debug( treatment + " action: " + action );

            for ( ProtocolApplication protocolApplication : treatment.getProtocolApplications() ) {
                fillInProtocolApplication( protocolApplication );
            }
        }
        return bioMaterialService.findOrCreate( entity );
    }

    /**
     * @param compound
     * @return
     */
    private Compound persistCompound( Compound compound ) {
        if ( compound == null ) return null;
        if ( compound.getIsSolvent() == null )
            throw new IllegalArgumentException( "Compound must have 'isSolvent' value set." );
        return compoundService.findOrCreate( compound );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     * 
     * @param experimentalFactor
     * @return
     */
    private ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        if ( !isTransient( experimentalFactor ) || experimentalFactor == null ) return experimentalFactor;
        assert experimentalFactor.getType() != null;
        fillInExperimentalFactorAssociations( experimentalFactor );
        assert ( !isTransient( experimentalFactor.getExperimentalDesign() ) );
        return experimentalFactorService.create( experimentalFactor );
    }

    /**
     * @param expExp
     * @return
     */
    private ExpressionExperiment persistExpressionExperiment( ExpressionExperiment expExp ) {

        if ( expExp == null ) return null;
        if ( !isTransient( expExp ) ) return expExp;

        log.info( "Persisting " + expExp );

        ExpressionExperiment existing = expressionExperimentService.findByShortName( expExp.getShortName() );
        if ( existing != null ) {
            log.warn( "Expression experiment with same short name exists (" + existing
                    + "), returning it (this method does not handle updates)" );
            return existing;
        }

        expExp.setPrimaryPublication( ( BibliographicReference ) persist( expExp.getPrimaryPublication() ) );

        if ( expExp.getOwner() == null ) {
            expExp.setOwner( defaultOwner );
        }
        expExp.setOwner( ( Contact ) persist( expExp.getOwner() ) );

        persistCollectionElements( expExp.getQuantitationTypes() );
        persistCollectionElements( expExp.getOtherRelevantPublications() );
        persistCollectionElements( expExp.getInvestigators() );

        if ( expExp.getAccession() != null ) {
            fillInDatabaseEntry( expExp.getAccession() );
        }

        // this has to come first and be persisted, so our factorvalues get persisted before we process the bioassays.
        if ( expExp.getExperimentalDesign() != null ) {
            ExperimentalDesign experimentalDesign = expExp.getExperimentalDesign();
            processExperimentalDesign( experimentalDesign );
            assert experimentalDesign.getId() != null;
            expExp.setExperimentalDesign( experimentalDesign );
        }

        checkExperimentalDesign( expExp );

        // this does most of the preparatory work.
        processBioAssays( expExp );
        expExp = expressionExperimentService.create( expExp );
        this.getSession().flush(); // Yes, this is important.

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled" );
            expressionExperimentService.delete( expExp );
            throw new CancellationException( "Thread canceled during EE persisting. " + this.getClass() );
        }

        expressionExperimentService.update( expExp ); // help fix up ACLs. Yes, this is a good idea. See AclAdviceTest

        return expExp;
    }

    /**
     * @param entity
     * @return
     */
    private ExpressionExperimentSubSet persistExpressionExperimentSubSet( ExpressionExperimentSubSet entity ) {
        if ( !isTransient( entity ) ) return entity;

        if ( entity.getBioAssays().size() == 0 ) {
            throw new IllegalArgumentException( "Cannot make a subset with no bioassays" );
        } else if ( isTransient( entity.getSourceExperiment() ) ) {
            throw new IllegalArgumentException(
                    "Subsets are only supported for expressionexperiments that are already persistent" );
        }

        return expressionExperimentSubSetService.findOrCreate( entity );
    }

    /**
     * If we get here first (e.g., via bioAssay->bioMaterial) we have to override the cascade.
     * 
     * @param factorValue
     * @return
     */
    private FactorValue persistFactorValue( FactorValue factorValue ) {
        if ( factorValue == null ) return null;
        if ( !isTransient( factorValue ) ) return factorValue;
        if ( isTransient( factorValue.getExperimentalFactor() ) ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        fillInFactorValueAssociations( factorValue );

        return factorValueService.findOrCreate( factorValue );

    }

    /**
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     * 
     * @param expressionExperiment
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment ) {

        Collection<BioAssay> alreadyFilled = new HashSet<BioAssay>();

        if ( expressionExperiment.getRawExpressionDataVectors().size() > 0 ) {
            alreadyFilled = fillInExpressionExperimentDataVectorAssociations( expressionExperiment );
            expressionExperiment.setBioAssays( alreadyFilled );
        } else {
            for ( BioAssay bA : expressionExperiment.getBioAssays() ) {
                fillInBioAssayAssociations( bA );
                alreadyFilled.add( bA );
            }
        }

        // for ( ExpressionExperimentSubSet subset : expressionExperiment.getSubsets() ) {
        // for ( BioAssay bA : subset.getBioAssays() ) {
        // bA.setId( persistBioAssay( bA ).getId() );
        // assert bA.getArrayDesignUsed().getId() != null;
        //
        // final BioAssay baF = bA;
        //
        // // thaw - this is necessary to avoid lazy exceptions later, but perhaps could be done more elegantly!
        // HibernateTemplate templ = this.getHibernateTemplate();
        // templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
        // public Object doInHibernate( org.hibernate.Session session )
        // throws org.hibernate.HibernateException {
        // ArrayDesign arrayDesignUsed = baF.getArrayDesignUsed();
        // session.update( arrayDesignUsed );
        // if ( arrayDesignUsed.getDesignProvider() != null ) {
        // session.update( arrayDesignUsed.getDesignProvider().getAuditTrail() );
        // arrayDesignUsed.getDesignProvider().getAuditTrail().getEvents().size();
        // }
        // arrayDesignUsed.getMergees().size();
        // return null;
        // }
        // } );
        //
        // if ( !alreadyFilled.contains( bA ) ) {
        // /*
        // * This is an exceptional circumstance that might indicate problems with the source.
        // */
        // log.error( "Subset bioassay " + bA + " found that isn't in the parent. Parent contains:" );
        // StringBuilder buf = new StringBuilder();
        // buf.append( "\n" );
        // for ( BioAssay assay : alreadyFilled ) {
        // buf.append( assay + "\n" );
        // }
        // log.error( buf );
        // throw new IllegalStateException( bA + " in subset " + subset + " not in the parent experiment?" );
        // }
        // }
        // }
    }

    /**
     * @param experimentalDesign
     */
    private void processExperimentalDesign( ExperimentalDesign experimentalDesign ) {

        persistCollectionElements( experimentalDesign.getTypes() );

        // withhold to avoid premature cascade.
        Collection<ExperimentalFactor> factors = experimentalDesign.getExperimentalFactors();
        experimentalDesign.setExperimentalFactors( null );

        // note we use create because this is specific to the instance. (we're overriding a cascade)
        experimentalDesign = experimentalDesignService.create( experimentalDesign );

        // put back.
        experimentalDesign.setExperimentalFactors( factors );

        for ( ExperimentalFactor experimentalFactor : experimentalDesign.getExperimentalFactors() ) {

            experimentalFactor.setExperimentalDesign( experimentalDesign );

            // override cascade like above.
            Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
            experimentalFactor.setFactorValues( null );
            experimentalFactor = persistExperimentalFactor( experimentalFactor );

            for ( FactorValue factorValue : factorValues ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                fillInFactorValueAssociations( factorValue );
            }

            // factorvalue is cascaded.
            experimentalFactor.setFactorValues( factorValues );
            experimentalFactorService.update( experimentalFactor );
        }
    }

}