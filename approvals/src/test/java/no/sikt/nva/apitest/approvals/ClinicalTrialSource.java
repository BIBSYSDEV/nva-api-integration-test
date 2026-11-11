package no.sikt.nva.apitest.approvals;

import static no.sikt.nva.apitest.base.ApplicationConstants.getRegion;

import java.net.URI;
import java.nio.file.Path;
import no.sikt.nva.apitest.base.S3Storage;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;

public final class ClinicalTrialSource {

  public static final String INVESTIGATOR_PERSON_IDENTIFIER = "23297";

  public static final URI INVESTIGATOR_PERSON_URI =
      URI.create("https://api.e2e.nva.aws.unit.no/cristin/person/" + INVESTIGATOR_PERSON_IDENTIFIER);

  private static final String FIXTURE_BUCKET_PREFIX = "approval-source-fixtures-";
  private static final String TRIAL_DOCUMENT_FILE = "clinical-trial.json";
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String PUBLIC_URL_FORMAT = "https://%s.s3.%s.amazonaws.com/%s";

  private ClinicalTrialSource() {}

  /**
   * Publishes the trial document and returns the url an approval points at as its source.
   */
  public static URI publishClinicalTrial() {
    var bucketName = fixtureBucket();
    S3Storage.putTextObject(bucketName, TRIAL_DOCUMENT_FILE, trialDocument(), JSON_LD_MEDIA_TYPE);
    return URI.create(PUBLIC_URL_FORMAT.formatted(
        bucketName, getRegion(new Environment()), TRIAL_DOCUMENT_FILE));
  }

  private static String fixtureBucket() {
    return FIXTURE_BUCKET_PREFIX + S3Storage.accountId();
  }

  private static String trialDocument() {
    return IoUtils.stringFromResources(Path.of(TRIAL_DOCUMENT_FILE));
  }
}
