<%-- Other notifications and...stuff --%>
<%@ include file="/common/taglibs.jsp"%>

<div id="left-bar-messages">
	<h2>
		Welcome!
	</h2>
	<p style="font-size: 0.90em">
		Gemma is a database and software system for the
		<strong>meta-analysis of gene expression data</strong>. Gemma contains data from hundreds of public
		<a href="<c:url value="/expressionExperiment/showAllExpressionExperiments.html"/>">microarray data sets</a>,
		referencing hundreds of
		<a href="<c:url value="/bibRef/showAllEeBibRefs.html"/>">published papers</a>. Users can search, access and visualize
		<a href="<c:url value="/searchCoexpression.html"/>">coexpression</a> and
		<a href="<c:url value="/diff/diffExpressionSearch.html"/>">differential expression</a> results.
	</p>
	<p style="font-size: 0.9em">
		More information about the project is available
		<a href="<c:url value="/static/about.html"/>">here</a>. Gemma also has a
		<a href="http://chibi.ubc.ca/faculty/pavlidis/wiki/display/gemma">Wiki</a> where you can read
		documentation, in addition to the in-line help.
	</p>
		<p style="font-size: 0.9em">
		Want to see what we're working on? Check out our work in progress for the 
		<a href="/Gemma/home.html">latest version of Gemma</a>.
	</p>
</div>


<%--Don't show this area if the user is logged in. --%>
<security:authorize access="isAnonymous()">
	<div class="roundedcornr_box_777249" style="margin-bottom: 10px;">
		<div class="roundedcornr_top_777249">
			<div></div>
		</div>
		<div class="roundedcornr_content_777249" id="contact">
			<div>
				<strong>Get an account</strong>
				<p class="emphasized" style="font-size: 0.90em">
					Most features of Gemma are open to guests. However, to access some functionality, such as data upload, you'll need
					an account.
					<strong><a href="<c:url value="/register.html"/>">Sign up</a> </strong>, or
					<strong><a href="<c:url value="/login.jsp" />">log in</a> </strong> if you already have an account.
				</p>
			</div>
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
	</div>
</security:authorize>

<c:if test="${whatsNew != null}">
	<div class="roundedcornr_box_777249" style="margin-bottom: 15px;">
		<div class="roundedcornr_top_777249">
			<div></div>
		</div>
		<div class="roundedcornr_content_777249">
			<div id="whatsNew">
				<Gemma:whatsNew whatsNew="${whatsNew}" />
			</div>
		</div>
		<div class="roundedcornr_bottom_777249">
			<div></div>
		</div>
	</div>
</c:if>


<div class="roundedcornr_box_777249" style="margin-bottom: 10px;">
	<div class="roundedcornr_top_777249">
		<div></div>
	</div>
	<div class="roundedcornr_content_777249" id="contact">
		<div>
			<strong>Contacting us</strong>
			<p class="emphasized" style="font-size: 0.90em">
				To get emails about updates to the Gemma software, subscribe to the
				<a href="http://lists.chibi.ubc.ca/mailman/listinfo/gemma-announce">Gemma-announce mailing list</a>. Please send bug
				reports or feature requests
				<a href="mailto:gemma@chibi.ubc.ca">here</a>.
			</p>
		</div>
	</div>
	<div class="roundedcornr_bottom_777249">
		<div></div>
	</div>
</div>

