package ubic.gemma.persistence.util;

public class Sort {

    public enum Direction {
        ASC, DESC
    }

    public String orderBy;
    public Direction direction;

    public Sort( String orderBy, Direction direction ) {
        this.orderBy = orderBy;
        this.direction = direction;
    }
}
