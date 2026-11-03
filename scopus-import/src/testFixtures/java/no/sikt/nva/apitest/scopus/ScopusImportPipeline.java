package no.sikt.nva.apitest.scopus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.apitest.base.MessageQueues;
import no.sikt.nva.apitest.base.S3Storage;

/** Bucket names are the template defaults in nva-publication-api, suffixed with the account id. */
public final class ScopusImportPipeline {

  private static final String XML_BUCKET_PREFIX = "scopus-xml-files-v2-";
  private static final String ZIP_BUCKET_PREFIX = "scopus-zip-files-v2-";
  private static final String REPORT_BUCKET_PREFIX = "scopus-import-reports-";
  private static final String IMPORT_QUEUE_LOGICAL_ID = "ScopusImportQueue";
  private static final String URI_ATTRIBUTE = "uri";
  private static final String ZIP_SUFFIX = ".zip";

  private ScopusImportPipeline() {}

  /** Does what the unzipper does per record. The upload alone triggers nothing. */
  public static void submitRecord(ScopusRecord scopusRecord) {
    var xmlBucket = bucketName(XML_BUCKET_PREFIX);
    S3Storage.putTextObject(xmlBucket, scopusRecord.fileName(), scopusRecord.toXml());
    MessageQueues.sendMessage(
        IMPORT_QUEUE_LOGICAL_ID,
        scopusRecord.fileName(),
        Map.of(URI_ATTRIBUTE, "s3://%s/%s".formatted(xmlBucket, scopusRecord.fileName())));
  }

  public static void submitZip(ScopusRecord scopusRecord) {
    S3Storage.putBytesObject(
        bucketName(ZIP_BUCKET_PREFIX), scopusRecord.eid() + ZIP_SUFFIX, scopusRecord.toZip());
  }

  /** The handler files reports by week, as computed in the Lambda's UTC zone and en_US locale. */
  public static Optional<String> findErrorReport(ScopusRecord scopusRecord, String exceptionName) {
    var now = Instant.now().atZone(ZoneOffset.UTC);
    var key =
        "ERROR/%d-%d/%s/%s"
            .formatted(
                now.get(WeekFields.of(Locale.US).weekOfYear()),
                now.getYear(),
                exceptionName,
                scopusRecord.fileName());
    return S3Storage.findTextObject(bucketName(REPORT_BUCKET_PREFIX), key);
  }

  private static String bucketName(String prefix) {
    return prefix + S3Storage.accountId();
  }
}
