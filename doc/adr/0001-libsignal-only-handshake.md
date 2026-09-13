# ADR-0001: Drop the separate Noise XX handshake layer, use libsignal X3DH only

## Status
Accepted — 2026-09-13

## Context
`CLAUDE.md` B.1 and `doc/14_Security_Privacy.md` §2.1 specified two separate crypto layers:
a Noise XX handshake at the transport level, and Signal Protocol (X3DH + Double Ratchet) at
the messaging level. Neither was implemented (see `2COM_AUDIT.md`); this ADR covers the
decision made while implementing the fix.

`CLAUDE.md` B.1 explicitly forbids hand-rolling cryptographic primitives or protocols. A
survey of the Java/Android Noise Protocol Framework ecosystem (2026-09-13) found:
- `jchambers/java-noise` — modern, spec-complete, explicitly supports Noise_XX — but its own
  README states it "has not yet been published to any artifact repository."
- `com.github.auties00/noise-java` — the only Noise implementation actually published to
  Maven Central — last released 2021, unmaintained, no evidence of an independent security
  audit.
- No other maintained, audited, Central-published option was found.

Depending on either option means either vendoring an unpublished library (no reproducible
supply chain, `CLAUDE.md` A.6 dependency pinning intent defeated) or shipping a five-year-old
unaudited crypto implementation for the single most safety-critical code path in the app.

## Decision
Do not implement a separate Noise XX layer. Use `org.signal:libsignal-android` /
`org.signal:libsignal-client` exclusively:
- The X3DH key agreement (`PreKeyBundle` exchange carried in a `HELLO` frame at connection
  start) is itself a mutually-authenticated, forward-secret handshake once both sides verify
  the presented identity key against the expected `BLAKE3(signingPub‖agreementPub)` peer
  hash (satisfies FR-14 and invariant B.3 — connection is dropped on mismatch, no fallback).
- The Double Ratchet session established from that handshake covers ongoing message
  encryption and forward secrecy/post-compromise security exactly as `doc/14_Security_Privacy.md`
  §2.4 already describes.

This means the "Transport handshake: Noise XX" row in `doc/14_Security_Privacy.md` §2.1 is
removed/merged rather than implemented literally.

## Consequences
- One fewer third-party crypto dependency, and the one that remains (libsignal) is the most
  heavily audited open-source E2E messaging library in existence — a better fit for
  `CLAUDE.md` B.1's "never implement primitives yourself" intent than adding a second,
  weaker-provenance library.
- Loses the defense-in-depth of two independent handshake layers (if libsignal's X3DH had an
  undiscovered flaw, there is no second Noise-based authentication layer to fall back on).
  Given libsignal's audit history and adoption at massive scale, this is judged an acceptable
  trade for not depending on an unmaintained or unpublished crypto library.
- Follow-up (tracked, not scheduled): revisit adding a Noise XX transport layer if/when a
  maintained, audited, Central-published Java/Kotlin implementation becomes available —
  particularly relevant once the wire protocol moves to the full Protobuf framing described
  in `doc/10_API_Spec.md` (currently deferred, see the versioned-frame note in the
  implementation PR).
