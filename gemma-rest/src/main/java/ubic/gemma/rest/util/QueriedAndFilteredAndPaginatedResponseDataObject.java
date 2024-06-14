package ubic.gemma.rest.util;

public interface QueriedAndFilteredAndPaginatedResponseDataObject<T> extends ResponseDataObject<T>, QueriedResponseObject, FilteredResponseObject, GroupedResponseObject, SortedResponseObject, PaginatedResponseObject {
}
