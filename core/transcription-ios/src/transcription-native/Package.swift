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
            url: "https://github.com/fdddf/sherpa-onnx-spm.git",
            from: "1.13.4"
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
                .product(name: "sherpa-onnx-spm", package: "sherpa-onnx-spm"),
                .product(name: "SwiftWhisper", package: "SwiftWhisper")
            ],
            resources: [
                .process("Resources/silero_vad.int8.onnx")
            ]
        )
    ]
)
