<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Registration</title>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/signup.js' />
	<script type="text/javascript" src="http://api.recaptcha.net/js/recaptcha_ajax.js">
	
</script>

</head>

<p>
	Register to use use features of Gemma like data upload. You might want to review the
	<a href='<c:url value="/static/termsAndConditions.html" />'>Terms and conditions</a> (which includes our privacy
	policy) before signing up.
</p>
<p>
	After submitting the form, you will be sent an email with your account details.
</p>
<div align='center' id='signup' style='margin-bottom: 1em;'></div>
<div align='center' id='errorMessage' style='width: 700px; margin-bottom: 1em;'></div>
