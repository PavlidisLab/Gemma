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
		<script type='text/javascript' src='/Gemma/dwr/interface/DifferentialExpressionSearchController.js'></script>		
		<%-- We should use this as soon as we figure out how to set generateDtoClasses option to dtoall 
		script type='text/javascript' src='/Gemma/dwr/dtoall.js'></script>	--%>
 				
		<%-- for registration, possible from any page--%>
		<script type="text/javascript" src="http://api.recaptcha.net/js/recaptcha_ajax.js"> </script>
		
		<%-- not bundled with JAWR, needs to be included *last* (at least after include.js & gemma-lib.js) --%>
		<jwr:script src='/scripts/app/valueObjectsInheritanceStructure.js' />	
		
		
		
		<%-- log javascript errors --%>
		<script type="text/javascript">
			window.onerror = function(errorMessage, url, line){		
				// message == text-based error description
			    // url     == url which exhibited the script error
			    // line    == the line number being executed when the error occurred
				JavascriptLogger.writeToErrorLog(errorMessage, url, line, document.location.href, navigator.userAgent);
				
				//return true; // supresses js error messages
			};
		</script>
		
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

