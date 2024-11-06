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
package ubic.gemma.persistence.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.expression.arrayDesign.AlternateName;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;
import java.util.HashSet;

/**
 * Methods to test business-key-related issues on objects. The 'checkValidKey' methods can be used to check whether an
 * object has the required business key values filled in. An exception is thrown if they don't.
 * This class contains some important code that determines our rules for how entities are detected as being the same as
 * another (in queries from the database; this is on top of basic 'equals', but should be compatible).
 *
 * @author pavlidis
 */
public class BusinessKey {

    private static final Log log = LogFactory.getLog( BusinessKey.class.getName() );

    public static void addRestrictions( Criteria queryObject, ArrayDesign arrayDesign ) {

        /*
         * Test whether ANY of the associated external references match any of the given external references.
         */
        if ( arrayDesign.getPrimaryTaxon() != null && arrayDesign.getPrimaryTaxon().getId() != null ) {
            queryObject.add( Restrictions.eq( "primaryTaxon", arrayDesign.getPrimaryTaxon() ) );
        }

        if ( arrayDesign.getExternalReferences().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "externalReferences" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( DatabaseEntry databaseEntry : arrayDesign.getExternalReferences() ) {
                disjunction.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) );
                // FIXME this should include the ExternalDatabase in the criteria.
            }
            externalRef.add( disjunction );
            return;
        } else if ( arrayDesign.getAlternateNames().size() != 0 ) {
            Criteria externalRef = queryObject.createCriteria( "alternateNames" );
            Disjunction disjunction = Restrictions.disjunction();
            for ( AlternateName alternateName : arrayDesign.getAlternateNames() ) {
                disjunction.add( Restrictions.eq( "name", alternateName.getName() ) );
            }
            externalRef.add( disjunction );
            return;
        } else if ( arrayDesign.getShortName() != null ) {
            // this might not be such a good idea, because we can edit the short name.
            queryObject.add( Restrictions.eq( "shortName", arrayDesign.getShortName() ) );
        } else {
            BusinessKey.addNameRestriction( queryObject, arrayDesign );
        }

        if ( arrayDesign.getDesignProvider() != null && StringUtils
                .isNotBlank( arrayDesign.getDesignProvider().getName() ) ) {
            queryObject.createCriteria( "designProvider" )
                    .add( Restrictions.eq( "name", arrayDesign.getDesignProvider().getName() ) );
        }

    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addRestrictions( Criteria queryObject, BioAssay bioAssay ) {
        if ( bioAssay.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", bioAssay.getId() ) );
        } else if ( bioAssay.getAccession() != null ) {
            BusinessKey.attachCriteria( queryObject, bioAssay.getAccession(), "accession" );
        }
        queryObject.add( Restrictions.eq( "name", bioAssay.getName() ) );
    }

    public static void addRestrictions( Criteria queryObject, BioMaterial bioMaterial ) {

        if ( bioMaterial.getName() != null ) {
            queryObject.add( Restrictions.eq( "name", bioMaterial.getName() ) );
        }

        if ( bioMaterial.getExternalAccession() != null ) {
            // this is not completely foolproof.
            queryObject.createCriteria( "externalAccession" )
                    .add( Restrictions.eq( "accession", bioMaterial.getExternalAccession().getAccession() ) );
        } else if ( StringUtils.isNotBlank( bioMaterial.getDescription() ) ) {
            // The description is generally only filled in by Gemma, and contains the experiment short name.
            queryObject.add( Restrictions.eq( "description", bioMaterial.getDescription() ) );
        }

        if ( bioMaterial.getSourceTaxon() != null ) {
            queryObject.add( Restrictions.eq( "sourceTaxon", bioMaterial.getSourceTaxon() ) );
        }

    }

    /**
     * Note: The finder has to do the additional checking for equality of sequence and/or database entry - we don't know
     * until we get the sequences. Due to the following issues:
     * <ul>
     * <li>Sometimes the sequence in the database lacks the DatabaseEntry
     * <li>Sometimes the old entry lacks the actual sequence (ATCG..)
     * </ul>
     * This means that we can't use those criteria up front. Instead we match by name and taxon. If those match, the
     * caller has to sort through possible multiple results to find the correct one.
     *
     * @param bioSequence bio sequence
     * @param queryObject query object
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addRestrictions( Criteria queryObject, BioSequence bioSequence ) {

        if ( bioSequence.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", bioSequence.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( bioSequence.getName() ) ) {
            BusinessKey.addNameRestriction( queryObject, bioSequence );
        }

        BusinessKey.attachCriteria( queryObject, bioSequence.getTaxon(), "taxon" );

    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addRestrictions( Criteria queryObject, Characteristic characteristic ) {

        if ( characteristic.getCategoryUri() != null ) {
            queryObject.add( Restrictions.eq( "categoryUri", characteristic.getCategoryUri() ) );
        } else if ( characteristic.getCategory() != null ) {
            queryObject.add( Restrictions.eq( "category", characteristic.getCategory() ) );
        }

        if ( StringUtils.isNotBlank( characteristic.getValueUri() ) ) {
            queryObject.add( Restrictions.eq( "valueUri", characteristic.getValueUri() ) );
        } else {
            assert characteristic.getValue() != null;
            queryObject.add( Restrictions.eq( "value", characteristic.getValue() ) );
        }

    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
    public static void addRestrictions( Criteria queryObject, Chromosome chromosome ) {
        queryObject.add( Restrictions.eq( "name", chromosome.getName() ) );
        BusinessKey.attachCriteria( queryObject, chromosome.getTaxon(), "taxon" );
        if ( chromosome.getAssemblyDatabase() != null ) {
            BusinessKey.attachCriteria( queryObject, chromosome.getAssemblyDatabase() );
        }
        if ( chromosome.getSequence() != null ) {
            BusinessKey.attachCriteria( queryObject, chromosome.getSequence(), "sequence" );
        }
    }

    public static void addRestrictions( Criteria queryObject, Contact contact ) {

        if ( contact instanceof User ) {
            queryObject.add( Restrictions.eq( "userName", ( ( User ) contact ).getUserName() ) );
            return;
        }

        if ( StringUtils.isNotBlank( contact.getEmail() ) ) {
            // email is NOT unique.
            queryObject.add( Restrictions.eq( "email", contact.getEmail() ) );
        }

        if ( StringUtils.isNotBlank( contact.getName() ) )
            queryObject.add( Restrictions.eq( "name", contact.getName() ) );

    }

    public static void addRestrictions( Criteria queryObject, ExperimentalFactor experimentalFactor ) {

        if ( experimentalFactor.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", experimentalFactor.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( experimentalFactor.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", experimentalFactor.getName() ) );
        }

        if ( experimentalFactor.getCategory() != null ) {
            BusinessKey.attachCriteria( queryObject, experimentalFactor.getCategory(), "category" );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addRestrictions( Criteria queryObject, Gene gene, boolean stricter ) {
        if ( gene.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getId() ) );
        } else if ( gene.getNcbiGeneId() != null ) {
            /*
             * These are unambiguous identifiers.
             */

            if ( StringUtils.isNotBlank( gene.getPreviousNcbiId() ) ) {
                Collection<Integer> ncbiIds = new HashSet<>();
                ncbiIds.add( gene.getNcbiGeneId() );
                for ( String previousId : StringUtils.split( gene.getPreviousNcbiId(), "," ) ) {
                    /*
                     * Check to see if the new gene used to use an id that is in the system. This is needed to deal with
                     * the case where NCBI changes a gene id (which is common, from gene_history).
                     */
                    try {
                        ncbiIds.add( Integer.parseInt( previousId ) );
                    } catch ( NumberFormatException e ) {
                        BusinessKey.log.warn( "Previous Ncbi id wasn't parseable to an int: " + previousId );
                    }
                }
                queryObject.add( Restrictions.in( "ncbiGeneId", ncbiIds ) );

            } else {
                queryObject.add( Restrictions.eq( "ncbiGeneId", gene.getNcbiGeneId() ) );
            }

        } else if ( StringUtils.isNotBlank( gene.getOfficialSymbol() ) ) {
            /*
             * Second choice, but not unambiguous even within a taxon unless we know the physical location
             */
            queryObject.add( Restrictions.eq( "officialSymbol", gene.getOfficialSymbol() ) );

            BusinessKey.attachCriteria( queryObject, gene.getTaxon(), "taxon" );

            if ( stricter ) {
                // Need either the official name AND the location to be unambiguous. But if we are already restricted on
                // some other characteristic such as the gene, this causes too many false negatives. Typical case: NCBI
                // vs. GoldenPath.
                if ( StringUtils.isNotBlank( gene.getOfficialName() ) ) {
                    queryObject.add( Restrictions.eq( "officialName", gene.getOfficialName() ) );
                }

                if ( gene.getPhysicalLocation() != null ) {
                    BusinessKey.attachCriteria( queryObject, gene.getPhysicalLocation(), "physicalLocation" );
                }
            }
        } else {
            throw new IllegalArgumentException( "No valid key " + gene );
        }
    }

    public static void addRestrictions( Criteria queryObject, Gene2GOAssociation gene2GOAssociation ) {
        BusinessKey.attachCriteria( queryObject, gene2GOAssociation.getGene(), "gene" );
        BusinessKey.attachCriteria( queryObject, gene2GOAssociation.getOntologyEntry() );
    }

    public static void addRestrictions( Criteria queryObject, Taxon taxon ) {
        BusinessKey.checkValidKey( taxon );
        BusinessKey.attachCriteria( queryObject, taxon );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void addRestrictions( DetachedCriteria queryObject, DatabaseEntry databaseEntry ) {
        queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) )
                .createCriteria( "externalDatabase" )
                .add( Restrictions.eq( "name", databaseEntry.getExternalDatabase().getName() ) );
    }

    /**
     * Restricts the query to the provided BioSequence.
     *
     * @param bioSequence  The object used to create the criteria
     * @param propertyName Often this will be 'bioSequence'
     * @param queryObject  query object
     */
    public static void attachCriteria( Criteria queryObject, BioSequence bioSequence, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addRestrictions( innerQuery, bioSequence );
    }

    /**
     * Restricts the query to the provided OntologyEntry.
     *
     * @param ontologyEntry The object used to create the criteria
     * @param propertyName  Often this will be 'ontologyEntry'
     * @param queryObject   query object
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachCriteria( Criteria queryObject, Characteristic ontologyEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addRestrictions( innerQuery, ontologyEntry );
    }

    /**
     * Restricts query to the given DatabaseEntry association
     *
     * @param databaseEntry to match
     * @param propertyName  often "accession"
     * @param queryObject   query object
     */
    public static void attachCriteria( Criteria queryObject, DatabaseEntry databaseEntry, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.attachCriteria( innerQuery, databaseEntry );
    }

    /**
     * Restricts the query to the provided Gene.
     *
     * @param queryObject  query object
     * @param gene         gene
     * @param propertyName property name
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachCriteria( Criteria queryObject, Gene gene, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.addRestrictions( innerQuery, gene, true );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachCriteria( Criteria queryObject, PhysicalLocation physicalLocation, String attributeName ) {
        Criteria nestedCriteria = queryObject.createCriteria( attributeName );

        if ( physicalLocation.getChromosome() == null ) {
            throw new IllegalArgumentException();
        }

        if ( physicalLocation.getChromosome().getId() != null ) {
            nestedCriteria.createCriteria( "chromosome" )
                    .add( Restrictions.eq( "id", physicalLocation.getChromosome().getId() ) );
        } else {
            // FIXME should add taxon to this.
            nestedCriteria.createCriteria( "chromosome" )
                    .add( Restrictions.eq( "name", physicalLocation.getChromosome().getName() ) );
        }

        if ( physicalLocation.getNucleotide() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotide", physicalLocation.getNucleotide() ) );

        if ( physicalLocation.getNucleotideLength() != null )
            nestedCriteria.add( Restrictions.eq( "nucleotideLength", physicalLocation.getNucleotideLength() ) );

    }

    /**
     * Restricts query to the given Taxon.
     *
     * @param propertyName often "taxon"
     * @param queryObject  query object
     * @param taxon        taxon
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void attachCriteria( Criteria queryObject, Taxon taxon, String propertyName ) {
        Criteria innerQuery = queryObject.createCriteria( propertyName );
        BusinessKey.attachCriteria( innerQuery, taxon );
    }

    @SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
    public static void attachCriteria( DetachedCriteria queryObject, DatabaseEntry databaseEntry,
            String attributeName ) {
        DetachedCriteria externalRef = queryObject.createCriteria( attributeName );
        BusinessKey.addRestrictions( externalRef, databaseEntry );
    }

    public static void checkKey( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getPubAccession() == null
                || bibliographicReference.getPubAccession().getAccession() == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference was null or had no accession : " + bibliographicReference );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkKey( Characteristic ontologyEntry ) {
        if ( ontologyEntry.getValue() == null )
            throw new IllegalArgumentException();
    }

    public static void checkKey( Contact contact ) {
        if ( contact == null || ( StringUtils.isBlank( contact.getName() ) && StringUtils
                .isBlank( contact.getEmail() ) ) ) {
            throw new IllegalArgumentException( "Contact must have at least some information filled in!" );
        }
    }

    public static void checkKey( DatabaseEntry accession ) {
        if ( accession.getId() != null )
            return;
        if ( StringUtils.isBlank( accession.getAccession() ) ) {
            throw new IllegalArgumentException( accession + " did not have an accession" );
        }
        BusinessKey.checkKey( accession.getExternalDatabase() );
    }

    public static void checkKey( DesignElementDataVector designElementDataVector ) {
        if ( designElementDataVector == null || designElementDataVector.getDesignElement() == null
                || designElementDataVector.getExpressionExperiment() == null ) {
            throw new IllegalArgumentException(
                    "DesignElementDataVector did not have complete business key " + designElementDataVector );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkKey( ExternalDatabase externalDatabase ) {
        if ( externalDatabase.getId() != null )
            return;
        if ( StringUtils.isBlank( externalDatabase.getName() ) ) {
            throw new IllegalArgumentException( externalDatabase + " did not have a name" );
        }
    }

    public static void checkKey( FactorValue factorValue ) {
        if ( factorValue.getValue() == null && factorValue.getMeasurement() == null
                && factorValue.getCharacteristics().size() == 0 ) {
            throw new IllegalArgumentException(
                    "FactorValue must have a value (or associated measurement or characteristics)." );
        }
    }

    public static void checkKey( Gene gene ) {
        if ( gene == null )
            throw new IllegalArgumentException( "Gene cannot be null" );
        if ( ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getPhysicalLocation() == null
                && ( gene.getProducts() == null || gene.getProducts().isEmpty() ) ) && gene.getNcbiGeneId() == null ) {
            throw new IllegalArgumentException( "No valid key for " + gene
                    + ": Gene must have official symbol and name with taxon + physical location or gene products, or ncbiId" );
        }
    }

    public static void checkKey( User user ) {
        if ( user == null || StringUtils.isBlank( user.getUserName() ) ) {
            throw new IllegalArgumentException( "User was null or had no userName defined" );
        }
    }

    public static void checkValidKey( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || ( StringUtils.isBlank( arrayDesign.getName() ) && StringUtils
                .isBlank( arrayDesign.getShortName() ) && arrayDesign.getExternalReferences().size() == 0 ) ) {
            throw new IllegalArgumentException( arrayDesign + " did not have a valid key" );
        }
    }

    public static void checkValidKey( BioSequence bioSequence ) {
        if ( bioSequence == null || bioSequence.getTaxon() == null || StringUtils.isBlank( bioSequence.getName() ) ) {
            throw new IllegalArgumentException( bioSequence + " did not have a valid key" );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static void checkValidKey( Chromosome chromosome ) {
        if ( StringUtils.isBlank( chromosome.getName() ) ) {
            throw new IllegalArgumentException( "Chromosome did not have a valid key" );
        }
        BusinessKey.checkValidKey( chromosome.getTaxon() );
    }

    public static void checkValidKey( DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null || databaseEntry.getAccession() == null
                || databaseEntry.getExternalDatabase() == null ) {
            throw new IllegalArgumentException( "DatabaseEntry does not have valid key" );
        }
    }

    public static void checkValidKey( ExperimentalFactor experimentalFactor ) {
        if ( StringUtils.isBlank( experimentalFactor.getName() ) && experimentalFactor.getCategory() == null ) {
            throw new IllegalArgumentException( "Experimental factor must have name or category" );
        }
    }

    @SuppressWarnings("WeakerAccess") // Consistency
    public static void checkValidKey( Gene gene ) {
        if ( gene == null || ( gene.getNcbiGeneId() == null && ( StringUtils.isBlank( gene.getOfficialSymbol() )
                || gene.getTaxon() == null || StringUtils.isBlank( gene.getOfficialName() ) ) ) ) {
            throw new IllegalArgumentException(
                    "Gene does not have valid key (needs NCBI numeric id or Official Symbol + Official Name + Taxon" );
        }
    }

    public static void checkValidKey( Gene2GOAssociation gene2GOAssociation ) {
        BusinessKey.checkValidKey( gene2GOAssociation.getGene() );
    }

    public static void checkValidKey( GeneProduct geneProduct ) {
        if ( geneProduct.getId() != null )
            return;

        boolean ok = false;

        if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) || StringUtils.isNotBlank( geneProduct.getName() ) )
            ok = true;

        if ( !ok ) {
            throw new IllegalArgumentException( "GeneProduct did not have a valid key - requires name or NCBI GI" );
        }

        if ( geneProduct.getGene() != null ) {
            BusinessKey.checkKey( geneProduct.getGene() );
        }

    }

    public static void checkValidKey( Taxon taxon ) {
        if ( taxon == null || ( taxon.getNcbiId() == null && StringUtils.isBlank( taxon.getCommonName() ) && StringUtils
                .isBlank( taxon.getScientificName() ) ) ) {
            throw new IllegalArgumentException( "Taxon " + taxon + " did not have a valid key" );
        }
    }

    public static void checkValidKey( Unit unit ) {
        if ( unit == null || StringUtils.isBlank( unit.getUnitNameCV() ) ) {
            throw new IllegalArgumentException( unit + " did not have a valid key" );
        }
    }

    public static void createQueryObject( Criteria queryObject, FactorValue factorValue ) {

        ExperimentalFactor ef = factorValue.getExperimentalFactor();

        if ( ef == null )
            throw new IllegalArgumentException( "Must have experimentalfactor on factorvalue to search" );

        Criteria innerQuery = queryObject.createCriteria( "experimentalFactor" );
        BusinessKey.addRestrictions( innerQuery, ef );

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

                if ( StringUtils.isNotBlank( characteristic.getCategoryUri() ) ) {
                    c.add( Restrictions.eq( "categoryUri", characteristic.getCategoryUri() ) );
                }
                if ( StringUtils.isNotBlank( characteristic.getValueUri() ) ) {
                    c.add( Restrictions.eq( "valueUri", characteristic.getValueUri() ) );
                }

                if ( StringUtils.isNotBlank( characteristic.getValue() ) )
                    c.add( Restrictions.eq( "value", characteristic.getValue() ) );

                if ( StringUtils.isNotBlank( characteristic.getCategory() ) )
                    c.add( Restrictions.eq( "category", characteristic.getCategory() ) );

                vdj.add( c );
            }
            characteristicsCriteria.add( vdj );

        } else if ( factorValue.getMeasurement() != null ) {
            queryObject.add( Restrictions.eq( "measurement", factorValue.getMeasurement() ) );
        }

        queryObject.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );
    }

    public static void createQueryObject( Criteria queryObject, Gene gene ) {
        if ( gene.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", gene.getId() ) );
        } else {
            BusinessKey.addRestrictions( queryObject, gene, true );
        }

    }

    public static void createQueryObject( Criteria queryObject, GeneProduct geneProduct ) {
        if ( geneProduct.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", geneProduct.getId() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getNcbiGi() ) ) {
            queryObject.add( Restrictions.eq( "ncbiGi", geneProduct.getNcbiGi() ) );
        } else if ( StringUtils.isNotBlank( geneProduct.getName() ) ) { // NM_XXXXX etc.
            queryObject.add( Restrictions.eq( "name", geneProduct.getName() ) );
            /*
             * This can cause some problems when golden path and NCBI don't have the same information about the gene
             */
            if ( geneProduct.getGene() != null ) {
                Criteria geneCrits = queryObject.createCriteria( "gene" );
                BusinessKey.addRestrictions( geneCrits, geneProduct.getGene(), false );
            }
        }
    }

    @SuppressWarnings("unused") // Possible external use
    public static Criteria createQueryObject( Session session, ArrayDesign arrayDesign ) {
        Criteria queryObject = session.createCriteria( ArrayDesign.class );
        BusinessKey.addRestrictions( queryObject, arrayDesign );
        return queryObject;
    }

    public static Criteria createQueryObject( Session session, BioAssay bioAssay ) {
        Criteria queryObject = session.createCriteria( BioAssay.class );
        BusinessKey.checkKey( bioAssay );
        BusinessKey.addRestrictions( queryObject, bioAssay );
        return queryObject;
    }

    public static Criteria createQueryObject( Session session, BioSequence bioSequence ) {
        Criteria queryObject = session.createCriteria( BioSequence.class );
        BusinessKey.addRestrictions( queryObject, bioSequence );
        return queryObject;
    }

    @SuppressWarnings("unused") // Possible external use
    public static Criteria createQueryObject( Session session, Characteristic ontologyEntry ) {
        Criteria queryObject = session.createCriteria( Characteristic.class );
        BusinessKey.checkKey( ontologyEntry );
        BusinessKey.addRestrictions( queryObject, ontologyEntry );
        return queryObject;
    }

    public static Criteria createQueryObject( Session session, Unit unit ) {
        Criteria queryObject = session.createCriteria( Unit.class );

        if ( unit.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", unit.getId() ) );
        } else if ( unit.getUnitNameCV() != null ) {
            queryObject.add( Restrictions.eq( "unitNameCV", unit.getUnitNameCV() ) );
        }

        return queryObject;
    }

    private static void addNameRestriction( Criteria queryObject, Describable describable ) {
        if ( describable.getName() != null )
            queryObject.add( Restrictions.eq( "name", describable.getName() ) );
    }

    private static void addRestrictions( Criteria queryobject, ExternalDatabase assemblyDatabase ) {
        if ( assemblyDatabase.getId() != null ) {
            queryobject.add( Restrictions.eq( "id", assemblyDatabase.getId() ) );
            return;
        }

        if ( StringUtils.isNotBlank( assemblyDatabase.getName() ) ) {
            BusinessKey.addNameRestriction( queryobject, assemblyDatabase );
        }

    }

    private static void attachCriteria( Criteria queryObject, DatabaseEntry databaseEntry ) {

        queryObject.add( Restrictions.eq( "accession", databaseEntry.getAccession() ) )
                .createCriteria( "externalDatabase" )
                .add( Restrictions.eq( "name", databaseEntry.getExternalDatabase().getName() ) );

    }

    private static void attachCriteria( Criteria queryObject, ExternalDatabase assemblyDatabase ) {
        Criteria innerQuery = queryObject.createCriteria( "assemblyDatabase" );
        BusinessKey.addRestrictions( innerQuery, assemblyDatabase );
    }

    private static void attachCriteria( Criteria queryObject, Taxon taxon ) {
        if ( taxon == null )
            throw new IllegalArgumentException( "Taxon was null" );
        if ( taxon.getId() != null ) {
            queryObject.add( Restrictions.eq( "id", taxon.getId() ) );
        } else if ( taxon.getNcbiId() != null ) {
            Disjunction disjunction = Restrictions.disjunction();
            disjunction.add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
            disjunction.add( Restrictions.eq( "secondaryNcbiId", taxon.getNcbiId() ) );
            if ( taxon.getSecondaryNcbiId() != null ) {
                disjunction.add( Restrictions.eq( "ncbiId", taxon.getSecondaryNcbiId() ) );
            }
            queryObject.add( disjunction );

        } else if ( StringUtils.isNotBlank( taxon.getScientificName() ) ) {
            queryObject.add( Restrictions.eq( "scientificName", taxon.getScientificName() ) );
        } else if ( StringUtils.isNotBlank( taxon.getCommonName() ) ) {
            queryObject.add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
        }
    }

    private static void attachCriteria( Criteria queryObject, Characteristic ontologyEntry ) {
        Criteria innerQuery = queryObject.createCriteria( "ontologyEntry" );
        BusinessKey.addRestrictions( innerQuery, ontologyEntry );
    }

    private static void checkKey( BioAssay bioAssay ) {
        if ( bioAssay.getId() == null && bioAssay.getAccession() == null ) {
            throw new IllegalArgumentException( "Bioassay must have id or accession" );
        }

    }

    public static void checkKey( ExpressionExperimentSubSet entity ) {
        if ( entity.getBioAssays().isEmpty() ) {
            throw new IllegalArgumentException( "Subset must have bioassays" );
        }

        if ( entity.getSourceExperiment() == null || entity.getSourceExperiment().getId() == null ) {
            throw new IllegalArgumentException( "Subset must have persistent sourceExperiment" );
        }

        for ( BioAssay ba : entity.getBioAssays() ) {
            if ( ba.getId() == null ) {
                throw new IllegalArgumentException( "Subset must be made from persistent bioassays." );
            }
        }
    }

    public static void createQueryObject( Criteria queryObject, ExpressionExperimentSubSet entity ) {
        /*
         * Note that we don't match on name.
         */

        queryObject.add( Restrictions.eq( "sourceExperiment", entity.getSourceExperiment() ) );

        queryObject.add( Restrictions.sizeEq( "bioAssays", entity.getBioAssays().size() ) );

        queryObject.createCriteria( "bioAssays" )
                .add( Restrictions.in( "id", IdentifiableUtils.getIds( entity.getBioAssays() ) ) );

    }
}
