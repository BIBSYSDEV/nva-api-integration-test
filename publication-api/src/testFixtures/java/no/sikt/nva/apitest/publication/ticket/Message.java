package no.sikt.nva.apitest.publication.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Message(
    String type,
    String id,
    String identifier,
    String sender,
    String owner,
    String text,
    String status) {}
