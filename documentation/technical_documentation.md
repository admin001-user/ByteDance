# Technical Documentation

## 1. Project Overview

This project is a short-form video application similar to TikTok. It features a dual-column video feed for browsing and a single-column video player for an immersive viewing experience. Users can also view and add comments on videos.

## 2. Architecture

The application follows a Model-View-ViewModel (MVVM) architecture pattern. The key components are:

*   **Model**: Represents the data and business logic of the application. This includes the `Video` and `Comment` data classes.
*   **View**: The UI of the application, which is composed of Activities and Fragments. The View observes the ViewModel for data changes and updates the UI accordingly.
*   **ViewModel**: Acts as a bridge between the Model and the View. It exposes data from the Model to the View and handles user interactions.

## 3. Feature Implementation

### 3.1. Dual-Column Video Feed

*   **Layout**: Implemented using a `RecyclerView` with a `GridLayoutManager` to display two columns of video thumbnails.
*   **Adapter**: The `VideoAdapter` is responsible for binding the video data to the `item_video.xml` layout.
*   **Navigation**: Clicking on a video thumbnail navigates the user to the `PlayerActivity`.

### 3.2. Single-Column Video Player

*   **Layout**: Implemented using a `ViewPager2` to allow users to swipe vertically between videos.
*   **Video Playback**: `ExoPlayer` is used for video playback. Each video in the `ViewPager2` has its own `ExoPlayer` instance.
*   **Adapter**: The `VideoPlayerAdapter` is responsible for managing the `ExoPlayer` instances and binding the video data to the `item_video_player.xml` layout.

### 3.3. Comment Panel

*   **UI**: The comment panel is implemented as a `BottomSheetDialogFragment` which slides up from the bottom of the screen.
*   **Layout**: The `fragment_comment_panel.xml` layout contains a `RecyclerView` to display the comments and an `EditText` for adding new comments.
*   **Adapter**: The `CommentAdapter` is responsible for binding the comment data to the `item_comment.xml` layout.
*   **Functionality**: Users can add new comments, which are then added to the `RecyclerView` and displayed at the bottom of the list.