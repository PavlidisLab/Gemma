/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein.biomart;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.protein.biomart.model.Ensembl2NcbiValueObject;
import ubic.gemma.loader.util.parser.FileFormatException;
import ubic.gemma.loader.util.parser.LineMapParser;
import ubic.gemma.model.genome.Taxon;

/**
 * Parser for BioMart file. The taxon and the attributes in the file are essential for construction so that the parser
 * is configured to parse the file in the correct fashion for the taxon. The biomart file is taxon spefic which means
 * that the file is generated from bioamrt after providing taxon as a query parameter. It is of the gemma type
 * LineMapParser which means that after parsing a Map of BioMartEnsembleNcbi value objects are returned keyed on ensembl
 * peptide id.
 * <p>
 * Parsing is triggered by calling super class method parse which then calls child method parse oneline.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class BiomartEnsembleNcbiParser extends LineMapParser<String, Ensembl2NcbiValueObject> {
    private static final char FIELD_DELIM = '\t';

    private Map<String, Ensembl2NcbiValueObject> results = null;
    private Taxon taxon = null;
    private String[] bioMartHeaderFields = null;

    /**
     * Class needs to be initialised with taxon and which attributes have been used in query for biomart and thus what
     * columns are in this file.
     * 
     * @param taxon Taxon for the current file being processed
     * @param attributesInFile The attributes that were queried for in Biomart
     */
    public BiomartEnsembleNcbiParser( Taxon taxon, String[] attributesInFile ) {
        this.setTaxon( taxon );
        this.setBioMartFields( attributesInFile );
        results = new HashMap<String, Ensembl2NcbiValueObject>();
    }

    /**
     * Method that returns a particular BioMartEnsembleNcbi based on a peptide id.
     * 
     * @return boolean to indicate whether map contains particular peptide key.
     */
    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    /**
     * Given an array of strings representing the line to parse then create a BioMartEnsembleNcbi value object with some
     * validation. That is if a duplicate record keyed on peptide id is found then that means that it maps to more than
     * one entrez gene id. As such check that the duplicate and currently processed record share the same ensemble gene
     * id as a sanity check. Add the entrez gene to the existing collection of entrez genes.
     * 
     * @param fields Parsed line split on delimiter
     * @return BioMartEnsembleNcbi value object
     * @throws NumberFormatException Parsing a number that is not one
     * @throws FileFormatException Validation than when a duplicate record is found then the peptide id is the same the
     *         ensemble gene id should be the same.
     */
    public Ensembl2NcbiValueObject createBioMartEnsembleNcbi( String[] fields ) throws NumberFormatException,
            FileFormatException {
        Ensembl2NcbiValueObject bioMartEnsembleNcbi = new Ensembl2NcbiValueObject();
        String entrezGene = fields[2].trim();
        String ensemblProteinId = fields[3].trim();

        if ( StringUtils.isBlank( ensemblProteinId ) ) {
            log.warn( "Blank protein id for line: " + StringUtils.join( fields, " " ) );
            return null;
        }

        // if there is no entrezgene skip as that is what we want
        if ( StringUtils.isBlank( entrezGene ) ) {
            log.debug( ensemblProteinId + " has no entrez gene mapping" );
            return null;
        }

        String ensemblGeneID = fields[0].trim();
        bioMartEnsembleNcbi.setNcbiTaxonId( taxon.getNcbiId() ); // FIXME for yeast in biomart, is this 4932 or 559292?
        bioMartEnsembleNcbi.setEnsemblGeneId( ensemblGeneID );
        bioMartEnsembleNcbi.setEnsemblTranscriptId( fields[1] );
        bioMartEnsembleNcbi.setEnsemblPeptideId( ensemblProteinId );

        if ( !bioMartHeaderFields[4].isEmpty() && fields[4] != null ) {
            // only humans should have this field
            bioMartEnsembleNcbi.setHgnc_id( fields[4] );
        }

        // Ensembl ids can map to multiple entrez genes so we maintain a collection of entrezgenes
        if ( !containsKey( ensemblProteinId ) ) {
            bioMartEnsembleNcbi.getEntrezgenes().add( entrezGene );
            results.put( ensemblProteinId, bioMartEnsembleNcbi );
            if ( log.isDebugEnabled() ) log.debug( ensemblProteinId + " has no existing  entrez gene mapping" );
        } else {
            Ensembl2NcbiValueObject bioMartEnsembleNcbiDup = this.get( ensemblProteinId );
            // check that the this duplicate record also is the same for ensembl id
            if ( ensemblGeneID.equals( bioMartEnsembleNcbiDup.getEnsemblGeneId() ) ) {
                this.get( ensemblProteinId ).getEntrezgenes().add( entrezGene );
                if ( log.isDebugEnabled() ) log.debug( ensemblProteinId + "added gene to duplicate  " );
            } else {
                throw new FileFormatException( "A duplicate ensemblProteinId has been found: " + ensemblProteinId
                        + " but it does not match with the exisiting objects gene id " + ensemblGeneID + ", it was "
                        + bioMartEnsembleNcbiDup.getEnsemblGeneId() + ", line was:\n" + StringUtils.join( fields, " " ) );
            }
        }
        return bioMartEnsembleNcbi;
    }

    /**
     * Method that returns a particular BioMartEnsembleNcbi based on a peptide id.
     * 
     * @return BioMartEnsembleNcbi associated with that peptide id.
     */
    @Override
    public Ensembl2NcbiValueObject get( String key ) {
        return results.get( key );
    }

    /**
     * Getter for biomart header file
     * 
     * @return
     */
    public String[] getBioMartFields() {
        return bioMartHeaderFields;
    }

    /**
     * Based on what attributes were set on the original file then calculate how many columns should be in file.
     * 
     * @return Number of columns in file.
     */
    public int getBioMartFieldsPerRow() {
        int attributesSet = 0;
        for ( String attribute : this.getBioMartFields() ) {
            if ( attribute != null && !attribute.isEmpty() ) {
                attributesSet++;
            }
        }
        return attributesSet;
    }

    /**
     * Getter for values in map that is BioMartEnsembleNcbi value objects associated with the parsing of this file
     * 
     * @return Collection of Strings representing the peptide ids in the map
     */
    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }

    /**
     * Returns full map of BioMartEnsembleNcbi objects keyed on peptide id.
     * 
     * @return
     */
    public Map<String, Ensembl2NcbiValueObject> getMap() {
        return results;
    }

    /**
     * Getter for values in map that is BioMartEnsembleNcbi value objects associated with the parsing of this file
     * 
     * @return Collection of BioMartEnsembleNcbi value objects
     */
    @Override
    public Collection<Ensembl2NcbiValueObject> getResults() {
        return results.values();
    }

    /**
     * Method to parse one biomart line, note that there is a many to many relationship between ensemble ids and entrez
     * gene ids.
     * 
     * @return BioMartEnsembleNcbi Value object representing the line parsed
     */
    @Override
    public Ensembl2NcbiValueObject parseOneLine( String line ) {

        int bioMartFieldsPerRow = this.getBioMartFieldsPerRow();
        // header line from the bioMart headers then ignore it
        if ( line.startsWith( this.bioMartHeaderFields[0] ) || line.isEmpty() ) {
            return null;
        }
        // split the line into the attributes
        String[] fields = StringUtils.splitPreserveAllTokens( line, FIELD_DELIM );
        // validate that correct format
        if ( fields.length != bioMartFieldsPerRow ) {
            /*
             * I think we should just continue on. Previous behaviour was to throw an exception.
             */
            return null;
        }
        // create the object
        try {
            return createBioMartEnsembleNcbi( fields );

        } catch ( NumberFormatException e ) {
            throw new FileFormatException( e );
        } catch ( FileFormatException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Setter for the biomart header files
     * 
     * @param bioMartFields
     */
    public void setBioMartFields( String[] bioMartFields ) {
        this.bioMartHeaderFields = bioMartFields;
    }

    /**
     * Setter for the taxon that this file is for
     * 
     * @param taxon
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

}
