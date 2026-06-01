# Contributing

1. **Fork & Branch**: Create your feature branch off `main`. 
2. **Commit Guidelines**: Use standard semantic commit messages (e.g., `feat: multi-file transfer`, `fix: socket null pointer`).
3. **Jetpack Compose Guidelines**:
   - Keep views completely devoid of business logic.
   - Hoist states to ViewModels. 
   - Never inject Network Repositories directly into UI.
4. **Testing**: Any modification to cryptographic or network boundaries strictly requires automated unit tests maintaining `>80%` coverage. 
5. **Pull Requests**: Provide clear Before/After summaries when issuing UI changes. 
