package ubic.gemma.rest.util;

public interface FilteredAndLimitedResponseDataObject<T> extends ResponseDataObject<T>, FilteredResponseObject, GroupedResponseObject, SortedResponseObject, LimitedResponseObject {

}
