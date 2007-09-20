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
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
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

    /**
     * @param queryObject
     * @param describable
     */
    private static void addNameRestriction( Criteria queryObject, Describable describable ) {
        if ( describable.getName() != null ) queryObject.add( Restrictions.eq( "name", describable.getName() ) );
    }

    /**
     * @param queryObject
     * @param arrayDesign
     */
    public static void addRestrictions( Criteria queryObject, ArrayDesign arrayDesign ) {

        /*
         * Test whether ANY of the associated external references match any of the given external references.
         */
        if ( arrayDesign.getExternalReferences().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "externalReferences" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( DatabaseEntry databaseEntry : arrayDesign.getExternalReferences() ) {
                disjunction.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) );
                // FIXME this should include the ExternalDatabase in the criteria.
            }
            externalRef.add( disjunction );
            return;
        }

        addNameRestriction( queryObject, arrayDesign );

        if ( arrayDesign.getShortName() != null )
            queryObject.add( Restrictions.eq( "shortName", arrayDesign.getShortName() ) );

        if ( arrayDesign.getDesignProvider() != null
                && StringUtils.isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            queryObject.createCriteria( "designProvider" ).add(
                    Restrictions.eq( "name", arrayDesign.getDesignProvider().getName() ) );
        }

    }

    /**
     * @param queryObject
     * @param databaseEntry
     * @param attributeName
     */
    public static void attachCriteria( Criteria queryObject, DatabaseEntry databaseEntry, String attributeName ) {
        Criteria externalRef = queryObject.createCriteria( attributeName );
        addRestrictions( externalRef, databaseEntry );
    }

    /**
     * @param innerQuery
     * @param bioSequence
     */
    public static void addRestrictions( Criteria queryObject, BioSequence bioSequence ) {

        if ( bioSequence.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", bioSequence.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            addNameRestriction( queryObject, bioSequence );
        }

        attachCriteria( queryObject, bioSequence.getTaxon(), "taxon" );
        // The finder now does the additional checking for equality of sequence and/or database entry.
        // if ( bioSequence.getSequenceDatabaseEntry() != null ) {
        // // this is problematic - sometimes the old entry doesn't have the database entry.
        // queryObject.createCriteria( "sequenceDatabaseEntry" ).add(
        // Restrictions.eq( "accession", bioSequence.getSequenceDatabaseEntry().getAccession() ) );
        // } else if ( StringUtils.isNotBlank( bioSequence.getSequence() ) ) {
        // // this is also problematic - sometimes the old entry doesn't have the sequence.
        // queryObject.add( Restrictions.eq( "sequence", bioSequence.getSequence() ) );
        // }

    }

    /**
     * @param queryObject
     * @param chromosome
     */
    public static void addRestrictions( Criteria queryObject, Chromosome chromosome ) {
        queryObject.add( Restrictions.eq( "name", chromosome.getName() ) );
        attachCriteria( queryObject, chromosome.getTaxon(), "taxon" );
    }

    /**
     * @param queryObject
     * @param contact
     */
    public static void addRestrictions( Criteria queryObject, Contact contact ) {
        if ( StringUtils.isNotBlank( contact.getEmail() ) ) {
            // email is unique.
            queryObject.add( Restrictions.eq( "email", contact.getEmail() ) );
            return;
        }

        if ( StringUtils.isNotBlank( contact.getName() ) )
            queryObject.add( Restrictions.eq( "name", contact.getName() ) );

        if ( StringUtils.isNotBlank( contact.getAddress() ) )
            queryObject.add( Restrictions.eq( "address", contact.getAddress() ) );

        if ( StringUtils.isNotBlank( contact.getPhone() ) )
            queryObject.add( Restrictions.eq( "phone", contact.getPhone() ) );

    }

    /**
     * @param queryObject
     * @param experimentalFactor
     */
    public static void addRestrictions( Criteria queryObject, ExperimentalFactor experimentalFactor ) {

        if ( experimentalFactor.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", experimentalFactor.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( experimentalFactor.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", experimentalFactor.getName() ) );
        }

        if ( experimentalFactor.getCategory() != null ) {
            attachCriteria( queryObject, experimentalFactor.getCategory(), "category" );
        }
    }

    /**
     * @param innerQuery
     * @param gene
     */
    public static void addRestrictions( Criteria queryObject, Gene gene ) {
        if ( gene.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getId() ) );
        } else if ( StringUtils.isNotBlank( gene.getNcbiId() ) ) {
            queryObject.add( Restrictions.eq( "ncbiId", gene.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            queryObject.add( Restrictions.eq( "officialSymbol", gene.getOfficialSymbol() ) );

            attachCriteria( queryObject, gene.getTaxon(), "taxon" );

            // Need either the official name or the location to be unambiguous.
            if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
                queryObject.add( Restrictions.eq( "officialName", gene.getOfficialName() ) );
            } else if ( gene.getPhysicalLocation() != null ) {
                attachCriteria( queryObject, gene.getPhysicalLocation(), "physicalLocation" );
            }

        } else {
            throw new IllegalArgumentException( "No valid key " + gene );
        }
    }

    /**
     * @param queryObject
     * @param gene2GOAssociation
     */
    public static void addRestrictions( Criteria queryObject, Gene2GOAssociation gene2GOAssociation ) {
        attachCriteria( queryObject, gene2GOAssociation.getGene(), "gene" );
        attachCriteria( queryObject, gene2GOAssociation.getOntologyEntry(), "ontologyEntry" );
    }

    /**
     * @param queryObject
     * @param ontologyEntry
     * @param propertyName
     */
    private static void attachCriteria( Criteria queryObject, VocabCharacteristic ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, ontologyEntry );
    }

    /**
     * @param queryObject
     * @param characteristic
     */
    public static void addRestrictions( Criteria queryObject, VocabCharacteristic characteristic ) {

        if ( characteristic.getCategoryUri() != null ) {
            queryObject.add( Restrictions.eq( "categoryUri", characteristic.getCategoryUri() ) );
        } else {
            assert characteristic.getCategory() != null;
            queryObject.add( Restrictions.eq( "category", characteristic.getCategory() ) );
        }

        if ( characteristic.getValueUri() != null ) {
            queryObject.add( Restrictions.eq( "valueUri", characteristic.getValueUri() ) );
        } else {
            assert characteristic.getValue() != null;
            queryObject.add( Restrictions.eq( "value", characteristic.getValue() ) );
        }
    }

    /**
     * @param queryObject
     * @param characteristic
     */
    public static void addRestrictions( Criteria queryObject, Characteristic characteristic ) {
        if ( characteristic instanceof VocabCharacteristic ) {
            addRestrictions( queryObject, ( VocabCharacteristic ) characteristic );
        } else {
            if ( characteristic.getCategory() != null ) {
                queryObject.add( Restrictions.eq( "category", characteristic.getCategory() ) );
            }
            assert characteristic.getValue() != null;
            queryObject.add( Restrictions.eq( "value", characteristic.getValue() ) );
        }

    }

    /**
     * @param queryObject
     * @param taxon
     */
    public static void addRestrictions( Criteria queryObject, Taxon taxon ) {
        checkValidKey( taxon );
        attachCriteria( queryObject, taxon );
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
    }

    /**
     * Restricts the query to the provided Gene.
     * 
     * @param queryObject
     * @param gene
     * @param propertyName
     */
    public static void attachCriteria( Criteria queryObject, Gene gene, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, gene );
    }

    /**
     * Restricts the query to the provided OntologyEntry.
     * 
     * @param queryObject
     * @param ontologyEntry The object used to create the criteria
     * @param propertyName Often this will be 'ontologyEntry'
     */
    public static void attachCriteria( Criteria queryObject, Characteristic ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        addRestrictions( innerQuery, ontologyEntry );
    }

    /**
     * @param queryObject
     * @param physicalLocation
     * @param attributeName
     */
    public static void attachCriteria( Criteria queryObject, PhysicalLocation physicalLocation, String attributeName ) {
        Criteria nestedCriteria = queryObject.createCriteria( attributeName );

        if ( physicalLocation.getChromosome() == null ) {
            throw new IllegalArgumentException();
        }

        if ( physicalLocation.getChromosome().getId() != null ) {
            nestedCriteria.createCriteria( "chromosome" ).add(
                    Restrictions.eq( "id", physicalLocation.getChromosome().getId() ) );
        } else {
            // FIXME should add taxon to this.
            nestedCriteria.createCriteria( "chromosome" ).add(
                    Restrictions.eq( "name", physicalLocation.getChromosome().getName() ) );
        }

        if ( physicalLocation.getNucleotide() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotide", physicalLocation.getNucleotide() ) );

        if ( physicalLocation.getNucleotideLength() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotideLength", physicalLocation.getNucleotideLength() ) );

    }

    /**
     * @param queryObject
     * @param taxon
     */
    private static void attachCriteria( Criteria queryObject, Taxon taxon ) {
        if ( taxon == null ) throw new IllegalArgumentException( "Taxon was null" );
        if ( taxon.getNcbiId() != null ) {
            queryObject.add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( taxon.getScientificName() ) ) {
            queryObject.add( Restrictions.eq( "scientificName", taxon.getScientificName() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

    /**
     * Restricts query to the given Taxon.
     * 
     * @param queryObject
     * @param taxon
     * @param propertyName often "taxon"
     */
    public static void attachCriteria( Criteria queryObject, Taxon taxon, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        attachCriteria( innerQuery, taxon );
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
     * @param contact
     */
    public static void checkKey( Contact contact ) {
        if ( contact == null
                || ( StringUtils.isBlank( contact.getName() ) && StringUtils.isBlank( contact.getAddress() )
                        && StringUtils.isBlank( contact.getEmail() ) && StringUtils.isBlank( contact.getPhone() ) ) ) {
            throw new IllegalArgumentException( "Contact must have at least some information filled in!" );
        }
    }

    public static void checkKey( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            throw new IllegalArgumentException( "DesignElementDataVector did not have complete business key "
                    + designElementDataVector );
        }
    }

    /**
     * @param factorValue
     */
    public static void checkKey( FactorValue factorValue ) {
        if ( factorValue.getValue() == null && factorValue.getMeasurement() == null
                && factorValue.getCharacteristics().size() == 0 ) {
            throw new IllegalArgumentException(
                    "FactorValue must have a value (or associated measurement or characteristics)." );
        }
    }

    /**
     * @param gene
     */
    public static void checkKey( Gene gene ) {
        if ( ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getPhysicalLocation() == null
                && gene.getProducts() == null && gene.getProducts().size() == 0 )
                && gene.getNcbiId() == null ) {
            throw new IllegalArgumentException(
                    "No valid key for "
                            + gene
                            + ": Gene must have official symbol and name with taxon + physical location or gene products, or ncbiId" );
        }
    }

    /**
     * @param ontologyEntry
     */
    public static void checkKey( Characteristic ontologyEntry ) {

        if ( ontologyEntry instanceof VocabCharacteristic ) {
            if ( ( ( VocabCharacteristic ) ontologyEntry ).getValueUri() == null )
                throw new IllegalArgumentException();
        } else {
            if ( ontologyEntry.getValue() == null ) throw new IllegalArgumentException();
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
                || ( StringUtils.isBlank( arrayDesign.getName() ) && StringUtils.isBlank( arrayDesign.getShortName() ) && arrayDesign
                        .getExternalReferences().size() == 0 ) ) {
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
            throw new IllegalArgumentException( bioSequence + " did not have a valid key" );
        }
    }

    /**
     * @param chromosome
     */
    public static void checkValidKey( Chromosome chromosome ) {
        if ( StringUtils.isBlank( chromosome.getName() ) ) {
            throw new IllegalArgumentException( "Chromosome did not have a valid key" );
        }
        checkValidKey( chromosome.getTaxon() );
    }

    /**
     * @param databaseEntry
     */
    public static void checkValidKey( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null
                || databaseEntry.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "DatabaseEntry does not have valid key" );
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
     * @param gene
     */
    public static void checkValidKey( Gene gene ) {
        if ( gene == null
                || ( gene.getNcbiId() == null && ( StringUtils.isBlank( gene.getOfficialSymbol() )
                        || gene.getTaxon() == null || StringUtils.isBlank( gene.getOfficialName() ) ) ) ) {
            throw new IllegalArgumentException(
                    "Gene does not have valid key (needs NCBI numeric id or Official Symbol + Official Name + Taxon" );
        }
    }

    /**
     * @param gene2GOAssociation
     */
    public static void checkValidKey( Gene2GOAssociation gene2GOAssociation ) {
        checkValidKey( gene2GOAssociation.getGene() );
        checkValidKey( gene2GOAssociation.getOntologyEntry() );
    }

    /**
     * @param ontologyEntry
     */
    private static void checkValidKey( VocabCharacteristic ontologyEntry ) {
        // TODO Auto-generated method stub

    }

    /**
     * @param geneProduct
     */
    public static void checkValidKey( GeneProduct geneProduct ) {
        if ( geneProduct.getId() != null ) return;

        boolean ok = true;

        if ( geneProduct == null ) ok = false;

        if ( StringUtils.isNotBlank( geneProduct.getNcbiId() ) && StringUtils.isBlank( geneProduct.getName() ) )
            ok = true;

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key" );
        }

        checkKey( geneProduct.getGene() );
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
        if ( localFile == null || ( localFile.getLocalURL() == null && localFile.getRemoteURL() == null ) ) {
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

        ExperimentalFactor ef = factorValue.getExperimentalFactor();

        if ( ef == null )
            throw new IllegalArgumentException( "Must have experimentalfactor on factorvalue to search" );

        Criteria innerQuery = queryObject.createCriteria( "experimentalFactor" );
        addRestrictions( innerQuery, ef );

        if ( factorValue.getValue() != null ) {
            queryObject.add( Restrictions.eq( "value", factorValue.getValue() ) );
        } else if ( factorValue.getCharacteristics().size() > 0 ) {

            /*
             * All the characteristics have to match ones in the result, and the result cannot have any extras. In other
             * words there has to be a one-to-one match between the characteristics.
             */

            // this takes care of the size check
            queryObject.add( Restrictions.sizeEq( "characteristics", factorValue.getCharacteristics().size() ) );

            // now the equivalence.
            Criteria characteristicsCriteria = queryObject.createCriteria( "characteristics" );

            /*
             * Note that this isn't exactly correct, but it should work okay: "If all the characteristics in the
             * candidate are also in the query", along with the size restriction. The only problem would be if the same
             * characteristic were added to an object more than once - so the sizes would be the same, but a
             * characteristic in the query might not show up in the candidate. Multiple entries of the same
             * characteristic shouldn't be allowed, and even if it did happen the chance of a problem is small.... but a
             * formal possibility.
             */
            Disjunction vdj = Restrictions.disjunction();
            for ( Characteristic characteristic : factorValue.getCharacteristics() ) {

                Conjunction c = Restrictions.conjunction();

                if ( characteristic instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) characteristic;
                    if ( vc.getCategoryUri() != null ) {
                        c.add( Restrictions.eq( "categoryUri", vc.getCategoryUri() ) );
                    }
                    if ( vc.getValueUri() != null ) {
                        c.add( Restrictions.eq( "valueUri", vc.getValueUri() ) );
                    }
                }

                if ( characteristic.getValue() != null )
                    c.add( Restrictions.eq( "value", characteristic.getValue() ) );

                if ( characteristic.getCategory() != null )
                    c.add( Restrictions.eq( "category", characteristic.getCategory() ) );

                vdj.add( c );
            }
            characteristicsCriteria.add( vdj );

        } else if ( factorValue.getMeasurement() != null ) {
            queryObject.add( Restrictions.eq( "measurement", factorValue.getMeasurement() ) ); // FIXME this won't
            // really work.
        }

        queryObject.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY );
    }

    /**
     * @param queryObject
     * @param gene
     */
    public static void createQueryObject( Criteria queryObject, Gene gene ) {
        if ( gene.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getId() ) );
        } else {
            addRestrictions( queryObject, gene );
        }

    }

    /**
     * @param queryObject
     * @param geneProduct
     */
    public static void createQueryObject( Criteria queryObject, GeneProduct geneProduct ) {
        if ( geneProduct.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", geneProduct.getId() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getNcbiId() ) ) {
            queryObject.add( Restrictions.eq( "ncbiId", geneProduct.getNcbiId() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getName() ) ) { // NM_XXXXX etc.
            queryObject.add( Restrictions.eq( "name", geneProduct.getName() ) );

            if ( geneProduct.getAccessions() != null && geneProduct.getAccessions().size() > 0 ) {
                Criteria subCriteria = queryObject.createCriteria( "accessions" );
                Disjunction disjunction = Restrictions.disjunction();
                for ( DatabaseEntry databaseEntry : geneProduct.getAccessions() ) {
                    disjunction.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) );
                    // FIXME this should include the ExternalDatabase in the criteria.
                }
                subCriteria.add( disjunction );
            }

            /*
             * Can't use gene. This causes some problems when goldenpath and ncbi don't have the same information about
             * the gene (usually the symbol). Possibly use just the physical location of the gene, not the symbol? Or
             * check aliases?
             */
            // addRestrictions( subCriteria, geneProduct.getGene() );
        }
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
    public static Criteria createQueryObject( Session session, Characteristic ontologyEntry ) {
        Criteria queryObject = session.createCriteria( Characteristic.class );
        checkKey( ontologyEntry );
        addRestrictions( queryObject, ontologyEntry );
        return queryObject;
    }

    /**
     * @param accession
     */
    public static void checkKey( DatabaseEntry accession ) {
        if ( accession.getId() != null ) return;
        if ( StringUtils.isBlank( accession.getAccession() ) ) {
            throw new IllegalArgumentException( accession + " did not have an accession" );
        }
        checkKey( accession.getExternalDatabase() );
    }

    /**
     * @param externalDatabase
     */
    public static void checkKey( ExternalDatabase externalDatabase ) {
        if ( externalDatabase.getId() != null ) return;
        if ( StringUtils.isBlank( externalDatabase.getName() ) ) {
            throw new IllegalArgumentException( externalDatabase + " did not have a name" );
        }
    }

    /**
     * @param queryObject
     * @param databaseEntry
     */
    public static void addRestrictions( Criteria queryObject, DatabaseEntry databaseEntry ) {
        queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) ).createCriteria(
                "externalDatabase" ).add( Restrictions.eq( "name", databaseEntry.getExternalDatabase().getName() ) );
    }

    /**
     * @param queryObject
     * @param quantitationType
     */
    public static void addRestrictions( Criteria queryObject, QuantitationType quantitationType ) {
        queryObject.add( Restrictions.eq( "name", quantitationType.getName() ) );

        queryObject.add( Restrictions.eq( "description", quantitationType.getDescription() ) );

        queryObject.add( Restrictions.eq( "generalType", quantitationType.getGeneralType() ) );

        queryObject.add( Restrictions.eq( "type", quantitationType.getType() ) );

        if ( quantitationType.getIsBackground() != null )
            queryObject.add( Restrictions.eq( "isBackground", quantitationType.getIsBackground() ) );

        if ( quantitationType.getRepresentation() != null )
            queryObject.add( Restrictions.eq( "representation", quantitationType.getRepresentation() ) );

        if ( quantitationType.getScale() != null )
            queryObject.add( Restrictions.eq( "scale", quantitationType.getScale() ) );

        if ( quantitationType.getIsBackgroundSubtracted() != null )
            queryObject.add( Restrictions.eq( "isBackgroundSubtracted", quantitationType.getIsBackgroundSubtracted() ) );

        if ( quantitationType.getIsPreferred() != null )
            queryObject.add( Restrictions.eq( "isPreferred", quantitationType.getIsPreferred() ) );

        if ( quantitationType.getIsNormalized() != null )
            queryObject.add( Restrictions.eq( "isNormalized", quantitationType.getIsNormalized() ) );

    }

}
