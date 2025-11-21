# Development Standards

## 1. Code Style

*   **Formatting**: Follow the standard Android code style guidelines. Use Android Studio's default code formatter to ensure consistency.
*   **Line Length**: Keep lines of code under 100 characters.
*   **Comments**: Write clear and concise comments to explain complex logic. Avoid unnecessary comments.

## 2. Naming Conventions

*   **Classes**: Use PascalCase for class names (e.g., `VideoPlayerAdapter`).
*   **Methods and Variables**: Use camelCase for method and variable names (e.g., `onCreateViewHolder`, `videoPlayer`).
*   **Layout Files**: Use snake_case for layout file names (e.g., `activity_main.xml`).
*   **Views**: Use camelCase for view IDs in layout files (e.g., `videoRecyclerView`).

## 3. Best Practices

*   **Immutability**: Prefer immutable data classes whenever possible.
*   **Nullability**: Use `@NonNull` and `@Nullable` annotations to explicitly declare nullability.
*   **Error Handling**: Implement proper error handling, especially for network requests and file I/O.
*   **Testing**: Write unit tests for ViewModels and other business logic components.