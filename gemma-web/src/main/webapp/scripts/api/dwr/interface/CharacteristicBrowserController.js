/* this code is generated, see generate-dwr-client.py for details */
var CharacteristicBrowserController = {};
CharacteristicBrowserController._path = '/dwr';
CharacteristicBrowserController.count = function(callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'count', callback);
}
CharacteristicBrowserController.findCharacteristics = function(p0, callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'findCharacteristics', p0, callback);
}
CharacteristicBrowserController.findCharacteristicsCustom = function(p0, p1, p2, p3, p4, p5, p6, p7, callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'findCharacteristicsCustom', p0, p1, p2, p3, p4, p5, p6, p7, callback);
}
CharacteristicBrowserController.removeCharacteristics = function(p0, callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'removeCharacteristics', p0, callback);
}
CharacteristicBrowserController.updateCharacteristics = function(p0, callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'updateCharacteristics', p0, callback);
}
CharacteristicBrowserController.browse = function(p0, callback) {
  dwr.engine._execute(CharacteristicBrowserController._path, 'CharacteristicBrowserController', 'browse', p0, callback);
}
window.CharacteristicBrowserController = CharacteristicBrowserController;
export default CharacteristicBrowserController;
