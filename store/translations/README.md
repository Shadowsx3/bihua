# Store listing translations

Play Console can translate the store listing with AI. Treat the output as a draft:

- The app UI is currently English-only. A translated listing that opens an English app can disappoint users.
- Bihua teaches Chinese, Japanese, and Korean. A mistranslated character term, reading, or claim is especially visible to those audiences.
- Do not publish machine translations without a native-speaker check.
- Use the 12 closed testers to review any language before it goes live.
- The AI feature does not translate release notes; keep release notes in `<en-US>`.
- The AI feature does not translate the app UI. App-string translation is a separate project.

## Import format

If you import a file, use UTF-8 with a header row:

```csv
"locale","title","short_description","full_description"
"en-US","Bihua: Character Writing","Write Chinese, Japanese, and Korean characters. FSRS-7, offline, no account.","..."
```

Limits:

- title: 30 characters
- short description: 80 characters
- full description: 4,000 characters

`listing-en-US.csv` is the current English row. Keep the source of truth in this folder,
then compare what Play Console saved against the file.

## Recommended first step

For the first production submission, keep the listing in English and skip AI translations.
After the app UI is localized, add one language at a time with a native review.
