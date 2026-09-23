// swift-tools-version: 6.4
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "IosNativeTranscription",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "IosNativeTranscription",
            targets: ["IosNativeTranscription"]
        )
    ],
    dependencies: [
        .package(
            url: "https://github.com/microsoft/onnxruntime-swift-package-manager.git",
            from: "1.24.2"
        ),
        .package(
            url: "https://github.com/exPHAT/SwiftWhisper.git",
            branch: "master"
        )
    ],
    targets: [
        .target(
            name: "IosNativeTranscription",
            dependencies: [
                .product(name: "onnxruntime", package: "onnxruntime-swift-package-manager"),
                .product(name: "SwiftWhisper", package: "SwiftWhisper")
            ],
            resources: [
                .process("Resources/silero_vad.onnx")
            ]
        )
    ]
)
