# Task List: Essential Space DSA & Daily Task Tracker

## Phase 1: Foundation & Design System

## Task 1: Flutter Project Initialization & Nothing OS Design System
**Description:** Initialize the Flutter mobile application project with Android SDK 37 support. Build the complete Nothing OS "Essential Space" design system including AMOLED pure black theme (`#000000`), stark white accents (`#FFFFFF`), subtle borders (`#1F1F1F`), signature red accents (`#D71921`), custom dot-matrix typography / painter widgets for streak counters and headers, and the bottom tab navigation shell.

**Acceptance criteria:**
- [x] Flutter project compiles cleanly for Android.
- [x] Nothing OS theme tokens defined (`NothingColors`, `NothingTypography`, `NothingCardStyle`).
- [x] Dot-matrix NDot widget component renders customizable dot-matrix digits and text.
- [x] Tab navigation shell switches between: Overview, DSA, Courses, Jobs, and Settings.

**Verification:**
- [x] Tests pass: `flutter test test/design_system_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Tab bar renders in AMOLED black with crisp monochrome active states.

**Dependencies:** None

**Files likely touched:**
- `pubspec.yaml`
- `lib/main.dart`
- `lib/theme/nothing_theme.dart`
- `lib/theme/ndot_matrix.dart`
- `lib/screens/main_shell_screen.dart`
- `test/design_system_test.dart`

**Estimated scope:** Medium: 3-5 files

---

## Task 2: Local SQLite Database & Sync-Ready Schema
**Description:** Set up the offline-first SQLite database using `sqflite` with a sync-ready data model. Every entity uses UUIDs, ISO-8601 timestamps, and `is_deleted` soft-delete flags for future Google Drive sync. Build repository models and CRUD methods for DSA problems, checklist items, courses, jobs, streaks, and app settings.

**Acceptance criteria:**
- [x] Database schema migrations created for `dsa_problems`, `dsa_checklists`, `courses`, `jobs`, `streak_logs`, and `app_settings`.
- [x] CRUD repository operations implemented with UUID generation and timestamps.
- [x] Soft-delete filtering supported on all query methods.
- [x] Unit tests verify end-to-end database creation, insertions, queries, and soft-deletes.

**Verification:**
- [x] Tests pass: `flutter test test/database_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Database initializes cleanly in memory and file system without errors.

**Dependencies:** Task 1

**Files likely touched:**
- `lib/database/app_database.dart`
- `lib/models/dsa_problem.dart`
- `lib/models/course.dart`
- `lib/models/job_application.dart`
- `lib/models/streak_log.dart`
- `test/database_test.dart`

**Estimated scope:** Medium: 3-5 files

---

## Checkpoint: Foundation
- [x] All unit tests pass (`flutter test`)
- [x] Application builds without errors (`flutter analyze`)
- [x] Shell navigation and database initialization verified

---

## Phase 2: Core Vertical Slices

## Task 3: Daily Streak & Overview Dashboard
**Description:** Implement the main "Essential Space" dashboard. Displays the hero dot-matrix streak counter with Nothing red flame badge, today's DSA progress bar (e.g. `03/05 Solved`), live activity status indicator (`12m active` or `Snoozed until 14:30`), and quick-action cards to jump directly into active tasks.

**Acceptance criteria:**
- [x] Streak counter accurately calculates consecutive active days from `streak_logs`.
- [x] Today's DSA progress widget dynamically counts completed questions vs daily goal.
- [x] Status card indicates whether tracking is active, paused, or snoozed.
- [x] High contrast Nothing OS widget cards render with clean monochrome hierarchy.

**Verification:**
- [x] Tests pass: `flutter test test/streak_calculator_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Dashboard updates dynamically when tasks are marked complete.

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `lib/screens/overview_screen.dart`
- `lib/widgets/streak_hero_card.dart`
- `lib/widgets/daily_progress_widget.dart`
- `lib/services/streak_service.dart`
- `test/streak_calculator_test.dart`

**Estimated scope:** Medium: 3-5 files

---

## Task 4: Dual AI Vision Service (Google Gemini Free & DeepSeek V4.1 Flash)
**Description:** Build the AI engine capable of calling Google Gemini (via Google AI Studio free tier) or DeepSeek V4.1 Flash (`deepseek-flash`) for multimodal vision problem extraction. Enforces strict JSON prompt schemas, base64 image compression, provider switching, and automatic failover when a key hits rate limits or network errors.

**Acceptance criteria:**
- [x] Gemini API client formats multimodal requests with inline image data.
- [x] DeepSeek V4.1 client formats chat completion requests with image data URLs.
- [x] Structured prompt extracts: problem title, platform, difficulty, problem summary, and adaptive checkboxes (single-problem milestone checkboxes vs multi-problem list checkboxes).
- [x] Automatic failover executes if the active provider returns HTTP 429 or network failure.

**Verification:**
- [x] Tests pass: `flutter test test/ai_service_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Mock API response correctly deserializes into problem header + checkbox models.

