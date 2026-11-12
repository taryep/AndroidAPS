# Fast checks while developing

Use this loop while you work. The full CI suite is only for an important commit or a push (see
`CLAUDE.md`, "Verify Before You Push"). Times were measured on the Windows machine on 2026-11-08, for
the `:ui` module, with small changes.

Always use these flags: `--console=plain --no-daemon --configure-on-demand`. Send the output to a log
file and read the log. Do not pipe gradle into `tail`: the pipe hides the exit code.

## Commands

| Goal | Command | Time |
|---|---|---|
| Does the code compile? | `./gradlew.bat :ui:compileAndroidMain` | about 35 s |
| Does the test code compile? | `./gradlew.bat :ui:compileAndroidHostTest` | not timed |
| Run one test class | `./gradlew.bat :ui:testAndroidHostTest --tests "*MyTestClass"` | 1.5 to 3 min |
| Run a group of tests | `./gradlew.bat :ui:testAndroidHostTest --tests "*redesign*"` | about 4 min (first run) |

Example, with the output in a log file:

```
./gradlew.bat --console=plain --no-daemon --configure-on-demand :ui:testAndroidHostTest --tests "*MyTestClass" > "$TEMP/t.log" 2>&1
grep -n "BUILD \|Location:\| FAILED" "$TEMP/t.log"
```

## Notes

- **`:ui:compileDebugKotlin` does not exist.** `:ui` is a Kotlin Multiplatform module. The tasks are
  `compileAndroidMain`, `compileAndroidHostTest` and `testAndroidHostTest`. For another module, list
  its tasks first: `./gradlew.bat --configure-on-demand :<module>:tasks --all`.
- **`--configure-on-demand` skips the "uncommitted changes" check** in `app/build.gradle.kts`
  (`isMaster() && !allCommitted()`). Without it, any build fails while the tree has changes, even a
  `:ui` task, so you would need a commit before every run.
- **Kotlin errors do not start with `e: ` in this build.** They show as `Problem found: Kotlin
  compiler`, with a `Location:` line under it. Grep for `BUILD `, `Location:` and ` FAILED`. The
  `Location:` lines for `OverviewDataCacheImpl.kt` are old warnings, not errors.
- **Check that the tests really ran.** A filter that matches nothing can look like a pass. Open
  `ui/build/test-results/testAndroidHostTest/TEST-<full class name>.xml` and read `tests="N"
  skipped="0" failures="0" errors="0"`.
- **Pictures from screenshot tests** are written under `ui/build/reports/redesign/`. Look at them.
  A test can pass while a text is cut off.
- **Never run two gradle builds at once.** They lock each other and real-time tests become flaky.

## Before a commit or a push

Run the full suite, with `--continue`:

```
./gradlew.bat --console=plain --no-daemon --continue -PfirebaseDisable testFullDebugUnitTest testDebugUnitTest jvmTest testAndroidHostTest :desktop:shell:test
```

It takes more than 30 minutes. When a module far from your change fails (for example a pump module
after a UI change), run that module alone with its own test task. If it passes alone, the first
failure was a flaky test or a busy machine, not your change.
