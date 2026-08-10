package no.sikt.nva.apitest.scientificindex.candidate;

import static no.sikt.nva.apitest.scientificindex.ScientificIndexPaths.DELETE_NOTE_PATH;

import io.qameta.allure.Description;
import no.sikt.nva.apitest.scientificindex.ScientificIndexTestBase;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("DELETE " + DELETE_NOTE_PATH)
class DeleteNoteTest extends ScientificIndexTestBase {

  // TODO: Add tests

  @Test
  @DisplayName("placeholder")
  @Description(useJavaDoc = true)
  void placeholder(SoftAssertions softly) {
    softly.assertThat(DELETE_NOTE_PATH).isNotBlank();
  }
}
