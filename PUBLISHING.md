# Publishing

## Repositories and pages

- Public source: <https://github.com/Shadowsx3/bihua>
- Landing page source: `docs/` in the main repository
- GitHub Pages: <https://shadowsx3.github.io/bihua/>
- Release APK: <https://github.com/Shadowsx3/bihua/releases/latest>
- Play package: `com.bassmd.bihua`
- Play app name: `Bihua: Character Writing`

GitHub Pages is published from `docs/` on the main branch. The APK is attached to
the repository's GitHub release, not committed.

## Signing

The upload key is stored outside the repository:

- Keystore: `~/Documents/bihua-keystore/bihua-upload.jks`
- Alias: `bihua`
- Certificate SHA-256: `2050c59997c50b8faa9bed2b4adbe8b758af19ac88002a190bdd0e121a099b2b`

`keystore.properties` in the project root points Gradle at that key. Both files
are ignored by git. Back up the keystore and its password separately; losing
them means future builds cannot update an installed copy.

## Build

```bash
source scripts/env.sh
make check
make lint
./gradlew :app:assembleRelease :app:bundleRelease
```

Artifacts:

- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- Current version: `1.0.2` (`versionCode 12`)

## Play closed testing

1. Upload the AAB to **Test and release → Testing → Closed testing**.
2. Release name: `1.0.2 closed test`.
3. Release notes: `store/release-notes-1.0.2.txt`, including `<en-US>` tags.
4. Roll out to the existing closed-test track; do not pause it or create a new track.
5. Keep the same 12 testers opted in continuously for 14 days.
6. Store images and listing copy: `store/PLAY-UPLOAD.md`.

Production access is a separate application after the 14-day window. A tester
who leaves and rejoins restarts their own 14-day clock; app updates do not reset
the test for testers who stay opted in.

## GitHub release

1. Push the `v1.0.2` tag.
2. Create a GitHub release from that tag.
3. Upload the APK as `bihua-1.0.2.apk`.
4. The site uses `releases/latest`, so it will point at the newest release.
5. Update the SHA-256 in `docs/index.html` and `docs/download/SHA256SUMS.txt`
   when the local APK is rebuilt.

Do not commit APKs, AABs, keystores, `keystore.properties`, `screenshots/`, or
build output. Those paths are ignored.

## CI

GitHub Actions runs unit tests, the content audit, and lint. It does not assemble
or upload an APK. The full local check is:

```bash
make check
```

## Licensing

- Application code: MIT (`LICENSE`).
- Content database: open-data compilation, with CC BY-SA 4.0 portions and the
  upstream Arphic/LGPL/Unicode terms (`CONTENT-LICENSE.md`).
- Full notices: `THIRD_PARTY_NOTICES.md` and `licenses/`.
- The same notices are bundled in the APK under Settings → About → Licences
  and sources → Full texts.

Hanjaro is intentionally absent. Its site grants no redistribution rights, so
the Korean course uses Unihan only. Korean word examples are not bundled.

## Pages limitation

GitHub Pages is served from `docs/` on the main branch. The old `bihua-site`
repository is archived and redirects to this page.
