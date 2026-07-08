package no.sikt.nva.apitest.search.resources.jsonld;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import no.sikt.nva.apitest.search.SearchTestBase;

abstract class JsonLdTestBase extends SearchTestBase {

  protected static final String APPLICATION_LD_JSON = "application/ld+json";
  protected static final String RESOURCES_PATH = "/search/resources";

  protected static final String NUMBER_OF_ITEMS_POINTER = "numberOfItems";
  protected static final String ITEM_LIST_ELEMENT_POINTER = "itemListElement";

  protected static JsonPath itemList(Response response) {
    return JsonPath.from(response.body().asString());
  }
}
