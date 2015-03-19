package compbio.codereview;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import compbio.codereview.PDBRestResponse.PDBResponseSummary;

public class PDBRestClient {
	private static String PDB_SEARCH_ENDPOINT = "http://wwwdev.ebi.ac.uk/pdbe/search/pdb/select?";
	private static int DEFAULT_RESPONSE_SIZE = 200;

	/**
	 * Takes a PDBRestRequest object and returns a response upon execution
	 * 
	 * @param pdbRestRequest
	 *            the pdbRequest to be sent
	 * @return the pdbResponse object for the given pdbRequest
	 */
	public PDBRestResponse executeRequest(PDBRestRequest pdbRestRequest) {
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(clientConfig);

		String wantedFields = getPDBDocFieldsAsCommaDelimitedString(pdbRestRequest.getWantedFields());
		int responseSize = (pdbRestRequest.getResponseSize() == 0) ? DEFAULT_RESPONSE_SIZE : pdbRestRequest.getResponseSize();
		String sortParam = (pdbRestRequest.getFieldToSortBy() == null || pdbRestRequest.getFieldToSortBy().trim().isEmpty()) ? "" : (pdbRestRequest.getFieldToSortBy() + (pdbRestRequest.isAscending() ? " asc" : " desc"));

		// Build request parameters for the REST Request
		WebResource webResource = client.resource(PDB_SEARCH_ENDPOINT).queryParam("wt", "json").queryParam("fl", wantedFields).queryParam("rows", String.valueOf(responseSize)).queryParam("q", pdbRestRequest.getQuery()).queryParam("sort", sortParam);

		// Execute the REST call and get a Response object
		ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

		// Get the JSON string from the response object
		String responseString = clientResponse.getEntity(String.class);

		// Check the response status and report exception if one occurs
		if (clientResponse.getStatus() != 200) {
			String errorMessage = "";
			if (clientResponse.getStatus() == 400) {
				errorMessage = parseJsonExceptionString(responseString);
				throw new RuntimeException(errorMessage);
			} else {
				errorMessage = "Failed : HTTP error code : " + clientResponse.getStatus();
				throw new RuntimeException(errorMessage);
			}
		}

		// Make object eligible for garbage collection to conserve memory since
		// they are no longer needed
		clientResponse = null;
		client = null;

		// Process the response and return the result to the caller.
		return parsePDBJsonResponse(responseString, pdbRestRequest);
	}

	/**
	 * Process error response from PDB server if/when one occurs.
	 * 
	 * @param jsonResponse
	 *            the json string containing error message from the server
	 * @return the processed error message from the json string
	 */
	public static String parseJsonExceptionString(String jsonErrorResponse) {
		String errorMessage = "RunTime error";
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObj = (JSONObject) jsonParser.parse(jsonErrorResponse);
			JSONObject errorResponse = (JSONObject) jsonObj.get("error");
			errorMessage = errorResponse.get("msg").toString();

			JSONObject responseHeader = (JSONObject) jsonObj.get("responseHeader");
			errorMessage += responseHeader.get("params").toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return errorMessage;
	}

	/**
	 * Parses the JSON response string from PDB REST API to a PDBRestResponse
	 * instance. The response is dynamic - it fetches/processes only the
	 * requested PDB fields specified in the 'wantedFields' request parameter.
	 * 
	 * @param pdbJsonResponseString
	 *            the json string to be parsed
	 * @param pdbRestRequest
	 *            the request object which contains parameters used to process
	 *            the json string
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static PDBRestResponse parsePDBJsonResponse(String pdbJsonResponseString, PDBRestRequest pdbRestRequest) {
		PDBRestResponse searchResult = new PDBRestResponse();
		List<PDBResponseSummary> result = null;
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObj = (JSONObject) jsonParser.parse(pdbJsonResponseString);

			JSONObject pdbResponse = (JSONObject) jsonObj.get("response");
			String queryTime = ((JSONObject) jsonObj.get("responseHeader")).get("QTime").toString();
			int numFound = Integer.valueOf(pdbResponse.get("numFound").toString());
			if (numFound > 0) {
				result = new ArrayList<PDBResponseSummary>();
				JSONArray docs = (JSONArray) pdbResponse.get("docs");
				for (Iterator<JSONObject> docIter = docs.iterator(); docIter.hasNext();) {
					JSONObject doc = docIter.next();
					result.add(searchResult.new PDBResponseSummary(doc, pdbRestRequest));
				}
				searchResult.setNumberOfItemsFound(numFound);
				searchResult.setResponseTime(queryTime);
				searchResult.setSearchSummary(result);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return searchResult;
	}

	/**
	 * Takes a collection of PDBDocField and converts its 'code' Field values
	 * into a comma delimited string.
	 * 
	 * @param pdbDocfields
	 * @return
	 */
	public static String getPDBDocFieldsAsCommaDelimitedString(Collection<PDBDocField> pdbDocfields) {
		String result = "";
		if (pdbDocfields != null && !pdbDocfields.isEmpty()) {
			StringBuilder returnedFields = new StringBuilder();
			for (PDBDocField field : pdbDocfields) {
				returnedFields.append(",").append(field.getCode());
			}
			returnedFields.deleteCharAt(0);
			result = returnedFields.toString();
		}
		return result;
	}

