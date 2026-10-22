# Essential Space

Essential Space is an industrial, minimalist habit and task tracking system inspired by Nothing OS.
It is built for daily DSA problem solving, online bootcamp tracking, job applications, and discipline streaks.

## Project Structure

```
├── android-app/          # 100% Native Android application (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/nothing/essential/
│   │   │   ├── data/             # SQLite database, models, and JSON serialization
│   │   │   ├── service/          # Exact alarms, 15m inactivity nudges, NLP snooze, AI service
│   │   │   ├── ui/               # Compose screens, 5x7 dot matrix, AMOLED components, theme
│   │   │   ├── EssentialApp.kt   # Application lifecycle and notification channel init
│   │   │   └── MainActivity.kt   # Main Activity and tab navigation
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── docs/                 # Architecture Decision Records (ADRs)
├── tasks/                # Specification & task tracking
├── GLOSSARY.md           # Domain model terminology
└── README.md
```

## Features

- **Nothing OS Visual Language**: AMOLED pure black (`#000000`), monochrome high-contrast styling, Nothing Red accents (`#D71921`), and dynamic 5x7 dot-matrix numeric displays.
- **DSA Tracking**: Pre-configured categories for **LeetCode**, **Codeforces** (for 100xDevs Winter Bootcamp 1.0), and **TUF+** (TakeUForward). Checkbox milestone tracking under one header per problem.
- **AI Problem Extraction**: Multimodal vision parser supporting **Google Gemini (Free API)** and **DeepSeek V4.1 Flash (`deepseek-flash`)** to extract problem titles, constraints, and actionable checklist milestones from descriptions or links.
- **15-Minute Inactivity Nudge**: Background exact alarm engine (`AlarmManager.setExactAndAllowWhileIdle`) waking the device every 15 minutes when no progress is recorded.
- **Natural Language Snooze**: Instant pause chips (`+15M`, `+30M`, `+45M`, `+1H`, `REST OF DAY`) and offline NLP parser for natural language inputs (`45m`, `until 5pm`, `eod`, `tomorrow`).
- **Online Courses**: Tracker pre-seeded with **100xDevs Winter Bootcamp 1.0** (module counters and topic progress).
- **Job Pipeline**: Stage tracker (`SAVED`, `APPLIED`, `INTERVIEWING`, `OFFER`, `REJECTED`).
- **Discipline Streak**: Daily logging and consecutive streak calculation.
- **Local SQLite & Backup**: Zero cloud dependencies, with instant JSON export and import to clipboard.

## Building and Installing

```bash
cd android-app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nothing.essential/.MainActivity
```
