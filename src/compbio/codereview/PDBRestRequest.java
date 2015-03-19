package compbio.codereview;

import java.util.Collection;

import compbio.codereview.PDBRestClient.PDBDocField;

/**
 * Represents the PDB request object to be consumed by the PDBRestClient
 * 
 * @author tcnofoegbu
 *
 */
public class PDBRestRequest {
	private String fieldToSearchBy;

	private String searchTerm;

	private String fieldToSortBy;

	private String associatedSequence;

	private boolean allowEmptySequence;

	private int responseSize;

	private boolean isSortAscending;

	private Collection<PDBDocField> wantedFields;

	public String getFieldToSearchBy() {
		return fieldToSearchBy;
	}

	public void setFieldToSearchBy(String fieldToSearchBy) {
		this.fieldToSearchBy = fieldToSearchBy;
	}

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public boolean isAllowEmptySeq() {
		return allowEmptySequence;
	}

	public void setAllowEmptySeq(boolean allowEmptySeq) {
		this.allowEmptySequence = allowEmptySeq;
	}

	public int getResponseSize() {
		return responseSize;
	}

	public void setResponseSize(int responseSize) {
		this.responseSize = responseSize;
	}

	public Collection<PDBDocField> getWantedFields() {
		return wantedFields;
	}

	public void setWantedFields(Collection<PDBDocField> wantedFields) {
		this.wantedFields = wantedFields;
	}

	public String getFieldToSortBy() {
		return fieldToSortBy;
	}

	public void setFieldToSortBy(String fieldToSortBy, boolean isSortAscending) {
		this.fieldToSortBy = fieldToSortBy;
		this.isSortAscending = isSortAscending;
	}

	public boolean isAscending() {
		return isSortAscending;
	}

	public String getAssociatedSequence() {
		return associatedSequence;
	}

	public void setAssociatedSequence(String associatedSequence) {
		this.associatedSequence = associatedSequence;
	}

	public String getQuery() {
		return fieldToSearchBy + searchTerm + (isAllowEmptySeq() ? "" : " AND molecule_sequence:['' TO *]");
	}
}
