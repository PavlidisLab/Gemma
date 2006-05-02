<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>
            <fmt:message key="expressionExperiments.title" />
        </title>
    </head>
    <body>
        <h2>
            <fmt:message key="search.results" />
        </h2>

        <display:table name="expressionExperiments" class="list" requestURI="" id="expressionExperimentList"
            export="true" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
			
			<display:column property="id" title="Internal Id" sortable="true" maxWords="20" />
			
			<display:column property="nameLink" sortable="true" titleKey="expressionExperiment.name" />
			  
			<display:column property="createDate" sortable="true" titleKey="auditTrail.date" />
			            
            <display:column property="source" autolink="true" sortable="true" maxWords="20" />   

            <display:column property="detailsLink" sortable="true" titleKey="expressionExperiment.id" />

            <display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />
            
            <display:column property="taxon" sortable="true" titleKey="expressionExperiment.taxon" />
  
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>

    </body>
</html>
