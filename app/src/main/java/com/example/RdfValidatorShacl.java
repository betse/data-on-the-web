package com.example;

import org.apache.jena.rdf.model.Model;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.util.FileManager;

import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import java.util.Collection;


public class RdfValidatorShacl {

    public void validate(String dataGraphFilePath, String shapesGraphFilePath) {
        // Load the data graph
        Model dataModel = ModelFactory.createDefaultModel();
        try {
            FileManager.get().readModel(dataModel, dataGraphFilePath);
            System.out.println("Successfully loaded data graph from: " + dataGraphFilePath);
        } catch (Exception e) {
            System.err.println("Error loading data graph: " + e.getMessage());
            return;
        }

        // Load the shapes graph
        Model shapesModel = ModelFactory.createDefaultModel();
        try {
            FileManager.get().readModel(shapesModel, shapesGraphFilePath, "TTL");
            System.out.println("Successfully loaded shapes graph from: " + shapesGraphFilePath);
        } catch (Exception e) {
            System.err.println("Error loading shapes graph: " + e.getMessage());
            return; // Exit if shapes graph loading fails
        }

        // Create a SHACL validator instance
        ShaclValidator validator = ShaclValidator.get();

        // Perform validation
        System.out.println("Starting SHACL validation...");
        Shapes shapes = Shapes.parse(shapesModel);
        ValidationReport report = validator.validate(shapes, dataModel.getGraph());
        System.out.println("Validation complete.");


        if (report.conforms()) {
            System.out.println(" Data conforms to all SHACL shapes.");
        } else {
            // Get the collection of ReportEntry objects
            Collection<ReportEntry> violations = report.getEntries();
            System.err.printf("Data does NOT conform: %d violation%s found.%n",
                    violations.size(),
                    violations.size() == 1 ? "" : "s");

            int i = 1;
            for (ReportEntry v : violations) {
                System.err.println();
                System.err.printf("── Violation %d ─────────────────────────────────────────%n", i++);
                //System.err.printf("Severity   : %s%n", v.severity().getLocalName());
                System.err.printf("Focus node : %s%n", v.focusNode());
                System.err.printf("Path       : %s%n", v.resultPath());
                System.err.printf("Shape      : %s%n", v.source());
                System.err.printf("Message    : %s%n", v.message());
            }
        }
    }
    public static void main(String[] args) {

        String dataGraphFilePath = "shacl/gdp.ttl";
        String shapesGraphFilePath = "shacl/gdp.shacl";
        RdfValidatorShacl validator = new RdfValidatorShacl();
        validator.validate(dataGraphFilePath, shapesGraphFilePath);
    }
}
