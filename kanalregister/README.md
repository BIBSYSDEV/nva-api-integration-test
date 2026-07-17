# Channel registry tests

The `kanalregister` module is different from the rest of the suite: it runs contract tests (smoke tests) directly against Kanalregisteret's external nva-api (HKDir), the publication channel register NVA depends on.
This is contract monitoring of a third-party dependency, not regression testing of our own code.
The tests assert the desired contract, identically for HKDir's prod and test environments, which show up as separate groups in the Allure report.

A failing test here means "upstream broke or has a known open bug", and the action is contacting HKDir, never changing the assertions to match broken behavior.

The module targets external hosts and needs no AWS credentials:

```
./gradlew :kanalregister:test
```
