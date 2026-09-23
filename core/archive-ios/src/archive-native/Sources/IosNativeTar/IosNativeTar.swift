import Foundation
import LibArchive

@objc(IosNativeTar)
public final class IosNativeTar: NSObject {

    /*
     * The current LibArchive Swift API does not expose an ArchiveWriter.
     *
     * Therefore TAR creation cannot currently be implemented without
     * dropping down to CArchive.
     */
    @objc(createTarFromDirectory:destination:error:)
    public static func createTar(
        fromDirectory sourceDirectory: String,
        destination: String,
        error: NSErrorPointer
    ) -> Bool {
        setError(
            error, message: "TAR creation is not supported by the current LibArchive Swift API.")
        return false
    }

    @objc(extractTar:destination:error:)
    public static func extractTar(
        _ source: String,
        destination: String,
        error: NSErrorPointer
    ) -> Bool {

        guard !source.isEmpty, !destination.isEmpty else {
            setError(
                error,
                message: "Source and destination cannot be empty."
            )
            return false
        }

        let sourceURL = URL(fileURLWithPath: source)
        let destinationURL = URL(fileURLWithPath: destination)

        do {
            try FileManager.default.createDirectory(
                at: destinationURL,
                withIntermediateDirectories: true
            )

            let reader = ArchiveReader()

            let entries = try reader.entries(at: sourceURL)

            for entry in entries {

                guard isSafeArchivePath(entry.path) else {
                    throw ArchiveError.unsafeEntryPath(entry.path)
                }

                let outputURL =
                    destinationURL
                        .appendingPathComponent(entry.path)

                switch entry.fileType {

                case .directory:
                    try FileManager.default.createDirectory(
                        at: outputURL,
                        withIntermediateDirectories: true
                    )

                case .regular:
                    try extractFile(
                        entry: entry,
                        reader: reader,
                        archiveURL: sourceURL,
                        outputURL: outputURL
                    )

                case .symbolicLink:
                    try extractSymbolicLink(
                        entry: entry,
                        outputURL: outputURL
                    )

                default:
                    continue
                }
            }

            return true

        } catch let archiveError {
            setError(
                error,
                message: errorMessage(archiveError)
            )
            return false
        }
    }

    private static func extractFile(
        entry: ArchiveEntry,
        reader: ArchiveReader,
        archiveURL: URL,
        outputURL: URL
    ) throws {

        let parentDirectory = outputURL.deletingLastPathComponent()

        try FileManager.default.createDirectory(
            at: parentDirectory,
            withIntermediateDirectories: true
        )

        let data = try reader.data(
            forEntryPath: entry.path,
            in: archiveURL
        )

        try data.write(
            to: outputURL,
            options: .atomic
        )

        applyMetadata(
            entry: entry,
            to: outputURL
        )
    }

    private static func extractSymbolicLink(
        entry: ArchiveEntry,
        outputURL: URL
    ) throws {

        guard let target = entry.symlinkTarget else {
            return
        }

        guard
            isSafeLinkPath(
                target,
                entryPath: entry.path
            )
        else {
            throw ArchiveError.unsafeLinkPath(
                entry: entry.path,
                link: target
            )
        }

        let parentDirectory = outputURL.deletingLastPathComponent()

        try FileManager.default.createDirectory(
            at: parentDirectory,
            withIntermediateDirectories: true
        )

        try FileManager.default.createSymbolicLink(
            atPath: outputURL.path,
            withDestinationPath: target
        )
    }

    // MARK: - Security

    private static func isSafeArchivePath(
        _ path: String
    ) -> Bool {

        guard !path.isEmpty else {
            return false
        }

        // Absolute Unix path.
        if path.hasPrefix("/") {
            return false
        }

        // Windows drive path.
        let characters = Array(path)

        if characters.count >= 2,
           characters[1] == ":" {
            return false
        }

        // Path traversal.
        let components = path.split(
            separator: "/",
            omittingEmptySubsequences: true
        )

        if components.contains("..") {
            return false
        }

        return true
    }

    private static func isSafeLinkPath(
        _ target: String,
        entryPath: String
    ) -> Bool {

        // Absolute link.
        if target.hasPrefix("/") {
            return false
        }

        let entryDirectory = URL(
            fileURLWithPath: entryPath
        ).deletingLastPathComponent()

        let resolved =
            entryDirectory
                .appendingPathComponent(target)
                .standardized

        let normalizedRoot = URL(
            fileURLWithPath: "."
        ).standardized

        return resolved.path.hasPrefix(
            normalizedRoot.path
        )
    }

    // MARK: - Metadata

    private static func applyMetadata(
        entry: ArchiveEntry,
        to url: URL
    ) {

        var attributes: [FileAttributeKey: Any] = [:]

        if let modificationDate = entry.modificationDate {
            attributes[.modificationDate] = modificationDate
        }

        if !attributes.isEmpty {
            try? FileManager.default.setAttributes(
                attributes,
                ofItemAtPath: url.path
            )
        }
    }

    // MARK: - Error Handling

    private static func setError(
        _ error: NSErrorPointer,
        message: String
    ) {

        error?.pointee = NSError(
            domain: "IosNativeTar",
            code: 1,
            userInfo: [
                NSLocalizedDescriptionKey: message
            ]
        )
    }

    private static func errorMessage(
        _ error: Error
    ) -> String {

        if let archiveError = error as? ArchiveError {
            return archiveErrorDescription(
                archiveError
            )
        }

        return error.localizedDescription
    }

    private static func archiveErrorDescription(
        _ error: ArchiveError
    ) -> String {

        switch error {

        case .cannotCreateReader:
            return "Unable to create archive reader."

        case .cannotCreateWriter:
            return "Unable to create archive writer."

        case .cannotCreateDirectory(let path, let message):
            return "Unable to create directory '\(path)': \(message)"

        case .cannotOpenArchive(let path, let message):
            return "Unable to open archive '\(path)': \(message)"

        case .entryNotFound(let path):
            return "Archive entry not found: \(path)"

        case .entryDataTooLarge(let path):
            return "Archive entry is too large: \(path)"

        case .invalidEntryDataOffset(let path, let offset):
            return "Invalid data offset \(offset) for entry '\(path)'."

        case .readFailed(let message):
            return "Archive read failed: \(message)"

        case .writeFailed(let message):
            return "Archive write failed: \(message)"

        case .cannotCreateUTF8Locale(let candidates):
            return "Unable to create UTF-8 locale: \(candidates)"

        case .invalidEntryPath:
            return "Archive contains an invalid entry path."

        case .unsafeEntryPath(let path):
            return "Unsafe archive path: \(path)"

        case .unsafeLinkPath(let entry, let link):
            return "Unsafe symbolic link '\(entry)' -> '\(link)'."
        }
    }
}
