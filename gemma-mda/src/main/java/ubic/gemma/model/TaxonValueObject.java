package ubic.gemma.model;

import ubic.gemma.model.genome.Taxon;

public class TaxonValueObject {

    private java.lang.String scientificName;
    private java.lang.String commonName;
    private java.lang.String abbreviation;
    private java.lang.String unigenePrefix;
    private java.lang.String swissProtSuffix;
    private java.lang.Integer ncbiId;
    private java.lang.Boolean isSpecies;
    private java.lang.Boolean isGenesUsable;
    private java.lang.Long id;
    private ExternalDatabaseValueObject externalDatabase;
    private TaxonValueObject parentTaxon;

    public java.lang.String getScientificName() {
        return this.scientificName;
    }

    public void setScientificName( java.lang.String scientificName ) {
        this.scientificName = scientificName;
    }

    public java.lang.String getCommonName() {
        return this.commonName;
    }

    public void setCommonName( java.lang.String commonName ) {
        this.commonName = commonName;
    }

    public java.lang.String getAbbreviation() {
        return this.abbreviation;
    }

    public void setAbbreviation( java.lang.String abbreviation ) {
        this.abbreviation = abbreviation;
    }

    public java.lang.String getUnigenePrefix() {
        return this.unigenePrefix;
    }

    public void setUnigenePrefix( java.lang.String unigenePrefix ) {
        this.unigenePrefix = unigenePrefix;
    }

    public java.lang.String getSwissProtSuffix() {
        return this.swissProtSuffix;
    }

    public void setSwissProtSuffix( java.lang.String swissProtSuffix ) {
        this.swissProtSuffix = swissProtSuffix;
    }

    public java.lang.Integer getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( java.lang.Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public java.lang.Boolean getIsSpecies() {
        return this.isSpecies;
    }

    public void setIsSpecies( java.lang.Boolean isSpecies ) {
        this.isSpecies = isSpecies;
    }

    public java.lang.Boolean getIsGenesUsable() {
        return this.isGenesUsable;
    }

    public void setIsGenesUsable( java.lang.Boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    public java.lang.Long getId() {
        return this.id;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    public ExternalDatabaseValueObject getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public TaxonValueObject getParentTaxon() {
        return this.parentTaxon;
    }

    public void setParentTaxon( TaxonValueObject parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }

    public static TaxonValueObject fromEntity( Taxon taxon ) {
        TaxonValueObject vo = new TaxonValueObject();
        vo.setScientificName( taxon.getScientificName() );
        vo.setId( taxon.getId() );
        vo.setCommonName( taxon.getCommonName() );

        if ( taxon.getExternalDatabase() != null ) {
            vo.setExternalDatabase( ExternalDatabaseValueObject.fromEntity( taxon.getExternalDatabase() ) );
        }

        return vo;
    }

}
