# Changelog

## [1.0.1] - 2026-08-16

### Changed
- Modernized Android target to API 36 / Android 16.
- Enforced JDK 17 toolchain for Gradle and AGP compatibility.
- Updated GitHub Actions pipeline to install and build against Android SDK 36.
- Improved release path and signing flow for reproducible APK builds.
- Added project TODO and clearer build documentation.

## [1.0.0-beta1] - 2026-05-09

### Added
- Identity generation (Ed25519 + X25519, BLAKE3 hash)
- QR code display of own identity
- Manual peer hash entry for adding contacts
- Encrypted local message storage (SQLCipher AES-256)
- DHT-based peer discovery via bootstrap server
- mDNS local network discovery
- Multi-transport connection manager (mDNS → DHT with fallback)
- Hardcoded fallback DHT peers (5 nodes, multi-region)
- Text messaging with delivery status
- Bootstrap server with peer announce and seed list API
- Dark theme UI (Jetpack Compose + Material3)

### Technical
- Clean Architecture: `:core:crypto`, `:core:transport`, `:core:database`, `:feature:*`
- Hilt dependency injection
- Room + SQLCipher encrypted database
- GitHub Actions CI/CD (PR checks, release, nightly)

### Known Limitations (beta)
- No push notifications yet
- No audio/video calls yet
- NAT traversal via STUN not yet implemented
- Message queue for offline delivery not yet implemented
