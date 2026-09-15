/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.analysis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationReadService;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ArrayDesignAnnotationServiceImplTest {

    private static final String GO_URI = "http://purl.obolibrary.org/obo/GO_0005515";
    /**
     * GENE2GO on production holds several hundred rows pointing at this EFO term ("pregnancy"), which the GO
     * ontology obviously cannot resolve.
     */
    private static final String NON_GO_URI = "http://www.ebi.ac.uk/efo/EFO_0002950";

    @Mock
    private Gene2GOAssociationReadService gene2GOAssociationService;

    @Mock
    private GeneOntologyService goService;

    @InjectMocks
    private ArrayDesignAnnotationServiceImpl service;

    /**
     * A GO association whose URI is not in the loaded ontology makes {@link GeneOntologyService#getTerm(String)}
     * return null. Collecting that null used to hand it straight to
     * {@code GeneOntologyUtils.asRegularGoId(OntologyTerm)} (and, for the LONG output type, to
     * {@code getAllParents}), which dereference every element — so a single unresolvable term aborted the whole
     * annotation file with a bare NullPointerException.
     */
    @Test
    public void generateAnnotationFile_whenATermIsNotInTheLoadedOntology_writesTheRemainingTerms() throws Exception {
        Gene gene = Gene.Factory.newInstance();
        gene.setId( 1L );
        gene.setOfficialSymbol( "FOS" );
        gene.setOfficialName( "Fos proto-oncogene" );
        gene.setNcbiGeneId( 2353 );

        Collection<Gene> genes = Collections.singleton( gene );
        when( gene2GOAssociationService.findByGenes( genes ) )
                .thenReturn( Collections.singletonMap( gene, Arrays.asList( goCharacteristic( GO_URI ), goCharacteristic( NON_GO_URI ) ) ) );

        OntologyTerm term = org.mockito.Mockito.mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( GO_URI );
        when( goService.getTerm( GO_URI ) ).thenReturn( term );
        when( goService.getTerm( NON_GO_URI ) ).thenReturn( null );

        StringWriter writer = new StringWriter();
        assertThat( service.generateAnnotationFile( writer, genes, true ) ).isEqualTo( 1 );

        String[] fields = writer.toString().split( "\t" );
        assertThat( fields[3] ).isEqualTo( "GO:0005515" );
    }

    private Characteristic goCharacteristic( String valueUri ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( valueUri );
        return c;
    }
}
