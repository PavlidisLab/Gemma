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
package ubic.gemma.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Methods to test business-key-related issues on objects. The 'checkValidKey' methods can be used to check whether an
 * object has the required business key values filled in. An exception is thrown if they don't.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BusinessKey {

    private static Log log = LogFactory.getLog( BusinessKey.class.getName() );

    public static void addRestrictions( Criteria queryObject, ArrayDesign arrayDesign ) {
        addNameRestriction( queryObject, arrayDesign );

        /*
         * Test whether ANY of the associated external references match any of the given external references.
         */
        if ( arrayDesign.getExternalReferences().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "externalReferences" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( DatabaseEntry databaseEntry : arrayDesign.getExternalReferences() ) {
                disjunction.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) );
            }
            externalRef.add( disjunction );
        }

        if ( arrayDesign.getDesignProvider() != null
                && StringUtils.isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            queryObject.createCriteria( "designProvider" ).add(
                    Restrictions.eq( "name", arrayDesign.getDesignProvider().getName() ) );
        }

        if ( log.isDebugEnabled() ) log.debug( queryObject.toString() );
    }

    /**
     * Restricts the query to the provided BioSequence.
     * 
     * @param queryObject
     * @param bioSequence The object used to create the criteria
     * @param propertyName Often this will be 'bioSequence'
     */
    public static void attachCriteria( Criteria queryObject, BioSequence bioSequence, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, bioSequence );
        log.warn( queryObject.toString() );
    }

    /**
     * Restricts the query to the provided OntologyEntry.
     * 
     * @param queryObject
     * @param ontologyEntry The object used to create the criteria
     * @param propertyName Often this will be 'ontologyEntry'
     */
    public static void attachCriteria( Criteria queryObject, OntologyEntry ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, ontologyEntry );
        log.debug( queryObject.toString() );
    }

    /**
     * @param bibliographicReference
     */
    public static void checkKey( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getPubAccession() == null
                || bibliographicReference.getPubAccession().getAccession() == null ) {
            throw new IllegalArgumentException( "BibliographicReference was null or had no accession : "
                    + bibliographicReference );
        }
    }

    /**
     * @param factorValue
     */
    public static void checkKey( FactorValue factorValue ) {
        if ( factorValue.getValue() == null && factorValue.getMeasurement() == null
                && factorValue.getOntologyEntry() == null ) {
            throw new IllegalArgumentException(
                    "FactorValue must have a value (or associated measurement or ontology entry)." );
        }
    }

    /**
     * @param gene
     */
    public static void checkKey( Gene gene ) {
        if ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getNcbiId() == null ) {
            throw new IllegalArgumentException( "Gene must have official symbol with taxon, or ncbiId" );
        }

    }

    /**
     * @param geneProduct
     */
    public static void checkKey( GeneProduct geneProduct ) {
        if ( geneProduct.getNcbiId() == null ) {
            throw new IllegalArgumentException( "Gene must have ncbiId" );
        }
    }

    /**
     * @param ontologyEntry
     */
    public static void checkKey( OntologyEntry ontologyEntry ) {
        if ( ( ontologyEntry.getAccession() == null || ontologyEntry.getExternalDatabase() == null )
                && ( ontologyEntry.getCategory() == null || ontologyEntry.getValue() == null ) ) {
            throw new IllegalArgumentException( "Either accession, or category+value must be filled in." );
        }
    }

    /**
     * @param user
     */
    public static void checkKey( User user ) {
        if ( user == null || StringUtils.isBlank( user.getUserName() ) ) {
            throw new IllegalArgumentException( "User was null or had no userName defined" );
        }
    }

    public static void checkValidKey( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null
                || ( StringUtils.isBlank( arrayDesign.getName() ) && arrayDesign.getExternalReferences().size() == 0 ) ) {
            throw new IllegalArgumentException( arrayDesign + " did not have a valid key" );
        }
    }

    /**
     * @param bioSequence
     */
    public static void checkValidKey( BioSequence bioSequence ) {
        if ( bioSequence == null
                || bioSequence.getTaxon() == null
                || ( StringUtils.isBlank( bioSequence.getName() ) && StringUtils.isBlank( bioSequence.getSequence() ) && bioSequence
                        .getSequenceDatabaseEntry() == null ) ) {
            throw new IllegalArgumentException( "Biosequence did not have a valid key" );
        }
    }

    /**
     * @param gene
     */
    public static void checkValidKey( Gene gene ) {
        // FIXME make key.
        return;
    }

    /**
     * @param geneProduct
     */
    public static void checkValidKey( GeneProduct geneProduct ) {
        boolean ok = true;

        if ( geneProduct == null ) ok = false;

        if ( StringUtils.isBlank( geneProduct.getNcbiId() ) && StringUtils.isBlank( geneProduct.getName() ) )
            ok = false;

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key" );
        }
    }

    /**
     * @param taxon
     */
    public static void checkValidKey( Taxon taxon ) {
        if ( taxon == null
                || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) && StringUtils
                        .isBlank( taxon.getScientificName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    /**
     * @param localFile
     */
    public static void checkValidKey( ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null || localFile.getLocalURL() == null ) {
            if ( localFile != null )
                log.error( "Localfile without valid key: localURL=" + localFile.getLocalURL() + " remoteUrL="
                        + localFile.getRemoteURL() + " size=" + localFile.getSize() );
            throw new IllegalArgumentException( "localFile was null or had no valid business keys" );
        }
    }

    /**
     * @param queryObject
     * @param factorValue
     */
    public static void createQueryObject( Criteria queryObject, FactorValue factorValue ) {
        if ( factorValue.getValue() != null ) {
            queryObject.add( Restrictions.eq( "value", factorValue.getValue() ) );
        } else if ( factorValue.getOntologyEntry() != null ) {
            BusinessKey.attachCriteria( queryObject, factorValue.getOntologyEntry(), "ontologyEntry" );
        } else if ( factorValue.getMeasurement() != null ) {
            queryObject.add( Restrictions.eq( "measurement", factorValue.getMeasurement() ) );
        }
    }

    /**
     * @param queryObject
     * @param gene
     */
    public static void createQueryObject( Criteria queryObject, Gene gene ) {
        // prefeerred key is NCBI.
        if ( gene.getNcbiId() != null ) {
            queryObject.add( Restrictions.eq( "ncbiId", gene.getNcbiId() ) );
        } else if ( gene.getOfficialSymbol() != null && gene.getTaxon() != null ) {
            queryObject.add( Restrictions.eq( "officialSymbol", gene.getOfficialSymbol() ) ).add(
                    Restrictions.eq( "taxon", gene.getTaxon() ) );
        }

    }

    /**
     * @param queryObject
     * @param geneProduct
     */
    public static void createQueryObject( Criteria queryObject, GeneProduct geneProduct ) {
        queryObject.add( Restrictions.eq( "ncbiId", geneProduct.getNcbiId() ) );
    }

    /**
     * @param session
     * @param ontologyEntry
     * @return
     */
    public static Criteria createQueryObject( Session session, ArrayDesign arrayDesign ) {
        Criteria queryObject = session.createCriteria( ArrayDesign.class );
        addRestrictions( queryObject, arrayDesign );
        return queryObject;
    }

    /**
     * @param session
     * @param bioSequence
     * @return
     */
    public static Criteria createQueryObject( Session session, BioSequence bioSequence ) {
        Criteria queryObject = session.createCriteria( BioSequence.class );
        addRestrictions( queryObject, bioSequence );
        return queryObject;
    }

    /**
     * @param session
     * @param ontologyEntry
     * @return
     */
    public static Criteria createQueryObject( Session session, OntologyEntry ontologyEntry ) {
        Criteria queryObject = session.createCriteria( OntologyEntry.class );
        addRestrictions( queryObject, ontologyEntry );
        return queryObject;
    }

    /**
     * @param queryObject
     * @param describable
     */
    private static void addNameRestriction( Criteria queryObject, Describable describable ) {
        if ( describable.getName() != null ) queryObject.add( Restrictions.eq( "name", describable.getName() ) );
    }

    /**
     * @param innerQuery
     * @param bioSequence
     */
    private static void addRestrictions( Criteria queryObject, BioSequence bioSequence ) {

        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            addNameRestriction( queryObject, bioSequence );
        }
        if ( StringUtils.isNotBlank( bioSequence.getSequence() ) ) {
            queryObject.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );
        }

        addRestrictions( queryObject, bioSequence.getTaxon() );

        if ( bioSequence.getSequenceDatabaseEntry() != null ) {
            queryObject.createCriteria( "sequenceDatabaseEntry" ).add(
                    Restrictions.eq( "accession", bioSequence.getSequenceDatabaseEntry().getAccession() ) );
        }

    }

    /**
     * @param queryObject
     * @param ontologyEntry
     */
    private static void addRestrictions( Criteria queryObject, OntologyEntry ontologyEntry ) {
        if ( ontologyEntry.getAccession() != null && ontologyEntry.getExternalDatabase() != null ) {
            queryObject.add( Restrictions.eq( "accession", ontologyEntry.getAccession() ) ).createCriteria(
                    "externalDatabase" ).add( Restrictions.eq( "name", ontologyEntry.getExternalDatabase().getName() ) );
        } else {
            queryObject.add( Restrictions.ilike( "category", ontologyEntry.getCategory() ) ).add(
                    Restrictions.ilike( "value", ontologyEntry.getValue() ) );
        }
    }

    /**
     * Assumes parameter name is 'taxon'.
     * 
     * @param queryObject
     * @param taxon
     */
    private static void addRestrictions( Criteria queryObject, Taxon taxon ) {
        checkValidKey( taxon );

        if ( taxon.getNcbiId() != null ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( taxon.getScientificName() ) ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "scientificName", taxon.getScientificName() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.createCriteria( "taxon" ).add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

    /**
     * @param experimentalFactor
     */
    public static void checkValidKey( ExperimentalFactor experimentalFactor ) {
        if ( StringUtils.isBlank( experimentalFactor.getName() ) && experimentalFactor.getCategory() == null ) {
            throw new IllegalArgumentException( "Experimental factor must have name or category" );
        }
    }

    /**
     * @param queryObject
     * @param experimentalFactor
     */
    public static void addRestrictions( Criteria queryObject, ExperimentalFactor experimentalFactor ) {
        if ( StringUtils.isNotBlank( experimentalFactor.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", experimentalFactor.getName() ) );
        }

        if ( experimentalFactor.getCategory() != null ) {
            // FIXME this might not be complete.
            queryObject.createCriteria( "category" ).add(
                    Restrictions.eq( "accession", experimentalFactor.getCategory().getAccession() ) ).createCriteria(
                    "externalDatabase" ).add(
                    Restrictions.eq( "name", experimentalFactor.getCategory().getExternalDatabase().getName() ) );
        }
    }

    public static void checkKey( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector did not have complete business key "
                    + designElementDataVector );
        }
    }
}
