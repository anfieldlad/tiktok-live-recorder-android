# Still Here 1.0

A rebuild of TTL Downloader as *Still Here* — a paper register, selaras with the
web app at app.dioriza.com/stillhere.

## What is new

- **Auto-record.** Place a standing order for a creator who is not live yet; the
  server watches and starts recording the moment they go live. This existed on
  the web only.
- **Three sections**, matching the web: Record live, Auto-record, Save post.
- **Sessions and server settings in one place** — a bottom sheet, not a separate
  screen.
- **Concurrent downloads.** Submissions are queued as jobs on the server, so
  several can be in flight at once.

## What is unchanged

Share-sheet receiving, saving into the phone's gallery, clipboard detection,
local history, and downloads that survive leaving the app.

## Upgrading

This is a **new app** to Android (`com.stillhere.app`), not an update to
`com.ttldownloader.app`. That was a deliberate choice:

- The old app stays installed as a separate icon and must be removed by hand.
- The backend URL, API key and local history do **not** carry over. Re-enter
  them under Sessions.
- Saved media now lands in `Movies/StillHere` and `Pictures/StillHere`. Anything
  already in the old folders stays where it is.

## This release retires two server shims

Both exist only because the previous app is in production. Once 1.0 is
installed, delete them — the triggers are recorded in `SSH.md` in the server
repo:

1. **The `/tiktok` nginx prefix.** 1.0 defaults to `/stillhere`.
2. **The synchronous download door** (`POST /downloads` with no `?async=1`).
   1.0 submits jobs and polls.

## Still to do before shipping

- **Bundle the three fonts.** `LedgerType` resolves to the platform's serif and
  monospace today; every size and weight is already correct. See `docs/fonts.md`
  — it is a four-line change in one file.
- **Run the instrumented tests on a device**
  (`./gradlew :app:connectedDebugAndroidTest`) and smoke-test the two flows that
  are invisible until someone tries them: sharing a link from TikTok into the
  app, and leaving the app mid-download.
