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
package ubic.gemma.core.loader.util.biomart;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import ubic.gemma.core.loader.util.biomart.BiomartEnsemblNcbiObjectGenerator;
import ubic.gemma.core.loader.util.biomart.Ensembl2NcbiValueObject;
import ubic.gemma.model.genome.Taxon;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Class to test BioMartEnsemblNcbiObjectGeneration. Simple class but there is some logic connected to the many to many
 * relationship between ensembl genes and entrez genes and map manipulation. Using awk on the file validate the numbers.
 *
 * @author ldonnison
 */
public class BioMartEnsemblNcbiObjectGeneratorTest {
    private BiomartEnsemblNcbiObjectGenerator biomartEnsemblNcbiObjectGenerator = null;
    private Collection<Taxon> taxa = null;
    private File taxonBiomartFile = null;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        biomartEnsemblNcbiObjectGenerator = new BiomartEnsemblNcbiObjectGenerator();
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setIsGenesUsable( true );
        taxon.setNcbiId( 10090 );
        taxon.setScientificName( "Mus musculus" );
        taxa = new ArrayList<>();
        taxa.add( taxon );

        String fileNameBiomartmouse = "/data/loader/protein/biomart/biomartmmusculusShort.txt";
        Resource resource = new ClassPathResource( fileNameBiomartmouse );
        taxonBiomartFile = resource.getFile();
    }

    /**
     * Tests that given a taxon biomart file a BioMartEnsembleNcbi can be returned and that the genes are correctly
     * mapped. Could be done through parser but thought do a quick test here. AWk commands are given to help give the
     * counts to check validity of the numbers
     */
    @Test
    public void testGenerate() {

        try {
            biomartEnsemblNcbiObjectGenerator.setBioMartFileName( taxonBiomartFile );
            Map<String, Ensembl2NcbiValueObject> map = biomartEnsemblNcbiObjectGenerator.generate( taxa );

            long counterEnsemblToManyGeneids = 0;
            long counterEnsemblToOneGeneids = 0;
            long counterNumberGenes = 0;
            long countHowManyNoGenes = 0;
            // awk -F'\t' 'length($3)>1' test.txt | awk -F'\t' '{print $4}' | uniq |sort | wc -l -1
            // there are 510 records which have one or more gene mapping
            assertEquals( 510, map.keySet().size() );
            for ( Ensembl2NcbiValueObject biomart : map.values() ) {
                // count how many have duplicate genes
                if ( biomart.getEntrezgenes().size() > 1 ) {
                    counterEnsemblToManyGeneids++;
                }
                // count how many 1
                else if ( biomart.getEntrezgenes().size() == 1 ) {
                    counterEnsemblToOneGeneids++;
                } // how many 0- should be null
                else {
                    countHowManyNoGenes++;
                }

                // count how many genes in total
                for ( String geneE : biomart.getEntrezgenes() ) {
                    if ( !geneE.isEmpty() ) {
                        counterNumberGenes++;
                    }
                }

            }
            // awk -F'\t' 'length($3)>1' test.txt | awk -F"\t" '{if (a[$4]) { print $4 } a[$4] = $0}' | sort | uniq | wc
            // -l
            assertEquals( 75, counterEnsemblToManyGeneids );
            // awk -F'\t' 'length($3)>1' test.txt | awk -F"\t" '{if (a[$4]==null) { print $4 } a[$4] = $0}' | sort |
            // uniq | wc -l -75
            assertEquals( 435, counterEnsemblToOneGeneids );
            // there should be none with no genes as they are filtered out
            assertEquals( 0, countHowManyNoGenes );
            // test the file awk -F'\t' 'length($3)>1' test.txt | awk -F'\t' '{print $3}' | uniq |sort | wc -l
            assertEquals( 638, counterNumberGenes );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }

    }

}
