package compbio.codereview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import compbio.codereview.PDBRestClient.PDBDocField;

public class PDBRestClientTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void executeRequestTest() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.MOLECULE_TYPE);
		wantedFields.add(PDBDocField.PDB_ID);
		wantedFields.add(PDBDocField.GENUS);
		wantedFields.add(PDBDocField.GENE_NAME);
		wantedFields.add(PDBDocField.TITLE);

		PDBRestRequest request = new PDBRestRequest();
		request.setAllowEmptySeq(false);
		request.setResponseSize(100);
		request.setFieldToSearchBy("text:");
		request.setSearchTerm("abc");
		request.setWantedFields(wantedFields);

		PDBRestResponse response = new PDBRestClient().executeRequest(request);
		assertTrue(response.getNumberOfItemsFound() > 99);
		assertTrue(response.getSearchSummary() != null);
		assertTrue(response.getSearchSummary().size() > 99);
	}

	@Test
	public void getPDBDocFieldsAsCommaDelimitedStringTest() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.MOLECULE_TYPE);
		wantedFields.add(PDBDocField.PDB_ID);
		wantedFields.add(PDBDocField.GENUS);
		wantedFields.add(PDBDocField.GENE_NAME);
		wantedFields.add(PDBDocField.TITLE);
		assertEquals("molecule_type,pdb_id,genus,gene_name,title", PDBRestClient.getPDBDocFieldsAsCommaDelimitedString(wantedFields));
	}

	@Test
	public void parsePDBJsonExceptionStringTest() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.MOLECULE_TYPE);
		wantedFields.add(PDBDocField.PDB_ID);
		wantedFields.add(PDBDocField.GENUS);
		wantedFields.add(PDBDocField.GENE_NAME);
		wantedFields.add(PDBDocField.TITLE);

		PDBRestRequest request = new PDBRestRequest();
		request.setAllowEmptySeq(false);
		request.setResponseSize(100);
		request.setFieldToSearchBy("text:");
		request.setSearchTerm("abc");
		request.setWantedFields(wantedFields);

		String jsonErrorResponse = "";
		try {
			jsonErrorResponse = readJsonStringFromFile("test/compbio/codereview/pdb_request_json_error.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String parsedErrorResponse = PDBRestClient.parseJsonExceptionString(jsonErrorResponse);
		String expectedErrorMsg = "org.apache.solr.search.SyntaxError: Cannot parse 'text:abc OR text:go:abc AND molecule_sequence:['' TO *]': Encountered \" \":\" \": \"\" at line 1, column 19.{\"q\":\"text:abc OR text:go:abc AND molecule_sequence:['' TO *]\",\"fl\":\"pdb_id\",\"sort\":\"\",\"rows\":\"100\",\"wt\":\"json\"}";

		assertEquals(expectedErrorMsg, parsedErrorResponse);
	}

	@Test(expected = RuntimeException.class)
	public void testForExpectedRuntimeException() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.PDB_ID);

		PDBRestRequest request = new PDBRestRequest();
		request.setFieldToSearchBy("text:");
		request.setSearchTerm("abc OR text:go:abc");
		request.setWantedFields(wantedFields);
		new PDBRestClient().executeRequest(request);
	}

	@Test
	public void parsePDBJsonResponseTest() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.MOLECULE_TYPE);
		wantedFields.add(PDBDocField.PDB_ID);
		wantedFields.add(PDBDocField.GENUS);
		wantedFields.add(PDBDocField.GENE_NAME);
		wantedFields.add(PDBDocField.TITLE);

		PDBRestRequest request = new PDBRestRequest();
		request.setAllowEmptySeq(false);
		request.setWantedFields(wantedFields);

		String jsonString = "";
		try {
			jsonString = readJsonStringFromFile("test/compbio/codereview/pdb_response_json.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		PDBRestResponse response = PDBRestClient.parsePDBJsonResponse(jsonString, request);
		assertTrue(response.getSearchSummary() != null);
		assertTrue(response.getNumberOfItemsFound() == 931);
		assertTrue(response.getSearchSummary().size() == 14);
	}

	@Test
	public void getPDBIdColumIndexTest() {
		List<PDBDocField> wantedFields = new ArrayList<PDBDocField>();
		wantedFields.add(PDBDocField.MOLECULE_TYPE);
		wantedFields.add(PDBDocField.GENUS);
		wantedFields.add(PDBDocField.GENE_NAME);
		wantedFields.add(PDBDocField.TITLE);
		wantedFields.add(PDBDocField.PDB_ID);
		assertEquals(5, PDBRestClient.getPDBIdColumIndex(wantedFields, true));
		assertEquals(4, PDBRestClient.getPDBIdColumIndex(wantedFields, false));
	}

	public String readJsonStringFromFile(String filePath) throws IOException {
		String fileContent;
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			fileContent = sb.toString();
		} finally {
			br.close();
		}
		return fileContent;
	}


}
