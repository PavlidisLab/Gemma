package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ScoreValueObject implements Comparable<ScoreValueObject> {

    // possible values: 0, 0.2, 0.4, 0.6, 0.8, 1
    // indicate the strength of the evidence
    private Double strength = new Double( 0 );

    // original value from what the strength came from
    private String scoreValue = "";
    private String scoreName = "";

    public ScoreValueObject() {

    }

    public ScoreValueObject( Double strength, String scoreValue, String scoreName, String scoreDescription ) {
        super();
        this.strength = strength;
        this.scoreValue = scoreValue;
        this.scoreName = scoreName;
    }

    public Double getStrength() {
        return this.strength;
    }

    public void setStrength( Double strength ) {
        this.strength = strength;
    }

    public String getScoreValue() {
        return this.scoreValue;
    }

    public void setScoreValue( String scoreValue ) {
        this.scoreValue = scoreValue;
    }

    public String getScoreName() {
        return this.scoreName;
    }

    public void setScoreName( String scoreName ) {
        this.scoreName = scoreName;
    }

    @Override
    public int compareTo( ScoreValueObject o ) {
        return this.strength.compareTo( o.strength );
    }

}
