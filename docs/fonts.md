# Fonts

The design calls for three faces, the same three the web app uses:

| Role | Face | Licence |
|---|---|---|
| Display | Fraunces | OFL |
| Body | Newsreader | OFL |
| Label, stamps | Cutive Mono | OFL |

They are **bundled as assets**, not fetched through the Google Fonts provider:
no Play Services dependency, works offline, and no flash of fallback type on a
cold start. All three are OFL, so redistribution is clear. Cost is roughly
300–500 KB of APK.

`LedgerType` currently resolves to the platform's serif and monospace. Every
size, weight and letter-spacing is already correct, so the app ships and looks
right in shape; adding the real faces is a drop-in.

## Adding them

1. Download the OFL files from Google Fonts.
2. **Subset aggressively** — the app needs Latin only, and Fraunces is a
   variable font whose full axis range is not used. Only `opsz`, `wght` and
   `SOFT` are referenced by the web design.
3. Put them in `app/src/main/res/font/` with lowercase, underscore-only names:
   `fraunces_regular.ttf`, `newsreader_regular.ttf`, `cutive_mono_regular.ttf`.
4. In `ui/ledger/Type.kt`, replace the three `FontFamily` assignments:

```kotlin
    val display = FontFamily(Font(R.font.fraunces_regular))
    val body = FontFamily(Font(R.font.newsreader_regular))
    val mono = FontFamily(Font(R.font.cutive_mono_regular))
```

Nothing else changes — every screen reads its face from `LedgerType`.
