/* this code is generated, see generate-dwr-client.py for details */
var IndexService = {};
IndexService._path = '/dwr';
IndexService.index = function(p0, callback) {
  dwr.engine._execute(IndexService._path, 'IndexService', 'index', p0, callback);
}
window.IndexService = IndexService;
export default IndexService;
