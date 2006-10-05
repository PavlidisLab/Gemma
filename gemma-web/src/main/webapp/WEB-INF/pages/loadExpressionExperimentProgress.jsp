<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="command" scope="request"
   class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand" />


<html>
    <head>
<title><fmt:message key="expressionExperiment.load.title" /></title>
<content tag="heading">
<fmt:message key="expressionExperiment.load.title" />
</content>



        <script type='text/javascript' src='/Gemma/dwr/interface/HttpProgressMonitor.js'></script>
        <script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
        <script type='text/javascript' src='/Gemma/dwr/util.js'></script>
        <script type='text/javascript' src="<c:url value="scripts/indeterminateProgress.js"/>"></script>
        <style type="text/css"> 
			#progressBar { padding-top: 5px; }
			#progressBarBox { width: 350px; height: 20px; border: 1px inset; background: #EEEEEE;}
			#progressBarBoxContent { width: 0; height: 20px; border-right: 1px solid #444444; background: #9ACB34; } 

</style>
    </head>
    <body>




	
<script type="text/javascript">
	createProgressBar();
	startProgress();
</script>



  </body>
</html>




