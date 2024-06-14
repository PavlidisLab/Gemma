package ubic.gemma.rest.util;

public interface FilteredAndPaginatedResponseDataObject<T> extends ResponseDataObject<T>, FilteredResponseObject, GroupedResponseObject, SortedResponseObject, PaginatedResponseObject {

}
