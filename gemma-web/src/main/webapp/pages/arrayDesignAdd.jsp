<%@ include file="/common/taglibs.jsp"%>

<%--@elvariable id="arrayDesigns" type="java.util.List"--%>
<%--@elvariable id="taxa" type="java.util.List"--%>

<title> add array design </title>

<spring:bind path="arrayDesignAddCommand.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<fmt:message key="icon.warning" var="warningIconAlt"/>
				<Gemma:img src="/images/iconWarning.gif" alt="${warningIconAlt}" cssClass="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>

<form method="post" action="<c:url value="/arrayDesign/newArrayDesign.html"/>" enctype="multipart/form-data">

	<ul>
		<li>
			<label styleClass="desc" key="arrayDesign" ></label>
			<spring:bind path="arrayDesignAddCommand.arrayDesign">
				<select name="${status.expression}">
					<c:forEach items="${arrayDesigns}" var="arrayDesign">
						<spring:transform value="${arrayDesign}" var="name" />
						<option value="${name}" <c:if test="${status.value == name}">selected</c:if>>
							${name}
						</option>
					</c:forEach>
				</select>
				<span class="fieldError">${status.errorMessage}</span>
			</spring:bind>
		</li>

		<li>
			<label styleClass="desc" key="taxon" ></label>
			<spring:bind path="arrayDesignAddCommand.taxon">
				<select name="${status.expression}">
					<c:forEach items="${taxa}" var="taxon">
						<spring:transform value="${taxon}" var="scientificName" />
						<option value="${scientificName}" <c:if test="${status.value == scientificName}">selected</c:if>>
							${scientificName}
						</option>
					</c:forEach>
				</select>
				<span class="fieldError">${status.errorMessage}</span>
			</spring:bind>
		</li>


		<li>
			<label styleClass="desc" key="file" ></label>
			<spring:bind path="arrayDesignSequenceCommand.file.file">
				<input type="file" size=30 name="<c:out value="${status.expression}" />" value="<c:out value="${status.value}" />" />
				<span class="fieldError">${status.errorMessage}</span>
			</spring:bind>
		</li>


	</ul>

	<div>
		<input type="submit" class="button" name="submit" value="<fmt:message key="button.submit"/>" />
		<input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
	</div>

</form>


