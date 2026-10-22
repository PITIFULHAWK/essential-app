# Implementation Plan: Essential Space DSA & Daily Task Tracker (Nothing OS Edition)

## Overview
A hyper-minimal, distraction-free daily task and DSA tracker designed with the Nothing OS "Essential Space" aesthetic (AMOLED black, NDot dot-matrix typography, monochrome surfaces, signature red accents). Built as a native Flutter Android app targeting the connected Nothing Phone (A059) running Android 17. Features offline-first SQLite storage, a 15-minute inactivity nudge system using exact Android alarms and native notifications, a hybrid natural language snooze system, and dual-provider multimodal AI problem extraction (Google Gemini Free tier and DeepSeek V4.1 Flash) for DSA screenshots and links.

## Architecture Decisions
- **Framework & Native Performance**: Flutter 3.47.7 (Dart AOT native ARM64). Zero JS runtime overhead, 120Hz smooth animations, direct deployment to connected Nothing Phone (`Nothing A059`) via ADB.
- **UI Design System**: Nothing OS "Essential Space" design language: AMOLED pure black (`#000000`), stark white (`#FFFFFF`), subtle dark gray borders (`#1F1F1F`), signature red accents (`#D71921`), NDot dot-matrix streak numbers, and geometric widget cards.
- **Data Persistence & Future Sync**: Local SQLite (`sqflite`) on-device. All tables use UUIDs, `updated_at`, and `is_deleted` soft-delete flags, plus a JSON export/import backup feature to enable seamless future Google Drive sync.
- **15-Minute Inactivity Nudge**: Android `AlarmManager` with exact alarms (`setExactAndAllowWhileIdle`) and `flutter_local_notifications`. Wakes device every 15 minutes of inactivity without battery drain; resets whenever progress is marked. Optional Nothing OS Essential ongoing status bar notification.
- **Hybrid Snooze Engine**: Instant offline regex parser for shorthand snoozes (`45m`, `30 mins`, `2 hours`, `until 5pm`) with fallback to LLM for conversational phrases.
- **Multimodal AI Vision & Failover**: Pluggable AI service supporting Google Gemini (`gemini-1.5-flash` / `gemini-2.0-flash` with Google AI Studio free tier) and DeepSeek (`deepseek-flash` / DeepSeek V4.1 with vision). Adaptive problem extraction (single problem: header + 3 milestone checkboxes; multi-problem: sheet header + checkbox per problem). Automatic failover if primary key hits rate limits.

## Task List

### Phase 1: Foundation & Design System
- [x] Task 1: Flutter Project Initialization & Nothing OS Design System
- [x] Task 2: Local SQLite Database & Sync-Ready Data Schema

### Checkpoint: Foundation
- [x] App builds cleanly without warnings
- [x] SQLite CRUD tests pass
- [x] Design system tokens and NDot typography verify correctly

### Phase 2: Core Vertical Slices
- [x] Task 3: Daily Streak & Overview Dashboard
- [x] Task 4: Dual AI Vision Service (Gemini Free & DeepSeek V4.1 Flash)
- [x] Task 5: DSA Problem Tracker with Multimodal Image/Link Extraction
- [x] Task 6: Course Progress & Job Application Pipeline Trackers

### Checkpoint: Core Features
- [x] End-to-end DSA screenshot upload extracts header + checkboxes
- [x] Course module counters and Job pipeline updates function offline
- [x] Streak counter updates accurately upon task completions

### Phase 3: 15-Minute Inactivity Nudge & Snooze Flow
- [x] Task 7: Background Inactivity Monitor & Native Nothing Notifications
- [x] Task 8: Essential Snooze Bottom-Sheet & Hybrid Natural Language Parser

### Checkpoint: Inactivity & Snooze
- [x] 15-minute inactivity trigger fires native notification
- [x] Quick snooze chips and custom text inputs pause nudges accurately

### Phase 4: Polish, Settings & On-Device Deployment
- [x] Task 9: Settings Screen, Key Management & Sync-Ready JSON Export
- [x] Task 10: End-to-End Testing & Deployment to Connected Nothing Phone (A059)

### Checkpoint: Complete
- [x] All acceptance criteria met
- [x] Verified running live on connected Nothing Phone A059

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| Android 17 / Nothing OS battery killer sleeps background timers | High | Use `AlarmManager.setExactAndAllowWhileIdle()` and native notification channels with high priority, plus optional ongoing Essential status tile. |
| AI API rate limits or network offline during DSA practice | Medium | Offline-first local storage ensures all existing data and manual problem creation work 100% offline. Dual API keys with automatic failover handle rate limits. |
| DeepSeek / Gemini prompt format inconsistencies | Low | Enforce strict JSON schema with client-side JSON parsing and sanitization fallback. |
| Large screenshot sizes exceeding API payload limits | Low | Client-side image compression and resizing before base64 encoding. |

## Open Questions
- None. All requirements settled during grilling rounds.
