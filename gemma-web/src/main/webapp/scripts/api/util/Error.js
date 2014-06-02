Ext.namespace( 'Gemma' );

Gemma.genericErrorHandler = function( err, exception ) {
   if ( typeof this.getEl == 'function' && this.getEl() != null && typeof this.getEl().unmask == 'function' ) {
      this.getEl().unmask();
   }
   if ( err.stack ) {
      console.log( err.stack );
      Ext.Msg.alert( "Generic error handler", err + "\n" + err.stack );
   } else {
      console.log( exception.stack );

      Ext.Msg.alert( "Generic error handler", err + "\n" + (exception ? exception.stack : 'No details') );

   }
};

Gemma.alertUserToError = function( baseValueObject, title ) {
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