**Dependencies:** Task 2

**Files likely touched:**
- `lib/services/ai/ai_service.dart`
- `lib/services/ai/gemini_provider.dart`
- `lib/services/ai/deepseek_provider.dart`
- `lib/services/ai/ai_extraction_result.dart`
- `test/ai_service_test.dart`

**Estimated scope:** Medium: 3-5 files

---

## Task 5: DSA Problem Tracker with Multimodal Image/Link Extraction
**Description:** Implement the DSA Tracker screen with image capture (camera/gallery) and URL link input. When an image or link is submitted, the AI analyzes it, generates the problem header, and creates the checkboxes underneath. Users can check off boxes as they work, reorder them, or manually add questions.

**Acceptance criteria:**
- [x] Image picker allows selecting screenshots or snapping photos of problems.
- [x] Link input field allows pasting LeetCode / GFG / Codeforces links.
- [x] Problem card displays single header (title, platform, difficulty tag) with checkboxes listed below it.
- [x] Ticking all checkboxes marks the problem complete, updates the daily goal, and records activity to reset the 15-minute inactivity timer.

**Verification:**
- [x] Tests pass: `flutter test test/dsa_screen_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Uploading a problem screenshot populates the card with title and checkboxes.

**Dependencies:** Task 2, Task 4

**Files likely touched:**
- `lib/screens/dsa_screen.dart`
- `lib/widgets/dsa_problem_card.dart`
- `lib/widgets/image_upload_sheet.dart`
- `lib/controllers/dsa_controller.dart`

**Estimated scope:** Medium: 3-5 files

---

## Task 6: Course Progress & Job Application Pipeline Trackers
**Description:** Implement the Courses and Job Applications tabs. The Courses screen tracks course titles, platforms, links, module counters (increment/decrement), and percentage progress bars. The Job Applications screen tracks company names, role links, and application status stages (Saved, Applied, Interviewing, Offer, Rejected) with quick status advance buttons.

**Acceptance criteria:**
- [x] Courses view allows adding courses, incrementing completed modules, and viewing progress bars.
- [x] Job view provides clean Nothing-style cards with company name, job URL, and status pill.
- [x] Status pill allows single-tap stage progression (`Saved` -> `Applied` -> `Interviewing`).
- [x] Updating course or job status logs user activity and resets the inactivity timer.

**Verification:**
- [x] Tests pass: `flutter test test/courses_and_jobs_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Adding a course and advancing a job application persists across app restarts.

**Dependencies:** Task 2

**Files likely touched:**
- `lib/screens/courses_screen.dart`
- `lib/screens/jobs_screen.dart`
- `lib/widgets/course_progress_card.dart`
- `lib/widgets/job_pipeline_card.dart`

**Estimated scope:** Medium: 4 files

---

## Checkpoint: Core Features
- [x] All unit and widget tests pass
- [x] DSA, Courses, and Jobs modules functional offline
- [x] AI extraction creates structured problem headers with checkboxes

---

## Phase 3: 15-Minute Inactivity Nudge & Snooze Flow

