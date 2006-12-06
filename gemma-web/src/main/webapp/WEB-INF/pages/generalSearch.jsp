<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

        <title>
            <fmt:message key="generalSearch.title" />
        </title>

        <h2>
            General search tool for searching Gemma
        </h2>

	<form name="generalSearch" action="searcher.html" method="POST">
			<h4> Enter search criteria for searching Gemma database here </h4>
			<input type="text" name="searchString" size="78" />
			<input type="submit" value="search"/>			
		</form>
		
		<br/>

<h3>
			Your search for  <b> <c:out value="${SearchString}"/> </b> found  <b> <c:out value="${numGenes}" /> </b> Genes.
		</h3>

	    <display:table name="geneList" class="list" 
	    	requestURI="" 
	    	id="genesList"
            decorator="ubic.gemma.web.taglib.displaytag.gene.GeneFinderWrapper" 
            pagesize="20">	
			<display:column property="nameLink" sortable="true" titleKey="gene.officialSymbol" maxWords="20" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" maxWords="20" />
			<display:column property="officialName" sortable="true" titleKey="gene.officialName" maxWords="20" />			
            <display:setProperty name="basic.empty.showtable" value="false" />      
        </display:table>
		<h3>
			Your search for <b> <c:out value="${SearchString}"/> </b> found   <b> <c:out value="${numEEs}" /> </b> Datasets
		</h3>

		<display:table pagesize="20" name="expressionList" sort="list" class="list" requestURI="" id="expressionExperimentList"
			decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
			<display:column property="nameLink" sortable="true" sortProperty="name" titleKey="expressionExperiment.name" />
			<display:column property="shortName" sortable="true" titleKey="expressionExperiment.shortName" />
			<authz:authorize ifAnyGranted="admin">
	 			<display:column property="arrayDesignLink" sortable="true" title="Arrays" />
			</authz:authorize>
 			<display:column property="assaysLink" sortable="true" titleKey="bioAssays.title" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" />
			<display:setProperty name="basic.empty.showtable" value="false" />
		</display:table>
		<h3>
			Your search for <b> <c:out value="${SearchString}"/>  </b> found   <b> <c:out value="${numADs}" /> </b> Arrays
		</h3>

				<display:table name="arrayList" sort="list" class="list" requestURI="" id="arrayDesignList"
				pagesize="20" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
					<display:column property="name" sortable="true" href="showArrayDesign.html" paramId="id" paramProperty="id"
						titleKey="arrayDesign.name" />
					<display:column property="shortName" sortable="true" titleKey="arrayDesign.shortName" />
					<display:column property="expressionExperimentCountLink" sortable="true" title="Expts" />
					<authz:authorize ifAnyGranted="admin">
						<display:column property="color" sortable="true" titleKey="arrayDesign.technologyType" />
					</authz:authorize>
					<display:setProperty name="basic.empty.showtable" value="false" />
				</display:table>
