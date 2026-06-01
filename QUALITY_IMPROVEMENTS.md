# Quality Improvements

## Documentation
- The repository would benefit highly from a standardized `CONTRIBUTING.md` outlining the DI graph and Jetpack Compose component map.

## Code Structure (Refactoring Opportunities)
- Domain layer needs decoupling from Data layer. Currently, `TransferManager` behaves like a god-object orchestrating everything.
  - *Recommendation:* Abstract socket layer to a `SocketDataSource` and inject.

## Removal of Dead Code
- Removed deprecated UI icon calls (e.g., `Icons.Filled.Send` migrated to `Icons.AutoMirrored.Filled.Send`).
- Cleaned up dangling Dagger injection artifacts that remained after testing migration tools.

## Logging & Error Handling
- Errors currently print standard stacktraces in the tests. Add `Timber` for robust application tree logging, especially in release builds where debugging sockets is extremely difficult.
