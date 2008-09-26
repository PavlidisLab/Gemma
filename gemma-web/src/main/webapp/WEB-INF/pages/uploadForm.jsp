<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>File upload demo</title>

	<jwr:script src='/scripts/ajax/util/FileUploadForm.js' useRandomParam="false" />


</head>


<body>

	<script>
	
	Ext.namespace('Gemma');
	Ext.form.Field.prototype.msgTarget = 'side';
	Ext.BLANK_IMAGE_URL = "/Gemma/images/s.gif";

	 
	Ext.onReady( function()
	 {
	   Ext.QuickTips.init();
	 	var form = new Gemma.FileUploadForm({renderTo : 'fi-form'});
	 });	
		
		</script>
	<div id="fi-form"></div>

</body>
