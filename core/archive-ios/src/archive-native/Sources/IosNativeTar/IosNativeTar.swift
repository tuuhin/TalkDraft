import Foundation
import SWCompression

private enum IosNativeTarError: Error {
    case unsafeEntryPath(String)
    case unsafeLinkPath(entry: String, link: String)
    case message(String)
}

@objc(IosNativeTar)
public final class IosNativeTar: NSObject {

    // MARK: - Create

    @objc(createTarFromDirectory:destination:error:)
    public static func createTar(
        fromDirectory sourceDirectory: String,
        destination: String,
        error: NSErrorPointer
    ) -> Bool {

        guard !sourceDirectory.isEmpty, !destination.isEmpty else {
            setError(error, message: "Source and destination cannot be empty.")
            return false
        }

        let fm = FileManager.default
        let rootURL = URL(fileURLWithPath: sourceDirectory).standardized
        let destinationURL = URL(fileURLWithPath: destination)

        do {
            guard
                let enumerator = fm.enumerator(
                    at: rootURL,
                    includingPropertiesForKeys: [.isDirectoryKey, .isSymbolicLinkKey],
                    options: []
                )
            else {
                throw IosNativeTarError.message("Unable to read directory: \(sourceDirectory)")
            }

            var entries: [TarEntry] = []
            let rootPath = rootURL.path.hasSuffix("/") ? rootURL.path : rootURL.path + "/"

            for case let fileURL as URL in enumerator {
                let fullPath = fileURL.standardized.path
                guard fullPath.hasPrefix(rootPath) else {
                    continue
                }
                let relativePath = String(fullPath.dropFirst(rootPath.count))
                guard !relativePath.isEmpty else {
                    continue
                }

                let values = try fileURL.resourceValues(
                    forKeys: [.isDirectoryKey, .isSymbolicLinkKey]
                )
                let attrs = try? fm.attributesOfItem(atPath: fileURL.path)

                if values.isSymbolicLink == true {
                    var info = TarEntryInfo(name: relativePath, type: .symbolicLink)
                    info.linkName = try fm.destinationOfSymbolicLink(atPath: fileURL.path)
                    info.modificationTime = attrs?[.modificationDate] as? Date
                    entries.append(TarEntry(info: info, data: nil))

                } else if values.isDirectory == true {
                    var info = TarEntryInfo(name: relativePath, type: .directory)
                    applyAttributes(attrs, to: &info)
                    entries.append(TarEntry(info: info, data: nil))

                } else {
                    var info = TarEntryInfo(name: relativePath, type: .regular)
                    applyAttributes(attrs, to: &info)
                    let data = try Data(contentsOf: fileURL)
                    entries.append(TarEntry(info: info, data: data))
                }
            }

            var tarData = TarContainer.create(from: entries)

            // Write gzip if the destination ends with .gz / .tgz
            let lower = destination.lowercased()
            if lower.hasSuffix(".gz") || lower.hasSuffix(".tgz") {
                tarData = try GzipArchive.archive(data: tarData)
            }

            try fm.createDirectory(
                at: destinationURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            try tarData.write(to: destinationURL, options: .atomic)
            return true

        } catch let err {
            setError(error, message: errorMessage(err))
            return false
        }
    }

    private static func applyAttributes(
        _ attrs: [FileAttributeKey: Any]?,
        to info: inout TarEntryInfo
    ) {
        info.modificationTime = attrs?[.modificationDate] as? Date
        if let posix = attrs?[.posixPermissions] as? NSNumber {
            info.permissions = Permissions(rawValue: UInt32(posix.intValue & 0o777))
        }
    }

    // MARK: - Extract

    @objc(extractTar:destination:error:)
    public static func extractTar(
        _ source: String,
        destination: String,
        error: NSErrorPointer
    ) -> Bool {

        guard !source.isEmpty, !destination.isEmpty else {
            setError(error, message: "Source and destination cannot be empty.")
            return false
        }

        let fm = FileManager.default
        let sourceURL = URL(fileURLWithPath: source)
        let destinationURL = URL(fileURLWithPath: destination).standardized

        do {
            try fm.createDirectory(at: destinationURL, withIntermediateDirectories: true)

            var archiveData = try Data(contentsOf: sourceURL)

            // gzip magic bytes: 1F 8B
            if archiveData.count >= 2, archiveData[0] == 0x1F, archiveData[1] == 0x8B {
                archiveData = try GzipArchive.unarchive(archive: archiveData)
            }

            let entries = try TarContainer.open(container: archiveData)

            for entry in entries {
                let name = entry.info.name

                guard isSafeArchivePath(name) else {
                    throw IosNativeTarError.unsafeEntryPath(name)
                }

                let outputURL = destinationURL.appendingPathComponent(name).standardized
                guard isInside(outputURL, root: destinationURL) else {
                    throw IosNativeTarError.unsafeEntryPath(name)
                }

                switch entry.info.type {

                case .directory:
                    try fm.createDirectory(at: outputURL, withIntermediateDirectories: true)

                case .regular:
                    try fm.createDirectory(
                        at: outputURL.deletingLastPathComponent(),
                        withIntermediateDirectories: true
                    )
                    try (entry.data ?? Data()).write(to: outputURL, options: .atomic)
                    applyMetadata(entry.info, to: outputURL)

                case .symbolicLink:
                    let target = entry.info.linkName
                    guard !target.isEmpty else {
                        continue
                    }

                    guard isSafeLink(target, entryPath: name, root: destinationURL) else {
                        throw IosNativeTarError.unsafeLinkPath(entry: name, link: target)
                    }

                    try fm.createDirectory(
                        at: outputURL.deletingLastPathComponent(),
                        withIntermediateDirectories: true
                    )
                    try? fm.removeItem(at: outputURL)
                    try fm.createSymbolicLink(
                        atPath: outputURL.path,
                        withDestinationPath: target
                    )

                default:
                    continue
                }
            }

            return true

        } catch let err {
            setError(error, message: errorMessage(err))
            return false
        }
    }

    // MARK: - Security

    private static func isSafeArchivePath(_ path: String) -> Bool {
        guard !path.isEmpty else {
            return false
        }
        if path.hasPrefix("/") {
            return false
        }

        let chars = Array(path)
        if chars.count >= 2, chars[1] == ":" {
            return false
        }

        let components = path.split(separator: "/", omittingEmptySubsequences: true)
        return !components.contains("..")
    }

    private static func isInside(_ url: URL, root: URL) -> Bool {
        let rootPath = root.path.hasSuffix("/") ? root.path : root.path + "/"
        let path = url.path
        return path == root.path || path.hasPrefix(rootPath)
    }

    private static func isSafeLink(_ target: String, entryPath: String, root: URL) -> Bool {
        if target.hasPrefix("/") {
            return false
        }

        let resolved =
            root
                .appendingPathComponent(entryPath)
                .deletingLastPathComponent()
                .appendingPathComponent(target)
                .standardized

        return isInside(resolved, root: root)
    }

    // MARK: - Metadata

    private static func applyMetadata(_ info: TarEntryInfo, to url: URL) {
        var attributes: [FileAttributeKey: Any] = [:]

        if let modified = info.modificationTime {
            attributes[.modificationDate] = modified
        }
        if let permissions = info.permissions {
            attributes[.posixPermissions] = Int(permissions.rawValue & 0o777)
        }

        if !attributes.isEmpty {
            try? FileManager.default.setAttributes(attributes, ofItemAtPath: url.path)
        }
    }

    // MARK: - Error Handling

    private static func setError(_ error: NSErrorPointer, message: String) {
        error?.pointee = NSError(
            domain: "IosNativeTar",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: message]
        )
    }

    private static func errorMessage(_ error: Error) -> String {
        if let e = error as? IosNativeTarError {
            switch e {
            case .unsafeEntryPath(let path):
                return "Unsafe archive path: \(path)"
            case .unsafeLinkPath(let entry, let link):
                return "Unsafe symbolic link '\(entry)' -> '\(link)'."
            case .message(let text):
                return text
            }
        }
        if let e = error as? TarError {
            return "Invalid TAR archive: \(e)"
        }
        if let e = error as? GzipError {
            return "Invalid gzip data: \(e)"
        }
        return error.localizedDescription
    }
}
