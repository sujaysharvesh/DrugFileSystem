package com.example.DrugSecure;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DrugClassificationService {
    public Map<String, Boolean> classifyDrugComponents(String content) {
        Map<String, Boolean> classification = new HashMap<>();

        // Simple regex patterns for demonstration
        // In real system, replace with proper ML model
        String[] sensitivePatterns = {
                "fentanyl", "oxycodone", "morphine", "heroin",
                "cocaine", "methamphetamine", "amphetamine",
                "ketamine", "pcp", "ecstasy", "lsd", "mdma",
                "modafinil", "clonazepam", "alprazolam", "zolpidem",
                "gabapentin", "pregabalin", "psilocybin", "dxm",
                "lorazepam", "diazepam", "dextroamphetamine",
                "methylphenidate", "tramadol", "tapentadol"
        };

        String[] nonSensitivePatterns = {
                "aspirin", "ibuprofen", "acetaminophen", "paracetamol",
                "naproxen", "celecoxib", "amoxicillin", "azithromycin",
                "ciprofloxacin", "doxycycline", "metronidazole",
                "loratadine", "cetirizine", "fexofenadine",
                "diphenhydramine", "atorvastatin", "metformin",
                "lisinopril", "levothyroxine", "sertraline",
                "omeprazole", "ranitidine", "loperamide", "vitamin",
                "calcium"
        };


        // Check for sensitive drugs
        for (String pattern : sensitivePatterns) {
            if (content.toLowerCase().contains(pattern)) {
                classification.put(pattern, true);
            }
        }

        // Check for non-sensitive drugs
        for (String pattern : nonSensitivePatterns) {
            if (content.toLowerCase().contains(pattern)) {
                classification.put(pattern, false);
            }
        }

        return classification;
    }
}
