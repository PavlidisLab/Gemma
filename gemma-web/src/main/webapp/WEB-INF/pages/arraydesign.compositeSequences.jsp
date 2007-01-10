<%@ include file="/common/taglibs.jsp"%>


		<h3>
			Displaying <c:out value="${numSequenceData}" /> probes.
		</h3>

				<display:table name="sequenceData" sort="list" class="list" requestURI="" id="arrayDesignSequenceList"
				pagesize="200" decorator="ubic.gemma.web.taglib.displaytag.expression.arrayDesign.ArrayDesignWrapper">
					
					<display:setProperty name="basic.empty.showtable" value="true" />
				</display:table>