## Task 7: Background Inactivity Monitor & Native Nothing Notifications
**Description:** Integrate `flutter_local_notifications` with Android 17 / Nothing OS permissions (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`). Manage the 15-minute inactivity timer: whenever a task is checked, course updated, or note added, `last_activity_time` is updated and an exact alarm is scheduled for 15 minutes later. If 15 minutes elapse without activity, a high-priority Nothing-style notification fires.

**Acceptance criteria:**
- [x] Exact alarms scheduled using `setExactAndAllowWhileIdle()` to survive Nothing OS battery optimization.
- [x] Notification channel configured with high importance, custom vibration, and Nothing OS styling.
- [x] Notification includes direct action buttons (`Mark Progress`, `Snooze 15m`).
- [x] Optional setting to enable an ongoing Essential status bar notification tile.

**Verification:**
- [x] Tests pass: `flutter test test/notification_service_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Notification triggers reliably after timer expiration.

**Dependencies:** Task 2, Task 3

**Files likely touched:**
- `android/app/src/main/AndroidManifest.xml`
- `lib/services/notification_service.dart`
- `lib/services/inactivity_tracker.dart`
- `test/notification_service_test.dart`

**Estimated scope:** Small to Medium: 3 files

---

## Task 8: Essential Snooze Bottom-Sheet & Hybrid Natural Language Parser
**Description:** Build the Nothing-styled Essential snooze sheet accessible from notifications or the in-app status bar. Features quick-snooze chips (`+15m`, `+30m`, `+1h`, `Rest of Day`), a Mark Progress button, and a natural language input field. Uses an instant regex parser for fast shorthand (`45m`, `2 hours`, `until 5pm`) with zero latency, falling back to LLM for conversational text.

**Acceptance criteria:**
- [x] Snooze bottom-sheet displays current inactive duration and active task.
- [x] One-tap preset chips immediately snooze the alarm and update the overview timer.
- [x] Regex parser extracts duration from phrases like `"lunch 45m"`, `"pause 30 mins"`, `"stop for 2 hours"`.
- [x] LLM fallback parses arbitrary complex phrasing if regex fails.
- [x] Snooze state is persisted to SQLite settings and reflected in the UI countdown.

**Verification:**
- [x] Tests pass: `flutter test test/snooze_parser_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Typing "45m" parses to exactly 45 minutes and reschedules the alarm.

**Dependencies:** Task 4, Task 7

**Files likely touched:**
- `lib/widgets/snooze_bottom_sheet.dart`
- `lib/services/snooze_parser.dart`
- `test/snooze_parser_test.dart`

**Estimated scope:** Medium: 3 files

---

## Checkpoint: Inactivity & Snooze
- [x] Inactivity triggers notification after 15m idle
- [x] Snooze sheet opens, parses shorthand and text, and reschedules alarm
- [x] UI reflects snoozed state and timer countdown

---

## Phase 4: Polish, Settings & On-Device Deployment

## Task 9: Settings Screen, Key Management & Sync-Ready JSON Export
**Description:** Build the Settings screen allowing configuration of Google Gemini API key, DeepSeek API key, primary provider toggle, daily DSA target count (e.g. 5 questions/day), and an offline JSON backup/export and import feature (preparing for future Google Drive sync).

**Acceptance criteria:**
- [x] Settings screen allows saving and validating Gemini and DeepSeek API keys.
- [x] Provider selector allows switching active primary model.
- [x] Daily DSA target goal input updates the overview progress calculations.
- [x] Export Data button generates a structured JSON dump of all tables.
- [x] Import Data button safely merges or restores records from a JSON file.

**Verification:**
- [x] Tests pass: `flutter test test/backup_service_test.dart`
- [x] Build succeeds: `flutter analyze`
- [x] Manual check: Settings persist across app restarts; backup file exports clean JSON.

**Dependencies:** Task 2, Task 4

**Files likely touched:**
- `lib/screens/settings_screen.dart`
- `lib/services/backup_service.dart`
- `lib/services/settings_service.dart`
- `test/backup_service_test.dart`

**Estimated scope:** Medium: 4 files

---

## Task 10: Build, Test & Deploy to Connected Nothing Phone (A059)
**Description:** Run full test suite, assemble the debug APK, and install the application directly onto the user's connected physical Nothing Phone (`Nothing A059`) via ADB. Verify all on-device hardware interactions: AMOLED black rendering, NDot typography, camera/gallery permissions, exact alarms, and local notifications.

**Acceptance criteria:**
- [x] All unit and widget tests pass.
- [x] App builds with zero errors for `arm64-v8a`.
- [x] App installs and launches successfully on device `00161353K000348`.
- [x] Full smoke test on device: theme renders in AMOLED black, image picker works, notifications trigger.

**Verification:**
- [x] Tests pass: `flutter test`
- [x] Build succeeds: `cd android && ./gradlew assembleDebug && adb install`
- [x] Manual check: App running live on Nothing Phone.

**Dependencies:** Tasks 1-9

**Files likely touched:**
- `android/app/build.gradle.kts`
- `tasks/todo.md`

**Estimated scope:** Small: 2 files

---

## Checkpoint: Complete
- [x] App fully functional on Nothing Phone (A059)
- [x] All acceptance criteria met
- [x] Ready for user review
