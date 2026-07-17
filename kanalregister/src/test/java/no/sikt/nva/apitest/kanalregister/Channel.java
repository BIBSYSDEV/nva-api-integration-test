package no.sikt.nva.apitest.kanalregister;

/** A channel fixture with a curl-verified, immutable level for the given year. */
public record Channel(String pid, String name, String level, int year) {}
