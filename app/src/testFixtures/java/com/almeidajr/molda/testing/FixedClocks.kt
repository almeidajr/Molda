package com.almeidajr.molda.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Noon UTC on 13/10/2026: "today" for the profile tests, so 14/10/2026 is a future date. */
val October13Clock: Clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)

/** Noon UTC on 18/10/2026: "now" for the project tests, after every seeded measurement date. */
val October18Clock: Clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC)
