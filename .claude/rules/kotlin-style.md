---
paths:
  - "**/*.kt"
  - "**/*.kts"
---

# Kotlin code style

Apply these rules to new Kotlin code and code already being changed in Molda.
Follow `AGENTS.md`, `.editorconfig`, and established local conventions; leave
unrelated code alone. Spotless/ktlint owns formatting.

**Zero warnings.** The Kotlin compiler (`-Werror`, `-Wextra`, return value
checker), type-resolved Detekt, and Android Lint fail the build on any finding.
Android Studio inspections the build cannot run count too, so write code that
passes them: declare a `when` subject inline (`when (val result = load())`),
import names instead of qualifying them in place, and place `@OptIn` only on
declarations that call the experimental API. Fix the cause; when a suppression
is the right call, scope it to one declaration and comment the reason.

**Functional first.** Prefer pure functions that transform inputs into outputs.
Use expression bodies for single expressions, `when` for alternatives, and
standard collection operations such as `map`, `filter`, `mapNotNull`, and `fold`
when they express the operation clearly. Prefer a direct loop to a convoluted
pipeline. Use sequences only when lazy evaluation serves the workload; they are
not automatically faster. Keep mutation local when it makes an implementation
simpler, and return an immutable result.

**Immutable by default.** Prefer `val`, immutable properties, and `data class`
for value models. Classes are final by default; open them only for intentional
inheritance. Use `copy` to describe state transitions. Expose read-only
collections rather than `MutableList`, `MutableSet`, `MutableMap`, or arrays
that callers can mutate. `val` fixes a reference, not the referenced object;
read-only collections can still share mutable backing storage, and `copy()` is
shallow. Snapshot externally owned mutable collections at boundaries and keep
their elements immutable. When persistent immutable collections materially
help shared state or repeated updates, prefer a maintained library such as
`kotlinx.collections.immutable` over a custom implementation.

**Pure logic, explicit effects.** Put business rules in small functions with
explicit inputs. Keep I/O, logging, Android dependencies, and state ownership at
the edges. Pass time into pure calculations; inject `java.time.Clock` when a
component must obtain the current instant. Use monotonic time for elapsed
durations. Add interfaces only where a real boundary needs them.

**Model valid states.** Use non-null types by default, nullable types for real
absence, and sealed types for mutually exclusive states or expected outcomes.
Prefer smart casts, safe calls, and the Elvis operator to `!!`. Validate external
input at its boundary; use `require` for argument preconditions and `check` for
internal state invariants. Use enums for a fixed set of named values. Introduce
value classes only when a distinct type prevents a meaningful mistake. Keep
error handling explicit; use `Result` only where its semantics fit the API.

**Modern, idiomatic syntax.** Use stable features supported by the project's
configured Kotlin toolchain. Prefer primary constructors, named arguments when
they clarify a call, default parameters over redundant overloads, string
templates, trailing lambdas, and destructuring when component meanings remain
clear. Infer obvious local types and declare public API types explicitly. Use
top-level functions or extensions when they fit; avoid utility objects and
extensions that hide effects. Use `let`, `run`, `with`, `apply`, and `also` only
when the receiver and result stay obvious. Avoid nested scope functions. Write
text with real line breaks, and SQL, as raw strings: `trimIndent()` when no line
is indented within the text, a `|` margin and `trimMargin()` when one is. Keep
prose wrapped only for line length as `+` concatenation, since a raw string
would add line breaks to it.
Experimental features need a concrete benefit and a narrow, explained opt-in.

**Idiomatic coroutines.** Use `suspend` for asynchronous one-shot operations and
`Flow` for streams when coroutines are needed. Name functions for their action;
an `Async` suffix is not a Kotlin convention. Use structured concurrency with
an explicitly owned, lifecycle-appropriate scope. Preserve cancellation: rethrow
`CancellationException` when catching broadly, including around `runCatching`
in suspending code. Keep blocking work off the main thread; `suspend` alone
does not move it. Make dispatchers replaceable where scheduling affects tests.
Use coroutine/AndroidX facilities rather than custom thread or scheduler code.

**Owned state.** Keep mutable state private and expose a read-only view. Prefer
an immutable state value updated through the state holder's supported API.
When using `MutableStateFlow.update`, keep the transformation free of effects
because it may run more than once. Choose confinement or synchronization that
matches the owner; immutability alone does not make concurrent updates atomic.

**Compose.** Prefer stateless composables with state passed in and events passed
out. Hoist state to the lowest owner that needs it; keep business logic outside
composables. Accept `modifier: Modifier = Modifier` in reusable UI components
and apply it to the root. Keep composition free of I/O and other uncontrolled
effects; use lifecycle-aware effect APIs when needed. Use `rememberSaveable`
for suitable UI state that must survive recreation. Preserve accessibility
semantics. Apply stability annotations only when their contracts are true;
`@Immutable` does not make mutable data immutable.

**Small functions, useful names.** Each function has one job. Use domain names,
not cryptic abbreviations: `temperature`, not `t`; `session`, not `s`. Kotlin's
implicit `it` is fine for a short, obvious lambda; name parameters in longer or
nested lambdas. Boolean names read as affirmative predicates: `isEnabled`,
`hasErrors`, `canRetry`, `shouldCache`, `wasProcessed`, `willExpire`. Follow
Molda's PascalCase composable names and camelCase ordinary function names.
Primitive and `String` constants are `const val` in `SCREAMING_SNAKE_CASE`;
other deeply immutable top-level values, such as `Dp`, use PascalCase.

**Comments and KDoc.** Let names and types explain the code. Add a short comment
for a non-obvious constraint or reason. Write KDoc when callers need a contract,
units, ownership, side effects, or an expected failure explained; coverage is
not a goal. Start with one sentence explaining purpose. Add `@param`, `@return`,
and `@throws` only when they convey information beyond the signature. Internal
APIs may need documentation for a non-obvious contract too. Keep design rationale
in the spec or ADR rather than copying it into every hover.

**Libraries over custom infrastructure.** Reuse project facilities and Kotlin,
JDK, or AndroidX APIs first. For substantial capabilities such as serialization,
persistence, networking, immutable collections, or coroutine testing, prefer an
established maintained library to hand-rolled machinery. Check Android/toolchain
compatibility, maintenance, license, security, and transitive cost. Add versions
through `gradle/libs.versions.toml`, respecting the Compose BOM. Add a dependency
when a concrete requirement needs it, rather than building a speculative toolkit
or importing a library for a trivial helper. Fetch current API documentation
through Context7 before selecting or using a library.

See [Kotlin tests](kotlin-tests.md) when changing tests.

References: [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html),
[collection types](https://kotlinlang.org/docs/collections-overview.html),
[data class copying](https://kotlinlang.org/docs/data-classes.html#copying),
and [coroutines overview](https://kotlinlang.org/docs/coroutines-overview.html).
