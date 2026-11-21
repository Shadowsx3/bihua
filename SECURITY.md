# Security

## Reporting

Please report security issues privately through GitHub Security Advisories:

<https://github.com/Shadowsx3/bihua/security/advisories/new>

Do not open a public issue for a vulnerability, a leaked key, or a backup file
that contains personal data.

## Scope

Bihua is a local-first Android app. The most relevant areas are:

- backup zip import and restore;
- progress database import;
- local Wi-Fi sync;
- downloaded handwriting and assistant models;
- Android intents and file pickers.

## Handling data

- Never attach a real progress database, backup zip, or personal note to a
  report.
- Redact device identifiers and file paths.
- The upload keystore and `keystore.properties` are not part of the repository.

## Supported versions

Security fixes target the latest released version. The project does not maintain
long-term support branches for old private beta builds.
