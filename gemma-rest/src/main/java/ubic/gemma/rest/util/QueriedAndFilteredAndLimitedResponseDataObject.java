package ubic.gemma.rest.util;

public interface QueriedAndFilteredAndLimitedResponseDataObject<T> extends ResponseDataObject<T>, QueriedResponseObject, FilteredResponseObject, GroupedResponseObject, SortedResponseObject, LimitedResponseObject {

}
