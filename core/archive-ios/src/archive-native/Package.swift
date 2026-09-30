// swift-tools-version: 6.4
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "IosNativeTar",

    platforms: [
        .iOS(.v17)
    ],

    products: [
        .library(
            name: "IosNativeTar",
            targets: ["IosNativeTar"]
        )
    ],

    dependencies: [
        .package(
            url: "https://github.com/tsolomko/SWCompression.git",
            from: "4.8.6"
        )
    ],
    targets: [
        .target(
            name: "IosNativeTar",
            dependencies: [
                .product(name: "SWCompression", package: "SWCompression")
            ]
        )
    ]
)
