# ADR 0003: Escalating Brutality Engine, Agent-Mediated Input Pipeline, and Key Pool Registry

## Status
Accepted

## Context
The user identified two critical operational needs:
1. They do not want an external chatbot or command prompt separated from the app. Instead, **the app itself is the agent**: any problem link, bootcamp milestone, or job entry must pass through the Agent Pipeline for intelligent analysis, milestone decomposition, and context synthesis, surfaced via an **Agent Proposal Card** for operator approval or customization.
2. The user struggles with multi-day motivation paralysis, where an empty queue or idle streak leads to days of inaction. A static reminder fails to break this inertia. The user specifically requires an **Escalating Brutality Engine** that begins with poetic/cinematic wisdom and escalates into sharp, unfiltered reality checks if inactivity persists beyond 1–3 hours without an **Excused Snooze**.
3. To maximize free-tier usage across Google Cloud projects while supporting DeepSeek fallback, API keys must be managed in a persistent **Key Pool Registry** with visual health badges and automated cooldowns.

## Decision
1. **Agent-Mediated Input Pipeline**:
   - The `+ ADD` actions across DSA, Courses, and Jobs route to the Agent Pipeline.
   - The user inputs raw links, screenshots, or thoughts. The pipeline injects the 3-day Episodic Context Buffer and calls the Key Pool to extract metadata, difficulty, and 3–5 milestone checklist items.
   - Output is presented on an **Agent Proposal Card** where the user can confirm with one tap, tweak instructions (e.g. "add constraint for negatives"), or cancel.

2. **Escalating Brutality Coaching Engine**:
   - Background alarms evaluate inactivity elapsed since the last completed task:
     - **0 to 60 mins**: Standard silent tracking.
     - **60 to 90 mins (Stage 1 - Literary & Cinematic Wisdom)**: Quotes from classic poets, writers, cinema, and comics (e.g. Marcus Aurelius, Oppenheimer, anime, Batman, Bhagavad Gita, Hunter S. Thompson) linking to their active goals.
     - **90 to 180 mins (Stage 2 - Direct Sarcasm & Tough Love)**: Sharp, witty callouts on streak preservation and 100xDevs / TUF commitments.
     - **180+ mins (Stage 3 - Unfiltered Reality Checks)**: Direct, unfiltered confrontation addressing procrastination, empty queues, and the cost of inaction.
   - If an **Excused Snooze** is active with a reason, all notifications remain strictly suppressed.

3. **Key Pool Registry**:
   - Persisted in SQLite table `api_key_pool` (`id`, `provider`, `label`, `api_key`, `status`, `cooldown_until`).
   - Round-robin load balancing on healthy keys; HTTP 429 triggers a 60-second cooldown and automatic rotation to the next active key.

## Consequences
- Single unified interaction model across the entire app.
- Highly personalized motivation engine tailored to break deep procrastination habits.
- Strict token management keeps LLM costs zero or negligible.
