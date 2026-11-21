# Publishing

## Repositories

- Public source: <https://github.com/Shadowsx3/bihua>
- Landing page source: `docs/` in the main repository
- GitHub Pages: <https://shadowsx3.github.io/bihua/>
- Release APK: <https://github.com/Shadowsx3/bihua/releases/download/v1.0.1/bihua-1.0.1.apk>

The source repository is public. GitHub Pages is published from `docs/` on the
main branch. The APK is attached to the repository's GitHub release.

## Signing

The upload key is stored outside the repository:

- Keystore: `~/Documents/bihua-keystore/bihua-upload.jks`
- Alias: `bihua`
- Certificate SHA-256: `2050c59997c50b8faa9bed2b4adbe8b758af19ac88002a190bdd0e121a099b2b`

`keystore.properties` in the project root points Gradle at that key. Both files
are ignored by git. Back up the keystore and its password separately; losing
them means future builds cannot update an installed copy.

## Build and publish

```bash
source scripts/env.sh
make check
make lint
./gradlew :app:assembleRelease
```

The signed APK is `app/build/outputs/apk/release/app-release.apk`.

1. Copy it to the public site pages or attach it to a new GitHub release.
2. Update `docs/download/SHA256SUMS.txt`.
3. Update the version and download URL in `docs/index.html`.
4. Commit the source repository, then commit and push the site pages.
5. Confirm the Pages deployment and the release asset.

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

GitHub Pages is served from `docs/` on the main branch. The old
`bihua-site` repository is archived and redirects to this page.
