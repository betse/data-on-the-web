package com.example;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal; // Import Literal
import org.apache.jena.rdf.model.RDFNode; // Import RDFNode
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Spring Boot Controller to handle requests for listing countries using SPARQL.
 * Annotated with @RestController, combining @Controller and @ResponseBody.
 */
@RestController
public class CountryListController {
    private static final String SPARQL_ENDPOINT = "http://localhost:3030/ds/query";

    private final String listCountriesSparqlQuery;

    public CountryListController() {
        // Load the SPARQL query from the file during controller initialization
        StringBuilder sb = new StringBuilder();
        try {
            ClassPathResource resource = new ClassPathResource("sparql/list_countries.sparql");
            try (InputStream in = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            listCountriesSparqlQuery = sb.toString();
        } catch (IOException e) {
            // Handle the case where the SPARQL file cannot be loaded
            throw new RuntimeException("Failed to load SPARQL query file", e);
        }
    }

    /**
     * Handles HTTP GET requests to the "/countries" path.
     * Executes a SPARQL SELECT query to get a list of countries and renders it as an HTML table.
     *
     * @return A ResponseEntity containing the HTML content or an error status.
     */
    @GetMapping("/countries")
    public ResponseEntity<String> listCountries() {

        // Execute the SPARQL SELECT query
        Query query = QueryFactory.create(listCountriesSparqlQuery);

        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qExec.execSelect();

            // Build the HTML response string with RDFa
            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<!DOCTYPE html>");
            htmlResponse.append("<html lang=\"en\" prefix=\"schema: http://schema.org/ ex: http://example.org/nswi144/\">"); // Added ex: prefix
            htmlResponse.append("<head><meta charset=\"UTF-8\"><title>Countries List</title> <link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">" +
                    "    <style>\n" +
                    "        body { font-family: 'Poppins',Arial, sans-serif; margin: 0; padding: 40px; background-color: #f0f0f0; }\n" +
                    "        .slide { background-color: white; padding: 20px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n" +
                    "        h1 { color: #333; }\n" +
                    "        h2 { color: #555; }\n" +
                    "        img { max-width: 100%; height: auto; }\n" +
                    "        ul { list-style-type: disc; padding-left: 20px; }\n" +
                    "    </style></head>");
            htmlResponse.append("<body>");
            htmlResponse.append("<h1>Countries List</h1>");
            // Link to the statistics page
            htmlResponse.append("<p><a href=\"/statistics\">View Statistics</a></p>");
            // htmlResponse.append("<p><a href=\"/\">Back to Home</a></p>");

            // Start the HTML table
            htmlResponse.append("<table style=\"border:none\">");
            htmlResponse.append("<thead>");
            htmlResponse.append("<tr><th>Country (URI)</th><th>Country Label</th><th>Country Code</th></tr>");
            htmlResponse.append("</thead>");
            htmlResponse.append("<tbody>");


            // Iterate through the query results and build table rows
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                // Check if the variables exist before accessing them
                if (soln.contains("country") && soln.contains("countryLabel") && soln.contains("countryCode")) {
                    RDFNode countryNode = soln.get("country"); // Get the country URI
                    String countryUri = countryNode.isURIResource() ? countryNode.asResource().getURI() : "#";

//                    Literal countryLabelLiteral = soln.getLiteral("countryLabel");
//                    System.out.println(soln.get("countryLabel"));
                    //String countryLabelValue = countryLabelLiteral.getString();
                    String countryCode = soln.get("countryCode").toString();

                    String displayedLabel = soln.get("countryLabel").toString();
//                    if (countryLabelLiteral.getLanguage() != null && !countryLabelLiteral.getLanguage().isEmpty()) {
//                        displayedLabel += "@" + countryLabelLiteral.getLanguage();
//                    }


                    // Use RDFa to mark up the table row for the country
                    htmlResponse.append("<tr typeof=\"schema:Country\" resource=\"").append(countryUri).append("\">");

                    // Table cell for Country URI, linking to the detail page using the code
                    htmlResponse.append("<td>");
                    htmlResponse.append("<a href=\"/country?code=").append(countryCode).append("\">").append(countryUri).append("</a>");
                    htmlResponse.append("</td>");

                    // Table cell for Country Label with schema:name property
                    htmlResponse.append("<td property=\"schema:name\"  style=\"padding-left:70px\">").append(displayedLabel).append("</td>");

                    // Table cell for Country Code with ex:countryCode property
                    htmlResponse.append("<td property=\"ex:countryCode\">").append(countryCode).append("</td>");
                    htmlResponse.append("</tr>"); // Close the table row

                } else {
                    // Log a warning or handle cases where expected variables are missing
                    System.err.println("Warning: SPARQL result missing expected variables (country, countryLabel, countryCode)");
                }
            }

            // End the HTML table
            htmlResponse.append("</tbody>");
            htmlResponse.append("</table>");


            htmlResponse.append("</body>");
            htmlResponse.append("</html>");

            // Return the generated HTML with a 200 OK status
            return ResponseEntity.ok(htmlResponse.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching countries list from SPARQL endpoint", e);
        }
    }
}
