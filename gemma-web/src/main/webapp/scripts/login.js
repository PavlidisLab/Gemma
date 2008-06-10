/**
 * FIXME this is non-functional.
 */
Ext.onReady(function() {
	if (getCookie("username") != null) {
		$("j_username").value = getCookie("username");
		$("j_password").focus();
	} else {
		$("j_username").focus();
	}
});

var saveUsername = function(theForm) {
	var expires = new Date();
	// sets it for approx 30 days.
	expires.setTime(expires.getTime() + 24 * 30 * 60 * 60 * 1000);
	setCookie("username", theForm.j_username.value, expires, "<c:url value="
			/ "/>");
}

var  validateForm = function(form) {
	return validateRequired(form); // defined in global.js
}

var passwordHint = function() {
	if ($("j_username").value.length == 0) {
		alert("The <fmt:message key="
				+ label.username
				+ "/> field must be filled in to get a password hint sent to you.");
		$("j_username").focus();
	} else {
		location.href = "Gemma/passwordHint.html?username="
				+ $("j_username").value;
	}
}

var required = function() {
	this.aa = new Array("j_username", "Required", new Function("varName",
			" return this[varName];"));
	this.ab = new Array("j_password", "Required", new Function("varName",
			" return this[varName];"));
}
