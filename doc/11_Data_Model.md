# 11 – Data Model / ERD

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Přehled

SecureWhisper používá **lokální zašifrovanou SQLite databázi** přes SQLCipher s ORM Room. Žádný serverový datový model neexistuje – je čistě P2P. Bootstrap nodes drží pouze in-memory ephemeral data o aktivních peer connections.

## 2. Lokální datový model (SQLCipher / Room)

### 2.1 ER diagram

```
┌──────────────────┐
│    identity      │
│ (singleton)      │
├──────────────────┤
│ id PK            │
│ public_signing   │
│ public_agreement │
│ identity_hash    │
│ created_at       │
└──────────────────┘

┌──────────────────┐         ┌────────────────────┐
│    contacts      │1───────∞│  signal_sessions   │
├──────────────────┤         ├────────────────────┤
│ id PK            │         │ contact_id PK FK   │
│ peer_hash UQ     │         │ session_state BLOB │
│ public_signing   │         │ last_updated       │
│ public_agreement │         └────────────────────┘
│ display_name     │
│ safety_number    │1
│ is_verified      │ │
│ is_blocked       │ │
│ last_seen_at     │ │
│ created_at       │ │
│ notes            │ │
└──────────────────┘ │
       1             │
       │             │
       ∞             ∞
┌──────────────────┐ ┌──────────────────┐
│  conversations   │ │     call_log     │
├──────────────────┤ ├──────────────────┤
│ id PK            │ │ id PK            │
│ contact_id FK    │ │ contact_id FK    │
│ last_message_at  │ │ call_type        │
│ unread_count     │ │ direction        │
│ is_archived      │ │ state            │
│ is_muted_until   │ │ started_at       │
│ disappear_secs   │ │ ended_at         │
└──────────────────┘ │ duration_seconds │
       1             └──────────────────┘
       │
       ∞
┌──────────────────┐
│    messages      │
├──────────────────┤      ┌──────────────────┐
│ id PK            │1────∞│   reactions      │
│ conversation_id  │      ├──────────────────┤
│ sender_is_self   │      │ id PK            │
│ content_type     │      │ message_id FK    │
│ content TEXT     │      │ emoji            │
│ attachment_path  │      │ is_self          │
│ attachment_size  │      │ created_at       │
│ attachment_mime  │      └──────────────────┘
│ reply_to_msg_id  │
│ status           │      ┌──────────────────┐
│ sent_at          │      │   attachments    │
│ delivered_at     │1────1├──────────────────┤
│ read_at          │      │ message_id PK FK │
│ expires_at       │      │ encrypted_path   │
│ is_deleted       │      │ encryption_key   │
└──────────────────┘      │ original_name    │
                          │ mime_type        │
                          │ size_bytes       │
                          │ thumbnail BLOB   │
                          │ download_state   │
                          └──────────────────┘
```

### 2.2 Tabulka `identity`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK CHECK(id=1) | Singleton |
| public_signing_key | BLOB | NOT NULL | Ed25519 public key (32 B) |
| public_agreement_key | BLOB | NOT NULL | X25519 public key (32 B) |
| keystore_alias | TEXT | NOT NULL | Reference do Android Keystore |
| identity_hash | TEXT | NOT NULL UNIQUE | 64-char hex |
| created_at | INTEGER | NOT NULL | Unix epoch ms |

**Poznámka:** Privátní klíče se NEUKLÁDAJÍ v DB – existují pouze v Android Keystore.

### 2.3 Tabulka `contacts`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| peer_hash | TEXT | NOT NULL UNIQUE | 64-char hex peer ID |
| public_signing_key | BLOB | NOT NULL | Ed25519 (32 B) |
| public_agreement_key | BLOB | NOT NULL | X25519 (32 B) |
| display_name | TEXT | | Lokálně přiřazené jméno |
| safety_number | TEXT | NOT NULL | 60-char hex |
| is_verified | INTEGER | DEFAULT 0 | 0/1 (uživatel ověřil) |
| is_blocked | INTEGER | DEFAULT 0 | 0/1 |
| last_seen_at | INTEGER | | Unix ms posledního známého online |
| created_at | INTEGER | NOT NULL | |
| notes | TEXT | | Volitelné poznámky |

