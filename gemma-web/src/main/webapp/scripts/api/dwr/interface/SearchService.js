/* this code is generated, see generate-dwr-client.py for details */
var SearchService = {};
SearchService._path = '/dwr';
SearchService.ajaxSearch = function(p0, callback) {
  dwr.engine._execute(SearchService._path, 'SearchService', 'ajaxSearch', p0, callback);
}
window.SearchService = SearchService;
export default SearchService;
