package ubic.gemma.rest.util;

public interface PaginatedResponseObject extends GroupedResponseObject {

    Integer getOffset();

    Integer getLimit();

    Long getTotalElements();
}
