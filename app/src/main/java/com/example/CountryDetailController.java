package com.example;

import org.apache.jena.query.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Spring Boot Controller to handle requests for country details using SPARQL.
 * Annotated with @RestController, combining @Controller and @ResponseBody.
 */
@RestController
public class CountryDetailController {

    private static final String SPARQL_ENDPOINT = "http://localhost:3030/ds/query";
    private final String countryDetailSparqlQuery;

    public CountryDetailController() {
        StringBuilder sb = new StringBuilder();
        try {
            ClassPathResource resource = new ClassPathResource("sparql/country_detail.sparql");
            try (InputStream in = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            countryDetailSparqlQuery = sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SPARQL query file", e);
        }
    }

    /**
     * Handles HTTP GET requests to the "/country" path.
     *
     * @param code The country code provided as a request parameter.
     * @return A ResponseEntity containing the HTML content or an error status.
     */
    @GetMapping("/country")
    public ResponseEntity<String> getCountryDetails(@RequestParam(name = "code") String code) {
        // Validate the country code parameter
        if (code == null || code.isEmpty()) {
            // Return a 400 Bad Request status if the code is missing
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country code is required");
        }

        // Replace the placeholder in the SPARQL query with the actual country code
        String finalQueryString = countryDetailSparqlQuery.replace("$CODE", "\"" + code + "\"");

        // Execute the SPARQL query
        Query query = QueryFactory.create(finalQueryString);
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qExec.execSelect();

            // Check if any results were returned
            if (!results.hasNext()) {
                // Return a 404 Not Found status if the country is not found
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found");
            }

            // Process the first result solution
            QuerySolution soln = results.nextSolution();

            // Extract data from the query solution
            String countryName = soln.getLiteral("countryName").getString();
            double gdpValue = soln.getLiteral("gdpValue").getDouble();
            // Check if optional values exist before accessing them
            Double hdiValue = soln.contains("hdiValue") ? soln.getLiteral("hdiValue").getDouble() : null;
            Double internetUsage = soln.contains("internetUsage") ? soln.getLiteral("internetUsage").getDouble() : null;

            // Build the HTML response string with RDFa
            StringBuilder htmlResponse = new StringBuilder();
            htmlResponse.append("<!DOCTYPE html>");
            htmlResponse.append("<html lang=\"en\" prefix=\"schema: http://schema.org/ ex: http://example.org/nswi144/\">");
            htmlResponse.append("<meta charset=\\\"UTF-8\\\"><title>Country Details: \").append(countryName).append(\"</title> <link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">" +
                    "    <style>\n" +
                    "        body { font-family: 'Poppins',Arial, sans-serif; margin: 0; padding: 40px; background-color: #f0f0f0; }\n" +
                    "        .slide { background-color: white; padding: 20px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n" +
                    "        h1 { color: #333; }\n" +
                    "        h2 { color: #555; }\n" +
                    "        img { max-width: 100%; height: auto; }\n" +
                    "        ul { list-style-type: disc; padding-left: 20px; }\n" +
                    "    </style></head>");
            htmlResponse.append("<body>");
            htmlResponse.append("<h1>Country Details: <span property=\"schema:name\">").append(countryName).append("</span></h1>");
            htmlResponse.append("<p><a href=\"/countries\">Back to Countries List</a></p>");
            htmlResponse.append("<div typeof=\"schema:Country\">");
            htmlResponse.append("<p><strong>GDP (2020):</strong> <span property=\"ex:gdpValue\">").append(gdpValue).append("</span> USD</p>");

            if (hdiValue != null) {
                htmlResponse.append("<p><strong>HDI (2020):</strong> <span property=\"ex:hdiValue\">").append(hdiValue).append("</span></p>");
            } else {
                htmlResponse.append("<p><strong>HDI (2020):</strong> Not available</p>");
            }

            if (internetUsage != null) {
                htmlResponse.append("<p><strong>Internet Usage (2020):</strong> <span property=\"ex:internetUsage\">").append(internetUsage).append("</span>%</p>");
            } else {
                htmlResponse.append("<p><strong>Internet Usage (2020):</strong> Not available</p>");
            }

            htmlResponse.append("</div>");
            htmlResponse.append("</body>");
            htmlResponse.append("</html>");

            // Return the generated HTML with a 200 OK status
            return ResponseEntity.ok(htmlResponse.toString());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching country details", e);
        }
    }
}
