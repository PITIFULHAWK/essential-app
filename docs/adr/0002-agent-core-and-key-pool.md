# ADR 0002: Agent-Centric Core, Tiered Key Pooling, and Token-Guarded Inactivity Coaching

## Status
Accepted

## Context
The user requires the application to act as an integrated agent where all operations (DSA tracking, course progress, job logging, inactivity monitoring) flow through an agent tool-calling engine. To prevent API billing spikes (capping usage at $0.00 on Gemini Free or <$0.15 on DeepSeek Flash), the agent context window must be strictly guarded. Additionally, to avoid single-account rate-limiting on the free tier without violating Google Terms of Service, the app requires a multi-key pool with automated failover.

## Decision
1. **Agent-Centric Domain Orchestration**:
   - The agent is not isolated to a chat screen. All user mutations flow through the agent execution layer via local SQLite tools (`create_dsa_problem`, `update_milestone`, `set_snooze`, `update_course_progress`, `log_job_application`).
   - Mutations are staged via an **Agent Proposal Card** allowing the user to confirm, cancel, or inject custom context before committing.

2. **Episodic Context Buffer (Sliding 72-Hour Digest)**:
   - Raw multi-turn chat transcripts are never persisted or sent to LLMs.
   - At invocation time, the local SQLite database generates a compact ~250-token markdown state vector (streak count, last 2-3 completed milestones, elapsed inactivity, active problem).

3. **Inactivity Poke Engine**:
   - Inactivity under 60 minutes remains quiet or purely local.
   - Inactivity exceeding 60 minutes without an **Excused Snooze** triggers a periodic check every 15 minutes.
   - When online, the background worker invokes the LLM with a 150-token prompt containing only the last 2-3 completed items to generate tailored, witty, or tough-love motivational notification copy and suggested next tasks.
   - If an Excused Snooze was set with a duration and reason, all pokes are strictly suppressed.

4. **Tiered Key Pool with Auto-Failover**:
   - Supports multiple keys per provider (e.g. Gemini Project 1, Gemini Project 2).
   - Primary tier: Google Gemini Free Pool (rotated round-robin).
   - On HTTP 429 (Rate Limit): the exhausted key enters a 60-second cooldown, and the request immediately re-routes to the next healthy key in the pool.
   - If all free keys are exhausted, the engine falls back to DeepSeek V4.1 Flash.

## Consequences
- **Positive**:
  - Zero API billing during normal usage via Google Free Tier key pooling.
  - Transparent failover prevents user disruption during intense DSA sessions.
  - Agentic pokes keep streaks alive without draining tokens.
  - Operator retains control over mutations via the Agent Proposal Card.
- **Negative / Trade-offs**:
  - Requires maintaining key health states in local SQLite.
  - Background pokes require network connectivity to generate dynamic copy; fallback to cached local templates when offline.
