# tomAIto

> **Alpha:** This project is in early development. Features may change, and bugs are expected.

tomAIto is an Android camera app that detects tomatoes and classifies their condition in real time. It is built with Kotlin and Jetpack Compose.

## Features

- Live detection overlay with bounding boxes, class labels, and confidence scores
- Three classes: Damaged, Ripe, Unripe, each with its own box color
- Adjustable confidence threshold (10% to 95%, default 80%)
- Live detection toggle
- Detection boxes drawn onto captured photos
- On-device inference with a YOLOv8 model, no network required

## Download

- [Download Latest APK](https://github.com/Tomato-Rangers/tomAIto/releases/latest)
- [View All Versions](https://github.com/Tomato-Rangers/tomAIto/releases)

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- LiteRT (TensorFlow Lite) with a YOLOv8 detection model
- Kotlin Coroutines and Flow

## Architecture

The project uses **MVVM (Model-View-ViewModel)** with a layered architecture, split into three Gradle modules.

[![Architecture diagram of tomato-rangers/tomaito](https://gitdiagram.com/tomato-rangers/tomaito/diagram.png)](https://gitdiagram.com/tomato-rangers/tomaito?utm_source=readme&utm_medium=picture)

### Modules

- **`app`:** Camera, settings, and photo review screens, plus camera and media data layers.
- **`detection`:** Model loading, preprocessing, inference, postprocessing, and the detection overlay UI.
- **`permission`:** Reusable runtime permission gate with rationale and open-Settings dialogs.

## How Detection Works

1. Camera frames are analyzed on a background thread (`ImageAnalysis`, RGBA, 640x480 target).
2. Each frame is letterboxed to the model input size and passed to the TFLite interpreter.
3. The output is decoded with a per-class confidence threshold, then filtered with non-maximum suppression.
4. Boxes are mapped back to the frame, mirrored for the front camera, and drawn over the preview.
5. On capture, the same pipeline runs on the saved photo and the boxes are drawn into the image.

The model expects a YOLOv8-style output head and supports float and quantized tensors, in either channel layout.

## Requirements

- Android 8.0 (API 26) or later
- A device with a camera

## License

This project is licensed under the GNU Affero General Public License v3.0. See [LICENSE](LICENSE) for details.

Portions derived from [elensiel/camer-](https://github.com/elensiel/camer-) are used under the MIT License. See [LICENSE-MIT.txt](LICENSE-MIT.txt).
