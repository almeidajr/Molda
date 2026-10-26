# Profiles move to Room 3 with its default driver, in a new database file, without migrating beta data

Measurement profiles were stored with a handwritten `SQLiteOpenHelper` in `profiles.db`, whose error handler refused to delete a corrupt file. We replaced it with Room 3, the latest stable Room, using its default `AndroidSQLiteDriver`. That driver opens the database with Android's default error handler, which deletes a corrupt file, and offers no way to change it. We accepted that: Molda is in beta, a profile is a name, a waist and a date that a sewist can re-enter in seconds, and the alternatives cost more than the data is worth. `BundledSQLiteDriver` throws instead of deleting but adds about 1.2 MB of native SQLite per ABI. A custom driver over the framework would mean reimplementing about 500 lines of internal androidx code. Wrapping `FrameworkSQLiteOpenHelperFactory` with a throwing callback looks simpler but breaks Room's migrations, because the open helper writes `user_version` before Room reads it.

The old table does not pass Room's schema validation (its `id` column is nullable in SQLite's view), so adopting it would need a migration that recreates the table. Instead, Room opens a new file, `molda.db`, and existing beta profiles are not carried over. Testers who update in place keep an orphaned `profiles.db` that nothing reads. Reusing the old name would make Room fail on every launch for them. Backup rules already exclude databases, so uninstalling removes both files.

## Consequences

- A corrupt database is deleted and the app starts with no profiles. Do not "restore" the old error handler without revisiting this decision.
- From this version on, every schema change ships a real `Migration`. We keep Room's default `requireMigration` and do not enable `fallbackToDestructiveMigration`, so a missing migration fails loudly instead of wiping data after release.
- Beta release notes for this version must tell testers that saved profiles are reset.
