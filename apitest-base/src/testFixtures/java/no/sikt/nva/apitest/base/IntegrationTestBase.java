package no.sikt.nva.apitest.base;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeAll;

public abstract class IntegrationTestBase {

  /**
   * The only account these tests are allowed to run in. Nothing else pins them to an environment:
   * the api domain, the test user password and every bucket, table and lambda they reach are
   * resolved from whichever account the credentials happen to belong to.
   */
  private static final String E2E_ACCOUNT_ID = "282305091481";

  private static final String WRONG_ACCOUNT_MESSAGE =
      """
      Refusing to run: the AWS credentials belong to account %s, but these tests only run in \
      the e2e account %s. They create, update and delete data, and some of them delete straight \
      from DynamoDB without passing an authorization check.
      Pass -PawsProfile=<e2e profile>. Without it the AWS_PROFILE environment variable decides \
      which account the tests hit.\
      """;

  private static final ReentrantLock CONFIGURATION_LOCK = new ReentrantLock();
  private static boolean restAssuredConfigured;

  // A static initializer rather than @BeforeAll, because a subclass may delete or create data from
  // its own @BeforeAll or from a static field initializer, and both of those run after this one:
  // the JVM initializes a superclass before the subclass that extends it.
  static {
    requireE2eAccount();
  }

  private static void requireE2eAccount() {
    var accountId = AwsAccount.accountId();
    if (!E2E_ACCOUNT_ID.equals(accountId)) {
      throw new IllegalStateException(WRONG_ACCOUNT_MESSAGE.formatted(accountId, E2E_ACCOUNT_ID));
    }
  }

  // Every test class inherits this @BeforeAll, but it only mutates global RestAssured state to the
  // same values. With test classes running concurrently, re-running it would race the config and
  // filter reassignment against in-flight requests from other classes, so it runs exactly once.
  @BeforeAll
  static void configureRestAssured() {
    CONFIGURATION_LOCK.lock();
    try {
      if (!restAssuredConfigured) {
        RestAssured.baseURI =
            "https://" + CognitoLogin.getValueFromParameterStore("/NVA/ApiDomain");
        RestAssured.replaceFiltersWith(
            new AllureRestAssured()
                .setRequestTemplate("sanitized-http-request.ftl")
                .setResponseTemplate("sanitized-http-response.ftl"),
            new RequestLogger());
        var logConfig =
            LogConfig.logConfig()
                .enableLoggingOfRequestAndResponseIfValidationFails()
                .blacklistHeaders(List.of("Authorization"));
        RestAssured.config = RestAssured.config().logConfig(logConfig);
        restAssuredConfigured = true;
      }
    } finally {
      CONFIGURATION_LOCK.unlock();
    }
  }

  protected static boolean isNotConflict(Response response) {
    return response.statusCode() != HttpURLConnection.HTTP_CONFLICT;
  }
}
