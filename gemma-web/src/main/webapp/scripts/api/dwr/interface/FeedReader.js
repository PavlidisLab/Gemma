/* this code is generated, see generate-dwr-client.py for details */
var FeedReader = {};
FeedReader._path = '/dwr';
FeedReader.getLatestNews = function(callback) {
  dwr.engine._execute(FeedReader._path, 'FeedReader', 'getLatestNews', callback);
}
window.FeedReader = FeedReader;
export default FeedReader;
