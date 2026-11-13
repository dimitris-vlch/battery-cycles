# F-Droid submission preparation

Status: preparation only; not submitted or approved.

## Completed
- Public source repository and Apache-2.0 project license.
- Dependency license references, including sun-security GPL-2.0 Classpath exception.
- Fastlane description, icon, screenshot, changelog and v1.1 source tag.
- Unsigned release build succeeds with JDK 17 and Android SDK 36, without private signing keys.
- Draft build metadata: fdroid/gr.dimitris.batterycycles.yml. The build is deliberately disabled until verified.

- Autonomous wireless pairing verified on 2026-11-13 (Galaxy S23 Ultra, Android 17): clean install, pairing through the notification, cycle reading, and re-reads after app restart.

## Remaining before submission
1. libadb-android 3.1.1 from JitPack includes native SPAKE2 code; prepare or locate an accepted source-build recipe.
2. Conscrypt 2.5.3 includes native code; verify accepted dependency handling or provide a source-build recipe.
3. Verify all transitive dependency licenses and notices, including sun-security-android 1.1.
4. Run fdroid lint, scanner and the Linux F-Droid build environment. A successful ordinary Gradle build is not F-Droid validation.
5. Resolve signing strategy: F-Droid's default signing differs from the GitHub APK; reproducible builds would allow retaining the upstream signature.
6. Open a merge request at https://gitlab.com/fdroid/fdroiddata using a GitLab account. No authenticated GitLab client is configured in this workspace.

Do not bypass scanner checks or label the draft as approved.
