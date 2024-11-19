Ext.namespace( 'Gemma' );

Gemma.Error = {};

Gemma.Error.genericErrorHandler = function( err, exception ) {
   if ( typeof this.getEl == 'function' && this.getEl() != null && typeof this.getEl().unmask == 'function' ) {
      this.getEl().unmask();
   }

   console.log( exception );
   if ( err.stack ) {
      var f = parseException( err.stack );
      console.log( f );

      Ext.Msg.alert( "There was an error", err + ":<br/>" + f );
   } else if ( exception.stackTrace ) {
      var c = exception.javaClassName;
      var m = exception.message;
      var f = Gemma.parseException( exception.stackTrace );
      console.log( c + ": " + m + "<br/>" + f );
      Ext.Msg.alert( "There was an error", err + "<br/>" + c + (m ? ":<br/>Message: " + m : "")
         + (f ? "<br/>Details:<br/>" + f : 'No details') );

   } else {
      console.log( exception );

      Ext.Msg.alert( "There was an error", err + "\n" + (exception ? exception : 'No details') );

   }
};

Gemma.Error.alertUserToError = function( baseValueObject, title ) {
   // Set the minimum width of message box so that title is not wrapped if it is longer than message box body text.
   Ext.MessageBox.minWidth = 250;

   if ( baseValueObject.errorFound ) {
      if ( baseValueObject.accessDenied ) {
         Ext.MessageBox.alert( title, Gemma.HelpText.CommonErrors.accessDenied );
      } else if ( baseValueObject.objectAlreadyRemoved ) {
         Ext.MessageBox.alert( title, Gemma.HelpText.CommonErrors.objectAlreadyRemoved );
      } else if ( baseValueObject.userNotLoggedIn ) {
         Ext.MessageBox.alert( title, Gemma.HelpText.CommonErrors.userNotLoggedIn, Gemma.AjaxLogin.showLoginWindowFn );
      } else {
         Ext.MessageBox.alert( title, Gemma.HelpText.CommonErrors.errorUnknown );
      }
   }
};

function parseException( ex ) {
   if ( ex.constructor === Array ) {
      var s = "";
      try {
         for ( var i = 0; i < ex.length; i++ ) {
            var l = ex[i];
            if ( l.fileName == "<generated>" || l.lineNumber < 0 ) {
               continue;
            }
            s = s + l.className + "." + l.methodName + " (" + l.lineNumber + ")<br/>";
            if ( i == 20 ) {
               s = s + "... (truncated)";
               return s;
            }
         }
         return s;
      } catch ( e ) {
         return "Error stack trace could not be parsed";
      }
   } else {
      // console.log( "what" );
      return ex;
   }

};