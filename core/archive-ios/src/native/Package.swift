// swift-tools-version: 6.4
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "IosNativeTar",

    platforms: [
        .iOS(.v15)
    ],

    products: [
        .library(
            name: "IosNativeTar",
            targets: ["IosNativeTar"]
        )
    ],

    dependencies: [
        .package(
            url: "https://github.com/everpcpc/libarchive-swift.git",
            from: "0.1.11"
        )
    ],

    targets: [
        .target(
            name: "IosNativeTar",
            dependencies: [
                .product(
                    name: "LibArchive",
                    package: "libarchive-swift"
                )
            ],
            )
    ]
)
