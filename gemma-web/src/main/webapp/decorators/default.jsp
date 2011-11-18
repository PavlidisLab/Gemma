<!DOCTYPE html>
<%-- Include common set of tag library declarations for each layout --%>
<%@ include file="/common/taglibs.jsp"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<%-- Include common set of meta tags for each layout --%>
		<%@ include file="/common/meta.jsp"%>
		
		<link rel="shortcut icon" href="/Gemma/images/logo/gemmaTinyTrans.gif" />
		<title><decorator:title /> | <fmt:message key="webapp.name" />
		</title>

		<jwr:style src="/bundles/gemma-all.css" />
		<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
		<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

		<jwr:script src="/bundles/include.js" />
		<jwr:script src="/bundles/gemma-lib.js" />
		
		<%-- for dwr creation of javascript objects to mirror java value objects--%>
		<script type='text/javascript' src='/Gemma/dwr/interface/DatabaseBackedExpressionExperimentSetValueObject.js'></script>
		<script type='text/javascript' src='/Gemma/scripts/app/valueObjectsInheritanceStructure.js'></script>
		
		<%-- for registration, possible from any page--%>
		<script type="text/javascript" src="http://api.recaptcha.net/js/recaptcha_ajax.js"> </script>
		
		
		<decorator:head />


	</head>
	<body <decorator:getProperty property="body.id" writeEntireProperty="true"/>
		<decorator:getProperty property="body.class" writeEntireProperty="true"/>>

		<div id="page">

			<div id="headerclearnopadding" class="clearfix">
				<jsp:include page="/common/header.inner.jsp" />
			</div>
			

			<div id="content" class="clearfix">



				<div id="main">
					<%@ include file="/common/messages.jsp"%>
					<%@ include file="/common/helpLink.jsp"%>

					<h2>
						<decorator:getProperty property="page.heading" />
					</h2>


					<decorator:body />
				</div>


			</div>
			
			<jsp:include page="/common/userStatusVariables.jsp" />

		</div>

	</body>
</html>

