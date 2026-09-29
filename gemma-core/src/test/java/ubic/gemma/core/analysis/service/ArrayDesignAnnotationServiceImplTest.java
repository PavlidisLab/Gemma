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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.association.Gene2GOAssociationReadService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    @Mock
    private ArrayDesignService arrayDesignService;

    @Mock
    private CompositeSequenceService compositeSequenceService;

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

    /**
     * The previous annotation file was deleted before the new one was written in place, so a failure midway left a
     * truncated file that was still valid gzip, and it was served as the platform's annotations.
     */
    @Test
    public void create_whenGenerationFails_keepsThePreviousFile( @TempDir Path annotDataDir ) throws Exception {
        ReflectionTestUtils.setField( service, "annotDataDir", annotDataDir );
        ReflectionTestUtils.setField( service, "entityUrlBuilder", new EntityUrlBuilder( "https://gemma.msl.ubc.ca" ) );
        ReflectionTestUtils.setField( service, "buildInfo", mock( BuildInfo.class ) );

        Taxon taxon = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "GPL0", taxon );
        ad.setId( 1L );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "probe1", ad );
        ad.getCompositeSequences().add( cs );
        Gene gene = Gene.Factory.newInstance();
        gene.setId( 1L );
        gene.setOfficialSymbol( "FOS" );
        GeneProduct geneProduct = GeneProduct.Factory.newInstance();
        geneProduct.setGene( gene );
        BlatAssociation association = BlatAssociation.Factory.newInstance();
        association.setGeneProduct( geneProduct );
        when( arrayDesignService.thaw( ad ) ).thenReturn( ad );
        when( compositeSequenceService.getGenesWithSpecificity( ad.getCompositeSequences() ) )
                .thenReturn( Collections.singletonMap( cs, Collections.singletonList( association ) ) );
        // fails after the file header is written
        when( gene2GOAssociationService.findByGenes( any() ) ).thenThrow( new IllegalStateException( "GO lookup failed" ) );

        Path annotationFile = annotDataDir.resolve( "GPL0" + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );
        try ( Writer w = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( annotationFile ) ), StandardCharsets.UTF_8 ) ) {
            w.write( "previous annotations\n" );
        }

        assertThatThrownBy( () -> service.create( ad, false, true ) ).hasMessage( "GO lookup failed" );

        try ( InputStream is = new GZIPInputStream( Files.newInputStream( annotationFile ) ) ) {
            assertThat( new String( IOUtils.toByteArray( is ), StandardCharsets.UTF_8 ) ).isEqualTo( "previous annotations\n" );
        }
        try ( Stream<Path> files = Files.list( annotDataDir ) ) {
            assertThat( files ).containsExactly( annotationFile );
        }
    }

    private Characteristic goCharacteristic( String valueUri ) {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setValueUri( valueUri );
        return c;
    }
}
