Ext.namespace('Gemma');

Gemma.alertUserToError = function(baseValueObject, formatArguments) {
	// Set the minimum width of message box so that title is not wrapped if it is longer than message box body text. 
	Ext.MessageBox.minWidth = 250;
	
	if (baseValueObject.errorFound) {
		if (baseValueObject.accessDenied) {
			Ext.MessageBox.alert(
				String.format(Gemma.HelpText.CommonErrors.accessDenied.title, formatArguments[0]),
				Gemma.HelpText.CommonErrors.accessDenied.text
			);
		} else if (baseValueObject.objectAlreadyRemoved) {
			Ext.MessageBox.alert(
				String.format(Gemma.HelpText.CommonErrors.objectAlreadyRemoved.title, formatArguments[0]),
				String.format(Gemma.HelpText.CommonErrors.objectAlreadyRemoved.text, formatArguments[1])
			);
		} else if (baseValueObject.userNotLoggedIn) {
			Ext.MessageBox.alert(
				String.format(Gemma.HelpText.CommonErrors.userNotLoggedIn.title, formatArguments[0]),
				Gemma.HelpText.CommonErrors.userNotLoggedIn.text,
				Gemma.AjaxLogin.showLoginWindowFn
			); 
		} else {
			Ext.MessageBox.alert(
				String.format(Gemma.HelpText.CommonErrors.errorUnknown.title, formatArguments[0]),
				Gemma.HelpText.CommonErrors.errorUnknown.text
			);
		}
	}			
};
