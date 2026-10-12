package no.sikt.nva.apitest.scientificindex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Approval(
    String type,
    String institutionId,
    String status,
    float points,
    String assignee,
    String finalizedBy,
    String finalizedDate) {}
