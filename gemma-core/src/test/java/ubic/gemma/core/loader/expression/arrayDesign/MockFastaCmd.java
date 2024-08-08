package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.RandomStringUtils;
import ubic.gemma.core.loader.genome.FastaCmd;
import ubic.gemma.core.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.PolymerType;

import java.util.Collection;
import java.util.HashSet;

class MockFastaCmd implements FastaCmd {

    private final Taxon taxon;

    public MockFastaCmd( Taxon t ) {
        this.taxon = t;
    }

    @Override
    public BioSequence getByAccession( String accession, String database ) {
        return this.getSingle( accession, database, null );
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database ) {
        return this.getSingle( identifier, database, null );
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database ) {
        return this.getMultiple( accessions, database, null );
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database ) {
        return this.getMultiple( identifiers, database, null );
    }

    @Override
    public BioSequence getByAccession( String accession, String database, String blastHome ) {
        return this.getSingle( accession, database, blastHome );
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database, String blastHome ) {
        return this.getSingle( identifier, database, blastHome );
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database,
            String blastHome ) {
        return this.getMultiple( accessions, database, blastHome );
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database,
            String blastHome ) {
        return this.getMultiple( identifiers, database, blastHome );
    }

    @SuppressWarnings("unused")
    private BioSequence getSingle( Object accession, String database, String blastHome ) {
        return this.makeSequence( accession );
    }

    @SuppressWarnings("unused")
    private Collection<BioSequence> getMultiple( Collection<?> accessions, String database, String blastHome ) {
        Collection<BioSequence> results = new HashSet<>();
        for ( Object object : accessions ) {
            BioSequence result = this.makeSequence( object );

            results.add( result );
        }
        return results;
    }

    private BioSequence makeSequence( Object object ) {
        BioSequence result = BioSequence.Factory.newInstance( taxon );
        result.setName( object.toString() );
        result.setLength( 100L );
        result.setPolymerType( PolymerType.DNA );
        result.setIsApproximateLength( false );
        result.setIsCircular( false );
        result.setFractionRepeats( 0.0 );
        result.setSequence( RandomStringUtils.insecure().next( 100, "ATGC" ) );
        DatabaseEntry genbank = ExternalDatabaseUtils.getGenbankAccession( object.toString() );
        result.setSequenceDatabaseEntry( genbank );
        return result;
    }
}