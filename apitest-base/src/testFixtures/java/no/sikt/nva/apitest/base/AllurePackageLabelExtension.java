package no.sikt.nva.apitest.base;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Rewrites the Allure package label from the fully qualified class name (the allure-junit5 default)
 * to the actual Java package, so the report can group tests by package.
 */
public class AllurePackageLabelExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    var packageName = context.getRequiredTestClass().getPackageName();
    Allure.getLifecycle()
        .updateTestCase(
            result -> {
              result.getLabels().removeIf(label -> "package".equals(label.getName()));
              result.getLabels().add(new Label().setName("package").setValue(packageName));
            });
  }
}
