/* this code is generated, see generate-dwr-client.py for details */
var BibliographicReferenceController = {};
BibliographicReferenceController._path = '/dwr';
BibliographicReferenceController.update = function(p0, callback) {
  dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'update', p0, callback);
}
BibliographicReferenceController.load = function(p0, callback) {
  dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'load', p0, callback);
}
BibliographicReferenceController.search = function(p0, callback) {
  dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'search', p0, callback);
}
BibliographicReferenceController.loadFromPubmedID = function(p0, callback) {
  dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'loadFromPubmedID', p0, callback);
}
BibliographicReferenceController.browse = function(p0, callback) {
  dwr.engine._execute(BibliographicReferenceController._path, 'BibliographicReferenceController', 'browse', p0, callback);
}
window.BibliographicReferenceController = BibliographicReferenceController;
export default BibliographicReferenceController;
