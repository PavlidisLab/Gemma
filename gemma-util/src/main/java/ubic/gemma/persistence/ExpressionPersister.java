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

import org.springframework.orm.hibernate3.HibernateTemplate;

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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.model.expression.biomaterial.CompoundService;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;

/**
 * Expression experiment is a top-level persister. That is, it contains only outbound associations.
 * 
 * @spring.property name="factorValueService" ref="factorValueService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="bioMaterialService" ref="bioMaterialService"
 * @spring.property name="bioAssayService" ref="bioAssayService"
 * @spring.property name="compoundService" ref="compoundService"
 * @spring.property name="experimentalDesignService" ref="experimentalDesignService"
 * @spring.property name="experimentalFactorService" ref="experimentalFactorService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class ExpressionPersister extends ArrayDesignPersister {

    private DesignElementDataVectorService designElementDataVectorService;

    private ExpressionExperimentService expressionExperimentService;

    private BioAssayDimensionService bioAssayDimensionService;

    private BioAssayService bioAssayService;

    private BioMaterialService bioMaterialService;

    private FactorValueService factorValueService;

    private CompoundService compoundService;

    private ExperimentalDesignService experimentalDesignService;

    private ExperimentalFactorService experimentalFactorService;

    Map<String, BioAssayDimension> bioAssayDimensionCache = new HashMap<String, BioAssayDimension>();

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
        DesignElement designElement = vect.getDesignElement();

        assert designElement != null;

        ArrayDesign ad = designElement.getArrayDesign();
        assert ad != null : designElement + " does not have an array design";

        ad = cacheArrayDesign( ad );

        String key = designElement.getName() + DESIGN_ELEMENT_KEY_SEPARATOR + ad.getName();

        if ( designElementCache.containsKey( key ) ) {
            designElement = designElementCache.get( key );
        } else {
            // means the array design is lacking it.
            designElement = addNewDesignElementToPersistentArrayDesign( ad, designElement );
        }

        assert designElement != null && designElement.getId() != null;
        vect.setDesignElement( designElement ); // shouldn't have to do this. Some kind of hibernate weirdness.s

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
        if ( !isTransient( experimentalFactor ) ) return experimentalFactor;

        persistCollectionElements( experimentalFactor.getAnnotations() );

        Characteristic category = experimentalFactor.getCategory();
        if ( category != null ) {
            experimentalFactor.setCategory( persistCharacteristicAssociations( category ) );
        }
        return experimentalFactor;
    }

    /**
     * @param entity
     */
    private Collection<BioAssay> fillInExpressionExperimentDataVectorAssociations( ExpressionExperiment entity ) {
        log.info( "Filling in DesignElementDataVectors..." );

        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();

        int count = 0;
        for ( DesignElementDataVector vect : entity.getDesignElementDataVectors() ) {
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

        if ( factorValue.getOntologyEntry() != null ) {
            if ( factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
            Characteristic ontologyEntry = factorValue.getOntologyEntry();
            factorValue.setOntologyEntry( persistCharacteristicAssociations( ontologyEntry ) );
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getOntologyEntry() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        }
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
            result = persistExpressionExperiment( ( ExpressionExperiment ) entity );
            clearCache();
            return result;
        } else if ( entity instanceof BioAssayDimension ) {
            return persistBioAssayDimension( ( BioAssayDimension ) entity );
        } else if ( entity instanceof BioMaterial ) {
            return persistBioMaterial( ( BioMaterial ) entity );
        } else if ( entity instanceof BioAssay ) {
            return persistBioAssay( ( BioAssay ) entity );
        } else if ( entity instanceof Compound ) {
            return persistCompound( ( Compound ) entity );
        } else if ( entity instanceof DesignElementDataVector ) {
            return persistDesignElementDataVector( ( DesignElementDataVector ) entity );
        }

        return super.persist( entity );

    }

    private void clearCache() {
        bioAssayDimensionCache.clear();
        clearArrayDesignCache();
        clearCommonCache();
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

        return bioAssayService.findOrCreate( assay );
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

        entity.setExternalAccession( persistDatabaseEntry( entity.getExternalAccession() ) );
        entity.setMaterialType( persistCharacteristicAssociations( entity.getMaterialType() ) );
        entity.setSourceTaxon( persistTaxon( entity.getSourceTaxon() ) );

        for ( Treatment treatment : entity.getTreatments() ) {

            Characteristic action = treatment.getAction();
            treatment.setAction( persistCharacteristicAssociations( action ) );
            log.debug( treatment + " action: " + action );

            for ( ProtocolApplication protocolApplication : treatment.getProtocolApplications() ) {
                fillInProtocolApplication( protocolApplication );
            }
        }

        // fillInOntologyEntries( entity.getCharacteristics() ); // characteristics themselves should cascade

        return bioMaterialService.findOrCreate( entity );
    }

    /**
     * @param compound
     * @return
     */
    private Compound persistCompound( Compound compound ) {
        if ( compound == null ) return null;
        compound.setCompoundIndices( persistCharacteristicAssociations( compound.getCompoundIndices() ) );
        if ( compound.getIsSolvent() == null )
            throw new IllegalArgumentException( "Compound must have 'isSolvent' value set." );
        return compoundService.findOrCreate( compound );
    }

    /**
     * This is used when creating vectors "one by one" rather than by composition with an ExpressionExperiment. Not
     * normally used.
     * 
     * @param vector
     * @return
     */
    private DesignElementDataVector persistDesignElementDataVector( DesignElementDataVector vector ) {
        if ( vector == null ) return null;
        this.fillInDesignElementDataVectorAssociations( vector );
        vector.setExpressionExperiment( persistExpressionExperiment( vector.getExpressionExperiment() ) );
        return designElementDataVectorService.findOrCreate( vector );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     * 
     * @param experimentalFactor
     * @return
     */
    private ExperimentalFactor persistExperimentalFactor( ExperimentalFactor experimentalFactor ) {
        if ( !isTransient( experimentalFactor ) ) return experimentalFactor;
        fillInExperimentalFactorAssociations( experimentalFactor );
        return experimentalFactorService.create( experimentalFactor );
    }

    /**
     * @param entity
     * @return
     */
    private ExpressionExperiment persistExpressionExperiment( ExpressionExperiment entity ) {

        if ( entity == null ) return null;
        if ( !isTransient( entity ) ) return entity;

        log.info( "Persisting " + entity );

        ExpressionExperiment existing = expressionExperimentService.findByName( entity.getName() );
        if ( existing != null ) {
            log.warn( "Expression experiment with same name exists (" + existing
                    + "), returning it (this method does not handle updates)" );
            return existing;
        }

        entity.setPrimaryPublication( ( BibliographicReference ) persist( entity.getPrimaryPublication() ) );

        if ( entity.getOwner() == null ) {
            entity.setOwner( defaultOwner );
        }
        entity.setOwner( ( Contact ) persist( entity.getOwner() ) );

        persistCollectionElements( entity.getQuantitationTypes() );
        persistCollectionElements( entity.getOtherRelevantPublications() );
        persistCollectionElements( entity.getInvestigators() );

        if ( entity.getAccession() != null ) {
            entity.setAccession( persistDatabaseEntry( entity.getAccession() ) );
        }

        // this has to come first and be persisted, so our factorvalues get persisted before we process the bioassays.
        if ( entity.getExperimentalDesign() != null ) {
            ExperimentalDesign experimentalDesign = entity.getExperimentalDesign();
            processExperimentalDesign( experimentalDesign );
            assert experimentalDesign.getId() != null;
            entity.setExperimentalDesign( experimentalDesign );
        }

        // this does most of the preparatory work.
        processBioAssays( entity );

        log.info( "Persisting " + entity );
        entity = expressionExperimentService.create( entity );
        this.getSession().flush(); // Yes, this is important.

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled" );
            expressionExperimentService.delete( entity );
            throw new CancellationException( "Thread canceled during EE persisting. " + this.getClass() );
        }

        return entity;
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

        fillInFactorValueAssociations( factorValue );

        // we use create because factor values are specific to this design.
        return factorValueService.create( factorValue );

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
     * Handle persisting of the bioassays on the way to persisting the expression experiment.
     * 
     * @param expressionExperiment
     */
    private void processBioAssays( ExpressionExperiment expressionExperiment ) {

        Collection<BioAssay> alreadyFilled = new HashSet<BioAssay>();

        if ( expressionExperiment.getDesignElementDataVectors().size() > 0 ) {
            alreadyFilled = fillInExpressionExperimentDataVectorAssociations( expressionExperiment );
            expressionExperiment.setBioAssays( alreadyFilled );
        } else {
            for ( BioAssay bA : expressionExperiment.getBioAssays() ) {
                fillInBioAssayAssociations( bA );
                alreadyFilled.add( bA );
            }
        }

        for ( ExpressionExperimentSubSet subset : expressionExperiment.getSubsets() ) {
            for ( BioAssay bA : subset.getBioAssays() ) {
                bA.setId( persistBioAssay( bA ).getId() );
                assert bA.getArrayDesignUsed().getId() != null;

                final BioAssay baF = bA;

                // thaw - this is necessary to avoid lazy exceptions later, but perhaps could be done more elegantly!
                HibernateTemplate templ = this.getHibernateTemplate();
                templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        ArrayDesign arrayDesignUsed = baF.getArrayDesignUsed();
                        session.update( arrayDesignUsed );
                        session.update( arrayDesignUsed.getDesignProvider().getAuditTrail() );
                        arrayDesignUsed.getDesignProvider().getAuditTrail().getEvents().size();
                        arrayDesignUsed.getMergees().size();
                        return null;
                    }
                } );

                if ( !alreadyFilled.contains( bA ) ) {
                    /*
                     * This is an exceptional circumstance that might indicate problems with the source.
                     */
                    log.error( "Subset bioassay " + bA + " found that isn't in the parent. Parent contains:" );
                    StringBuilder buf = new StringBuilder();
                    buf.append( "\n" );
                    for ( BioAssay assay : alreadyFilled ) {
                        buf.append( assay + "\n" );
                    }
                    log.error( buf );
                    throw new IllegalStateException( bA + " in subset " + subset + " not in the parent experiment?" );
                }
            }
        }
    }

    /**
     * @param experimentalDesign
     */
    private void processExperimentalDesign( ExperimentalDesign experimentalDesign ) {

        /* At this point, the bioassay experimental factor values have already been persisted. */

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

            experimentalFactor = persistExperimentalFactor( experimentalFactor );

            // factorvalue is cascaded.
            for ( FactorValue factorValue : experimentalFactor.getFactorValues() ) {
                factorValue.setExperimentalFactor( experimentalFactor );
                fillInFactorValueAssociations( factorValue );
            }
        }
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
     * @param designElementDataVectorService The designElementDataVectorService to set.
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
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

}