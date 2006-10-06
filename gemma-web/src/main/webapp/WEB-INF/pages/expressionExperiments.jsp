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
		
			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />
	            
            <display:column property="dataSource" sortable="true" titleKey="externalDatabase.title" maxWords="20" />   

            <display:column property="externalDetailsLink" sortable="true" titleKey="expressionExperiment.details" />

            <display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />
           
            <display:column property="taxon" sortable="true" titleKey="taxon.title" />
  
  				<display:column property="delete" sortable="false" titleKey="expressionExperiment.delete"/>
  				
            <display:setProperty name="basic.empty.showtable" value="true" />             
        </display:table>

    </body>
</html>
