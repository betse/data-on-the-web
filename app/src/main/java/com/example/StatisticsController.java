package com.example;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Spring Boot Controller to handle requests for statistics using SPARQL CONSTRUCT query.
 */
@RestController
public class StatisticsController {

    private static final String SPARQL_ENDPOINT = "http://localhost:3030/ds/query";
    private final String combinedGdpHdiSparqlQuery;

    public StatisticsController() {
        // load the SPARQL from classpath as before...
        StringBuilder sb = new StringBuilder();
        try ( InputStream in = new ClassPathResource("sparql/combined_gdp_hdi.sparql")
                .getInputStream();
              BufferedReader rdr = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)) ) {
            String line;
            while ((line = rdr.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load SPARQL query file", e);
        }
        this.combinedGdpHdiSparqlQuery = sb.toString();
    }

    @GetMapping(value = "/statistics", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getStatistics() {
        Model model = ModelFactory.createDefaultModel();
        Query query = QueryFactory.create(combinedGdpHdiSparqlQuery);
        try ( QueryExecution qExec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query) ) {
            qExec.execConstruct(model);
        } catch (Exception e) {
            throw new ResponseStatusException(500, "Error fetching statistics", e);
        }

        String EX = "http://example.org/nswi144/";
        Property hasCombined = model.createProperty(EX + "hasCombinedData");
        Property yearProp    = model.createProperty(EX + "year");
        Property gdpProp     = model.createProperty(EX + "gdpValue");
        Property hdiProp     = model.createProperty(EX + "hdiValue");
        Property labelProp   = SKOS.prefLabel;  // == "http://www.w3.org/2004/02/skos/core#prefLabel"

        Map<String, Map<String, Double>> stats = new LinkedHashMap<>();

        // For every country node that has a combined-data link...
        model.listSubjectsWithProperty(hasCombined)
                .forEachRemaining(countryRes -> {
                    // Look at each combined-resource it points to
                    countryRes.listProperties(hasCombined)
                            .mapWith(Statement::getObject)
                            .filterKeep(RDFNode::isResource)
                            .mapWith(RDFNode::asResource)
                            .forEachRemaining(newObs -> {
                                Statement yearStmt = newObs.getProperty(yearProp);
                                if (yearStmt != null &&
                                        "2020".equals(yearStmt.getLiteral().getLexicalForm())) {

                                    // read label (fallback to country code if missing)
                                    Statement lblStmt = newObs.getProperty(labelProp);
                                    String label = lblStmt != null
                                            ? lblStmt.getLiteral().getString()
                                            : countryRes.getLocalName();

                                    // read the numeric values
                                    Statement gdpStmt = newObs.getProperty(gdpProp);
                                    Statement hdiStmt = newObs.getProperty(hdiProp);
                                    double gdp = gdpStmt != null ? gdpStmt.getDouble() : Double.NaN;
                                    double hdi = hdiStmt != null ? hdiStmt.getDouble() : Double.NaN;

                                    Map<String, Double> values = new HashMap<>();
                                    if (!Double.isNaN(gdp)) values.put("gdp", gdp);
                                    if (!Double.isNaN(hdi)) values.put("hdi", hdi);

                                    stats.put(label, values);
                                }
                            });
                });

        // 4) Build the HTML table
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html lang=\"en\" prefix=\"schema: http://schema.org/ ex: http://example.org/nswi144/\">")
                .append("<head><meta charset=\"UTF-8\"><title>Statistics: GDP and HDI (2020)</title>  <link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">" +
                        "<style>\n" +
                        "        body { font-family: 'Poppins',Arial, sans-serif; margin: 0; padding: 40px; background-color: #f0f0f0; }\n" +
                        "        .slide { background-color: white; padding: 20px; margin-bottom: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n" +
                        "        h1 { color: #333; }\n" +
                        "        h2 { color: #555; }\n" +
                        "        img { max-width: 100%; height: auto; }\n" +
                        "        ul { list-style-type: disc; padding-left: 20px; }\n" +
                        "    </style></head>")

                .append("<body>")
                .append("<h1>Statistics: GDP and HDI (2020)</h1>")
                .append("<p><a href=\"/\">Back to Home</a></p>")
                .append("<table style=\"border:none\">")
                .append("<tr><th>Country</th><th>GDP (USD)</th><th style=\"padding-left:70px\">HDI</th></tr>");

        stats.forEach((country, vals) -> {
            html.append("<tr typeof=\"schema:Country\">")
                    .append("<td property=\"schema:name\">").append(country).append("</td>")
                    .append("<td property=\"ex:gdpValue\" style=\"padding-left:70px\">")
                    .append(vals.getOrDefault("gdp", Double.NaN))
                    .append("</td>")
                    .append("<td property=\"ex:hdiValue\" style=\"padding-left:70px\">")
                    .append(vals.getOrDefault("hdi", Double.NaN))
                    .append("</td>")
                    .append("</tr>");
        });

        html.append("</table></body></html>");

        return ResponseEntity.ok(html.toString());
    }
}