	/**
	 * Determines the column index for 'PDB Id' Fields in the dynamic summary
	 * table. The PDB Id serves as a unique identifier for a given row in the
	 * summary table
	 * 
	 * @param wantedFeilds
	 *            the available table columns in no particular order
	 * @return the pdb id field column index
	 */
	public static int getPDBIdColumIndex(Collection<PDBDocField> wantedFeilds, boolean hasRefSeq) {

		// If a reference sequence is attached then start counting from 1 else
		// start from zero
		int pdbFeildIndexCounter = hasRefSeq ? 1 : 0;

		for (PDBDocField feild : wantedFeilds) {
			if (feild.equals(PDBDocField.PDB_ID)) {
				break; // Once PDB Id index is determined exit iteration
			}
			++pdbFeildIndexCounter;
		}
		return pdbFeildIndexCounter;
	}

	/**
	 * Represents the fields available in the PDB API JSON response
	 *
	 */
	public enum PDBDocField {
		PDB_ID("PDB Id", "pdb_id"),
		TITLE("Title", "title"),
		MOLECULE_NAME("Molecule", "molecule_name"),
		MOLECULE_TYPE("Molecule Type", "molecule_type"),
		MOLECULE_SEQUENCE("Sequence", "molecule_sequence"),
		PFAM_ACCESSION("PFAM Accession", "pfam_accession"),
		PFAM_NAME("PFAM Name", "pfam_name"),
		INTERPRO_NAME("InterPro Name", "interpro_name"),
		INTERPRO_ACCESSION("InterPro Accession", "interpro_accession"),
		UNIPROT_ID("UniProt Id", "uniprot_id"),
		UNIPROT_ACCESSION("UniProt Accession", "uniprot_accession"),
		UNIPROT_COVERAGE("UniProt Coverage", "uniprot_coverage"),
		UNIPROT_FEATURES("Uniprot Features", "uniprot_features"),
		R_FACTOR("R Factor", "r_factor"),
		RESOLUTION("Resolution", "resolution"),
		DATA_QUALITY("Data Quality", "data_quality"),
		OVERALL_QUALITY("Overall Quality", "overall_quality"),
		POLYMER_COUNT("Number of Polymers", "number_of_polymers"),
		PROTEIN_CHAIN_COUNT("Number of Protein Chains", "number_of_protein_chains"),
		BOUND_MOLECULE_COUNT("Number of Bound Molecule", "number_of_bound_molecules"),
		POLYMER_RESIDUE_COUNT("Number of Polymer Residue", "number_of_polymer_residues"),
		GENUS("GENUS", "genus"),
		GENE_NAME("Gene Name", "gene_name"),
		EXPERIMENTAL_METHOD("Experimental Method", "experimental_method"),
		GO_ID("GO Id", "go_id"),
		ASSEMBLY_ID("Assembly Id", "assembly_form"),
		ASSEMBLY_FORM("Assembly Form", "assembly_id"),
		ASSEMBLY_TYPE("Assembly Type", "assembly_type"),
		SPACE_GROUP("Space Group", "spacegroup"),
		CATH_CODE("Cath Code", "cath_code"),
		TAX_ID("Tax Id", "tax_id"),
		TAX_QUERY("Tax Query", "tax_query"),
		INTERACTING_ENTRY_ID("Interacting Entry Id", "interacting_entry_id"),
		INTERACTING_ENTITY_ID("Interacting Entity Id", "interacting_entity_id"),
		INTERACTING_MOLECULES("Interacting Molecules", "interacting_molecules"),
		PUBMED_ID("Pubmed Id", "pubmed_id"),
		STATUS("Status", "status"),
		MODEL_QUALITY("Model Quality", "model_quality"),
		PIVOT_RESOLUTION("Pivot Resolution", "pivot_resolution"),
		DATA_REDUCTION_SOFTWARE("Data reduction software", "data_reduction_software"),
		MAX_OBSERVED_RES("Max observed residues", "max_observed_residues"),
		ORG_SCI_NAME("Organism scientific name", "organism_scientific_name"),
		SUPER_KINGDOM("Super kingdom", "superkingdom"),
		RANK("Rank", "rank"),
		CRYSTALLISATION_PH("Crystallisation Ph", "crystallisation_ph"),
		BIOLOGICAL_FUNCTION("Biological Function", "biological_function"),
		BIOLOGICAL_PROCESS("Biological Process", "biological_process"),
		BIOLOGICAL_CELL_COMPONENT("Biological Cell Component", "biological_cell_component"),
		COMPOUND_NAME("Compound Name", "compound_name"),
		COMPOUND_ID("Compound Id", "compound_id"),
		COMPOUND_WEIGHT("Compound Weight", "compound_weight"),
		COMPOUND_SYSTEMATIC_NAME("Compound Systematic Name", "compound_systematic_name"),
		INTERACTING_LIG("Interacting Ligands", "interacting_ligands"),
		JOURNAL("Journal", "journal"),
		ALL_AUTHORS("All Authors", "all_authors"),
		EXPERIMENTAL_DATA_AVAILABLE("Experiment Data Available", "experiment_data_available"),
		DIFFRACTION_PROTOCOL("Diffraction Protocol", "diffraction_protocol"),
		REFINEMENT_SOFTWARE("Refinement Software", "refinement_software"),
		STRUCTURE_DETERMINATION_METHOD("Structure Determination Method", "structure_determination_method"),
		SYNCHROTON_SITE("Synchrotron Site", "synchrotron_site"),
		SAMPLE_PREP_METHOD("Sample Preparation Method", "sample_preparation_method"),
		ENTRY_AUTHORS("Entry Authors", "entry_authors"),
		CITATION_TITLE("Citation Title", "citation_title"),
		STRUCTURE_SOLUTION_SOFTWARE("Structure Solution Software", "structure_solution_software"),
		ENTRY_ENTITY("Entry Entity", "entry_entity"),
		R_FREE("R Free", "r_free"),
		NO_OF_POLYMER_ENTITIES("Number of Polymer Entities", "number_of_polymer_entities"),
		NO_OF_BOUND_ENTITIES("Number of Bound Entities", "number_of_bound_entities"),
		CRYSTALLISATION_RESERVOIR("Crystallisation Reservoir", "crystallisation_reservoir"),
		DATA_SCALING_SW("Data Scalling Software", "data_scaling_software"),
		DETECTOR("Detector", "detector"),
		DETECTOR_TYPE("Detector Type", "detector_type"),
		MODIFIED_RESIDUE_FLAG("Modified Residue Flag", "modified_residue_flag"),
		NUMBER_OF_COPIES("Number of Copies", "number_of_copies"),
		STRUCT_ASYM_ID("Struc Asym Id", "struct_asym_id"),
		HOMOLOGUS_PDB_ENTITY_ID("Homologus PDB Entity Id", "homologus_pdb_entity_id"),
		MOLECULE_SYNONYM("Molecule Synonym", "molecule_synonym"),
		DEPOSITION_SITE("Deposition Site", "deposition_site"),
		SYNCHROTRON_BEAMLINE("Synchrotron Beamline", "synchrotron_beamline"),
		ENTITY_ID("Entity Id", "entity_id"),
		BEAM_SOURCE_NAME("Beam Source Name", "beam_source_name"),
		PROCESSING_SITE("Processing Site", "processing_site"),
		ENTITY_WEIGHT("Entity Weight", "entity_weight"),
		VERSION("Version", "_version_"),
		ALL("ALL", "text");

		private String name;

		private String code;

		PDBDocField(String name, String code) {
			this.name = name;
			this.code = code;
		}

		public String getName() {
			return name;
		}

		public String getCode() {
			return code;
		}

		public String toString() {
			return name;
		}
	}
}