**Indexy:**
- `idx_contacts_peer_hash` ON `peer_hash`
- `idx_contacts_blocked` ON `is_blocked` WHERE `is_blocked = 1`

### 2.4 Tabulka `conversations`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| contact_id | INTEGER | NOT NULL FK | → contacts.id |
| last_message_at | INTEGER | | Pro řazení v list |
| unread_count | INTEGER | DEFAULT 0 | |
| is_archived | INTEGER | DEFAULT 0 | |
| is_muted_until | INTEGER | | NULL nebo Unix ms |
| disappearing_messages_seconds | INTEGER | DEFAULT 0 | 0 = vypnuto |

**Indexy:**
- `idx_conversations_last_message` ON `last_message_at DESC`

### 2.5 Tabulka `messages`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| message_uuid | TEXT | NOT NULL UNIQUE | UUID pro idempotenci |
| conversation_id | INTEGER | NOT NULL FK | |
| sender_is_self | INTEGER | NOT NULL | 0=peer, 1=self |
| content_type | TEXT | NOT NULL | TEXT/IMAGE/VIDEO/AUDIO/FILE/REACTION |
| content | TEXT | | Text nebo JSON metadata |
| attachment_path | TEXT | | Lokální cesta |
| attachment_size | INTEGER | | Bytes |
| attachment_mime | TEXT | | MIME type |
| reply_to_message_id | INTEGER | FK | Self-reference, ON DELETE SET NULL |
| status | TEXT | NOT NULL | SENDING/SENT/DELIVERED/READ/FAILED |
| sent_at | INTEGER | NOT NULL | Unix ms |
| delivered_at | INTEGER | | |
| read_at | INTEGER | | |
| expires_at | INTEGER | | Pro disappearing messages |
| is_deleted | INTEGER | DEFAULT 0 | Soft delete |

**Indexy:**
- `idx_messages_conversation` ON `(conversation_id, sent_at DESC)`
- `idx_messages_expires` ON `expires_at` WHERE `expires_at IS NOT NULL`
- `idx_messages_status` ON `status` WHERE `status IN ('SENDING', 'FAILED')`
- `idx_messages_uuid` ON `message_uuid`

### 2.6 Tabulka `attachments`

Oddělená tabulka pro detaily příloh s lazy loading.

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| message_id | INTEGER | PK FK | → messages.id |
| encrypted_path | TEXT | NOT NULL | Cesta k zašifrovanému souboru |
| encryption_key | BLOB | NOT NULL | Per-attachment AES key |
| original_name | TEXT | | |
| mime_type | TEXT | | |
| size_bytes | INTEGER | | |
| thumbnail | BLOB | | Pro images/videos, 200×200 max |
| download_state | TEXT | NOT NULL | PENDING/IN_PROGRESS/COMPLETED/FAILED |
| download_progress | INTEGER | DEFAULT 0 | 0-100 |

### 2.7 Tabulka `signal_sessions`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| contact_id | INTEGER | PK FK | |
| session_state | BLOB | NOT NULL | libsignal serialized state (Double Ratchet) |
| last_updated | INTEGER | NOT NULL | |

### 2.8 Tabulka `call_log`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| contact_id | INTEGER | NOT NULL FK | |
| call_type | TEXT | NOT NULL | AUDIO/VIDEO |
| direction | TEXT | NOT NULL | INCOMING/OUTGOING |
| state | TEXT | NOT NULL | COMPLETED/MISSED/REJECTED/FAILED |
| started_at | INTEGER | NOT NULL | |
| ended_at | INTEGER | | |
| duration_seconds | INTEGER | | |

### 2.9 Tabulka `reactions`

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| message_id | INTEGER | NOT NULL FK | |
| emoji | TEXT | NOT NULL | Unicode emoji |
| is_self | INTEGER | NOT NULL | 0/1 |
| created_at | INTEGER | NOT NULL | |

**Constraint:** `UNIQUE(message_id, emoji, is_self)`

### 2.10 Tabulka `prekey_bundles`

