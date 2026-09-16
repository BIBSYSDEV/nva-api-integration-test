package no.sikt.nva.apitest.project;

import java.util.Map;

public record Project(String projectIdentifier, Map<String, Object> payload) {}
