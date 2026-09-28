package no.sikt.nva.apitest.publication.file;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;

/** The small text file that tests upload whenever the content of the file does not matter. */
public final class ExampleFile {

  public static final String NAME = "example.txt";
  public static final String MIME_TYPE = "text/plain";

  private static final String FILE_CONTENT = readContent();

  private ExampleFile() {}

  public static String content() {
    return FILE_CONTENT;
  }

  public static int sizeInBytes() {
    return FILE_CONTENT.getBytes(UTF_8).length;
  }

  private static String readContent() {
    try (var resourceStream = ExampleFile.class.getResourceAsStream("/" + NAME)) {
      return new String(resourceStream.readAllBytes(), UTF_8);
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not read file " + NAME, exception);
    }
  }
}
