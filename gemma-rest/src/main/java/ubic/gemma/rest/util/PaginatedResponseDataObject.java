package ubic.gemma.rest.util;

/**
 * Represents paginated results with offset and limit.
 *
 * @author poirigui
 * @see Responders#paginate
 */
public interface PaginatedResponseDataObject<T> extends ResponseDataObject<T>, PaginatedResponseObject, GroupedResponseObject, SortedResponseObject {

}
