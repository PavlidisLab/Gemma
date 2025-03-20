// log javascript errors
window.onerror = function( errorMessage, url, line ) {
   // message == text-based error description
   // url     == url which exhibited the script error
   // line    == the line number being executed when the error occurred
   if ( typeof JavascriptLogger !== 'undefined' ) {
      JavascriptLogger.writeToErrorLog( errorMessage, url, line, document.location.href, navigator.userAgent );
   }
};
