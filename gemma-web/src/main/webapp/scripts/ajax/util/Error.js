Ext.namespace('Gemma');

Gemma.alertUserToError = function(baseValueObject, title) {
	// Set the minimum width of message box so that title is not wrapped if it is longer than message box body text. 
	Ext.MessageBox.minWidth = 250;
	
	if (baseValueObject.errorFound) {
		if (baseValueObject.accessDenied) {
			Ext.MessageBox.alert(title,	Gemma.HelpText.CommonErrors.accessDenied);
		} else if (baseValueObject.objectAlreadyRemoved) {
			Ext.MessageBox.alert(title,	Gemma.HelpText.CommonErrors.objectAlreadyRemoved);
		} else if (baseValueObject.userNotLoggedIn) {
			Ext.MessageBox.alert(title,	Gemma.HelpText.CommonErrors.userNotLoggedIn,
				Gemma.AjaxLogin.showLoginWindowFn
			); 
		} else {
			Ext.MessageBox.alert(title,	Gemma.HelpText.CommonErrors.errorUnknown);
		}
	}			
};
