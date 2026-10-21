package no.sikt.nva.apitest.scopus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.apitest.base.MessageQueues;
import no.sikt.nva.apitest.base.S3Storage;

/**
 * The entry points of the Scopus import in nva-publication-api, which has no REST API for it. The
 * bucket names mirror the defaults of the ScopusXmlBucketName, ScopusZipBucketName and
 * ScopusImportReportBucketName parameters in its template, which suffix the account id.
 *
 * <p>Prerequisites: the test runner role has s3:PutObject on the XML and zip buckets,
 * sqs:ListQueues and sqs:SendMessage on ScopusImportQueue, and s3:GetObject and s3:ListBucket on
 * the report bucket.
 */
final class ScopusImportPipeline {

  private static final String XML_BUCKET_PREFIX = "scopus-xml-files-v2-";
  private static final String ZIP_BUCKET_PREFIX = "scopus-zip-files-v2-";
  private static final String REPORT_BUCKET_PREFIX = "scopus-import-reports-";
  private static final String IMPORT_QUEUE_LOGICAL_ID = "ScopusImportQueue";
  private static final String URI_ATTRIBUTE = "uri";
  private static final String ZIP_SUFFIX = ".zip";
  private static final String ERROR_REPORT_KEY = "ERROR/%d-%d/%s/%s";

  private ScopusImportPipeline() {}

  /**
   * Does what the unzipper lambda does for each record: writes the XML to the XML bucket under its
   * file name and sends the file name to the import queue, with the S3 URI in the uri attribute.
   * The upload alone triggers nothing, the queue message is what starts the import.
   */
  static void submitRecord(ScopusRecord scopusRecord) {
    var xmlBucket = xmlBucketName();
    S3Storage.putTextObject(xmlBucket, scopusRecord.fileName(), scopusRecord.toXml());
    MessageQueues.sendMessage(
        IMPORT_QUEUE_LOGICAL_ID,
        scopusRecord.fileName(),
        Map.of(URI_ATTRIBUTE, "s3://%s/%s".formatted(xmlBucket, scopusRecord.fileName())));
  }

  /** Uploads the record as a single-entry zip, which triggers the unzipper lambda. */
  static void submitZip(ScopusRecord scopusRecord) {
    S3Storage.putBytesObject(
        ZIP_BUCKET_PREFIX + S3Storage.accountId(),
        scopusRecord.eid() + ZIP_SUFFIX,
        scopusRecord.toZip());
  }

  /**
   * The error report the handler writes when the import of a record fails. The handler files it
   * under the week and year of the failure, computed in the lambda's default zone and locale, which
   * are UTC and en_US in the Java Lambda runtime.
   */
  static Optional<String> findErrorReport(ScopusRecord scopusRecord, String exceptionName) {
    var now = Instant.now().atZone(ZoneOffset.UTC);
    var key =
        ERROR_REPORT_KEY.formatted(
            now.get(WeekFields.of(Locale.US).weekOfYear()),
            now.getYear(),
            exceptionName,
            scopusRecord.fileName());
    return S3Storage.findTextObject(REPORT_BUCKET_PREFIX + S3Storage.accountId(), key);
  }

  private static String xmlBucketName() {
    return XML_BUCKET_PREFIX + S3Storage.accountId();
  }
}