Pro Signal Protocol prekey management.

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| prekey_id | INTEGER | NOT NULL UNIQUE | |
| public_key | BLOB | NOT NULL | |
| private_key_alias | TEXT | NOT NULL | Reference do Keystore |
| is_consumed | INTEGER | DEFAULT 0 | One-time prekeys |
| created_at | INTEGER | NOT NULL | |

### 2.11 Tabulka `pending_messages`

Fronta zpráv k odeslání, když peer není online.

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| id | INTEGER | PK AUTOINCREMENT | |
| message_id | INTEGER | NOT NULL FK | |
| peer_hash | TEXT | NOT NULL | |
| ciphertext | BLOB | NOT NULL | Zašifrovaný payload |
| retry_count | INTEGER | DEFAULT 0 | |
| next_retry_at | INTEGER | NOT NULL | |
| created_at | INTEGER | NOT NULL | |

### 2.12 Tabulka `app_settings`

Key-value store pro user preferences.

| Sloupec | Typ | Constraint | Popis |
|---------|-----|------------|-------|
| key | TEXT | PK | |
| value | TEXT | | JSON pro komplexnější struktury |
| updated_at | INTEGER | NOT NULL | |

**Klíče (příklady):**
- `app_lock_enabled` → "true"/"false"
- `app_lock_method` → "PIN"/"BIOMETRIC"
- `dht_active_when_locked` → "true"/"false"
- `notification_show_preview` → "false" (default)
- `theme_mode` → "DARK"/"LIGHT"/"SYSTEM"

## 3. Migrations strategie

### 3.1 Versioning
- Database version je celé číslo, inicializovaná na 1
- Každá migrace MUSÍ být reverzibilní (migration + downgrade)
- Migrace MUSÍ být testované na production-like data

### 3.2 Migration plan

```kotlin
@Database(
    entities = [
        Identity::class,
        Contact::class,
        Conversation::class,
        Message::class,
        Attachment::class,
        SignalSession::class,
        CallLog::class,
        Reaction::class,
        PreKeyBundle::class,
        PendingMessage::class,
        AppSetting::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() { /* ... */ }
```

## 4. Šifrování dat at-rest

### 4.1 SQLCipher
- AES-256 v CBC módu (default SQLCipher)
- Key derivace: PBKDF2-SHA512 (256 000 iterací) z user passphrase, NEBO Android Keystore klíč

### 4.2 Klíčový materiál
| Data | Místo uložení |
|------|---------------|
| DB encryption key | Android Keystore (StrongBox if available) |
| Identity privátní klíče | Android Keystore (StrongBox) |
| Signal session state | V DB (chráněno SQLCipher) |
| Attachments | Disk, šifrované AES-256-GCM (klíč v DB) |
| Push tokens | EncryptedSharedPreferences |

### 4.3 Mazání
- "Wipe all data" → Drop database, smazat Keystore aliasy, smazat všechny soubory v app dir
- Account migration → Export → Wipe → Import na cílovém zařízení

## 5. Bootstrap node ephemeral state

Bootstrap nodes neukládají persistent data. In-memory pouze:

```python
class PeerState:
    node_id: str          # SHA-256 hash, ne identity hash
    last_seen_ip: str
    last_seen_port: int
    last_announcement: timestamp
    
peer_table: Dict[str, PeerState]   # TTL 5 minut
```

Po výpadku se state znovu zaplní z aktivních peerů.

## 6. Datová politika

### 6.1 Co aplikace ukládá
- Lokálně: zprávy, kontakty, identity, attachments, settings, call log
- FCM server: opaque token (bez vazby na uživatele)
- Bootstrap node: ephemeral connection metadata (≤5 min)

### 6.2 Co aplikace NEUKLÁDÁ
- Žádný analytics, telemetry bez explicit opt-in
- Žádné read receipts ani delivery confirmations na centrálním serveru
- Žádné backups v cloudu by default
- Žádné kontakty z phone book

### 6.3 Right to portability (GDPR Art. 20)
- Settings → Export → Generates encrypted backup
- Backup format: JSON wrapped in age encryption (passphrase-derived)
- Importovatelné v jiné instalaci aplikace
