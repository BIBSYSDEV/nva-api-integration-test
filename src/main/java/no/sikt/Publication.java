package no.sikt;

import java.util.HashMap;
import java.util.Map;

public class Publication {

    public enum Category {
        ACADEMIC_ARTICLE
    }
    
    public static Map<String, ?> createTestPublication(String title, Category category) {

        Map<String, ?> testPublication = new HashMap<>();
        
        return testPublication;
    }

}
