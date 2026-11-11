package no.sikt.nva.apitest.approvals;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import java.net.URI;
import java.nio.file.Path;
import no.sikt.nva.apitest.base.S3Storage;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;

/**
 * Publishes a clinical trial for approvals to point at. Indexing accepts clinical trial sources
 * only, so an approval created from any other source is never searchable. CTIS has no stable test
 * environment, so the tests serve a captured trial document from this environment instead of
 * fetching one from CTIS.
 *
 * <p>The document is stored verbatim as it came from CTIS rather than built from a record, because
 * it is another system's payload: the approval api parses it against its own bundled CTIS context,
 * and a shape invented here would prove nothing about the real one.
 */
public final class ClinicalTrialSource {

  /**
   * An investigator of the trial document, as the index records them. Searching for a person is
   * searching for this one, so the values have to stay the ones in the document.
   */
  public static final String INVESTIGATOR_PERSON_ID = "23297";

  public static final URI INVESTIGATOR_PERSON_URI =
      URI.create("https://api.e2e.nva.aws.unit.no/cristin/person/" + INVESTIGATOR_PERSON_ID);

  private static final String FIXTURE_BUCKET_PREFIX = "approval-source-fixtures-";
  private static final String TRIAL_DOCUMENT_FILE = "clinical-trial.json";
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String PUBLIC_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

  private ClinicalTrialSource() {}

  /**
   * Publishes the trial document and returns the url an approval points at as its source. The
   * content type is set explicitly, since the approval api records what the response declares and
   * S3 otherwise serves the document as binary data.
   */
  public static String publishClinicalTrial() {
    var bucketName = fixtureBucket();
    S3Storage.putTextObject(bucketName, TRIAL_DOCUMENT_FILE, trialDocument(), JSON_LD_MEDIA_TYPE);
    return PUBLIC_URL_FORMAT.formatted(
        bucketName, getRegion(new Environment()), TRIAL_DOCUMENT_FILE);
  }

  private static String fixtureBucket() {
    return FIXTURE_BUCKET_PREFIX + S3Storage.accountId();
  }

  private static String trialDocument() {
    return IoUtils.stringFromResources(Path.of(TRIAL_DOCUMENT_FILE));
  }
}
