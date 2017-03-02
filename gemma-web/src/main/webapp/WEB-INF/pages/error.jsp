<%@ include file="/common/taglibs.jsp"%>

<%@ page language="java" isErrorPage="true"%>

<title><fmt:message key="errorPage.title" /></title>
<input type="hidden" id="reloadOnLogin" value="true" />

<style>
	ul{
		padding-left: 20px;
	}
	ul li{
		list-style-type: circle;
	}
</style>

<div class="padded" style="padding-top: 20px">
	<h2>
		<fmt:message key="errorPage.heading" />
	</h2>
	
	<hr class="normal">
	
	<div class="v-padded">
		<p>Possible next steps:</p>
		<ul>
			<li>Go back and try what you were doing again</li>
			<li>Do you need to log in?</li>
			<c:choose>
				<c:when test="${not empty param.exception}">
					<li><a href="mailto:gemma@chibi.ubc.ca?subject=${param.exception.message}">Email us</a> about the problem.</li>
				</c:when>
				<c:when test="${not empty requestScope['javax.servlet.error.exception']}">
					<li><a href="mailto:gemma@chibi.ubc.ca?subject=${requestScope['javax.servlet.error.exception']}">Email us</a> about the problem.</li>
				</c:when>
				<c:when test="${not empty requestScope['exception']}">
					<li><a href="mailto:gemma@chibi.ubc.ca?subject=${requestScope['exception']}">Email us</a> about the problem.</li>
				</c:when>
				<c:otherwise>
					<li><a href="mailto:gemma@chibi.ubc.ca?subject=Unknown exception">Email us</a> about the problem.</li>
				</c:otherwise>
			</c:choose>
		</ul>
	</div>
	
	<hr class="normal">

	<c:choose>
		<c:when test="${not empty param.exception}">
			<p>${param.exception.message}</p>

			<security:authorize access="hasRole('GROUP_ADMIN')">
				<Gemma:exception exception="${exception}" />
			</security:authorize>
		</c:when>
		
		<c:when test="${not empty requestScope['javax.servlet.error.exception']}">

			<p>${requestScope['javax.servlet.error.exception'].message}</p>

			<security:authorize access="hasRole('GROUP_ADMIN')">
				<%-- this is causing stackoverflow errors ... no idea why, since upgrading to spring 3.2 from 3.0.7 --%>
				<Gemma:exception exception="${requestScope['javax.servlet.error.exception']}" />
			</security:authorize>
		</c:when>

		<c:when test="${not empty requestScope['exception']}">
			<p>${requestScope['exception'].message}</p>
			
			<security:authorize access="hasRole('GROUP_ADMIN')">
				<Gemma:exception exception="${requestScope['exception']}" />
			</security:authorize>
		</c:when>
		
		<c:otherwise>
			<p>
				<fmt:message key="errorPage.info.missing" />
			</p>
		</c:otherwise>

	</c:choose>
	
</div>
