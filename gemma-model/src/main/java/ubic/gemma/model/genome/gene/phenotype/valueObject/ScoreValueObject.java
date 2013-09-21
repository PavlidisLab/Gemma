package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ScoreValueObject {

    // possible values: 0, 0.2, 0.4, 0.6, 0.8, 1
    // indicate the strength of the evidence
    private Double strength = null;

    // original value from what the strength came from
    private String scoreValue = "";
    private String scoreName = "";

    public ScoreValueObject() {

    }

    public ScoreValueObject( Double strength, String scoreValue, String scoreName ) {
        super();
        this.strength = strength;
        this.scoreValue = scoreValue;
        this.scoreName = scoreName;
    }

    public String getScoreName() {
        return this.scoreName;
    }

    public String getScoreValue() {
        return this.scoreValue;
    }

    public Double getStrength() {
        return this.strength;
    }

    public void setScoreName( String scoreName ) {
        this.scoreName = scoreName;
    }

    public void setScoreValue( String scoreValue ) {
        this.scoreValue = scoreValue;
    }

    public void setStrength( Double strength ) {
        this.strength = strength;
    }

}
