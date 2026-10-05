#include "tar_achive_engine.h"

#include <archive.h>
#include <archive_entry.h>

#include <filesystem>
#include <memory>
#include <string>
#include <system_error>

namespace fs = std::filesystem;

namespace {

constexpr size_t ARCHIVE_BUFFER_SIZE = 64 * 1024;

// ---------- RAII wrappers ----------
struct ReadDeleter {
    void operator()(struct archive* a) const {
        if (a != nullptr) archive_read_free(a); // also closes if still open
    }
};
struct WriteDeleter {
    void operator()(struct archive* a) const {
        if (a != nullptr) archive_write_free(a); // also closes if still open
    }
};
using ReadPtr  = std::unique_ptr<struct archive, ReadDeleter>;
using WritePtr = std::unique_ptr<struct archive, WriteDeleter>;

std::string archiveError(struct archive* archive) {
    const char* error = archive_error_string(archive);
    if (error != nullptr) return error;
    return "Unknown libarchive error";
}

// ARCHIVE_WARN (-20) is non-fatal (e.g. couldn't set ACL/fflags). Only real failures abort.
bool isFatal(int r) { return r < ARCHIVE_OK && r != ARCHIVE_WARN; }

// Validates a path taken from an archive. Rejects absolute paths and any ".." escape.
bool sanitizeEntryPath(const char* raw, fs::path& out) {
    fs::path p = fs::path(raw).lexically_normal();

    if (p.empty() || p.is_absolute() || p.has_root_name() || p.has_root_directory()) return false;
    for (const auto& part : p)
        if (part == "..") return false;
    out = p;
    return true;
}

// Calculates total size of regular files in input path (file or directory).
uintmax_t calculateDirectorySize(const fs::path& path) {
    uintmax_t size = 0;
    std::error_code ec;
    if (fs::is_regular_file(path, ec)) return fs::file_size(path, ec);
    if (fs::is_directory(path, ec)) {
        for (const auto& entry :
             fs::recursive_directory_iterator(path, fs::directory_options::skip_permission_denied, ec)) {
            if (!fs::is_regular_file(entry.path(), ec)) continue;
            size += fs::file_size(entry.path(), ec);
        }
    }
    return size;
}

// Copies entry data from a TAR reader to the disk writer (preserves sparse offsets).
bool copyDataToDisk(struct archive* in, struct archive* out, std::string& error,
                    const std::function<void(float)>& progressCallback, uintmax_t totalSize,
                    struct archive* inputArchive) {
    const void* buffer = nullptr;
    size_t size        = 0;
    la_int64_t offset  = 0;

    while (true) {
        int r = archive_read_data_block(in, &buffer, &size, &offset);
        if (r == ARCHIVE_EOF) return true;
        if (isFatal(r)) {
            error = "Failed to read TAR data: " + archiveError(in);
            return false;
        }

        r = static_cast<int>(archive_write_data_block(out, buffer, size, offset));
        if (isFatal(r)) {
            error = "Failed to write extracted data: " + archiveError(out);
            return false;
        }

        if (progressCallback && totalSize > 0 && inputArchive != nullptr) {
            auto bytesRead = archive_filter_bytes(inputArchive, 0);
            if (bytesRead < 0) bytesRead = 0;
            if (static_cast<uintmax_t>(bytesRead) > totalSize) {
                bytesRead = static_cast<la_int64_t>(totalSize);
            }
            float percentage = static_cast<float>(bytesRead) * 100.0f / static_cast<float>(totalSize);
            if (percentage > 100.0f) percentage = 100.0f;
            progressCallback(percentage);
        }
    }
}

// rootName is empty when archiving a directory (contents are stored relative to it),
// or the file name when archiving a single file.
Result writeTar(const fs::path& rootPath, const fs::path& rootName, const std::string& outputPath,
                const std::function<void(float)>& progressCallback, uintmax_t totalSize) {
    WritePtr output(archive_write_new());
    if (!output) return Result::Failure("Failed to create TAR writer");

    int result = archive_write_set_format_pax_restricted(output.get());
    if (result != ARCHIVE_OK) return Result::Failure("Failed to configure TAR format: " + archiveError(output.get()));

    result = archive_write_open_filename(output.get(), outputPath.c_str());
    if (result != ARCHIVE_OK) return Result::Failure("Failed to open TAR output: " + archiveError(output.get()));

    ReadPtr disk(archive_read_disk_new());
    if (!disk) return Result::Failure("Failed to create filesystem reader");

    archive_read_disk_set_standard_lookup(disk.get());

    result = archive_read_disk_open(disk.get(), rootPath.string().c_str());
    if (result != ARCHIVE_OK) return Result::Failure("Failed to open input path: " + archiveError(disk.get()));

    std::unique_ptr<char[]> buffer(new char[ARCHIVE_BUFFER_SIZE]);
    uintmax_t processedBytes = 0;

    while (true) {
        struct archive_entry* entry = nullptr;

        result = archive_read_next_header(disk.get(), &entry);
        if (result == ARCHIVE_EOF) break;
        if (isFatal(result)) return Result::Failure("Failed to read filesystem entry: " + archiveError(disk.get()));

        const char* source = archive_entry_sourcepath(entry);
        if (source == nullptr) source = archive_entry_pathname(entry);
        if (source == nullptr) return Result::Failure("Filesystem entry has no path");

        // Don't put the archive we are writing into itself.
        if (archive_entry_filetype(entry) == AE_IFREG) {
            std::error_code ec;
            if (fs::equivalent(source, outputPath, ec) && !ec) continue;
        }

        // Path stored in the archive, relative to the input root.
        fs::path rel = fs::path(source).lexically_relative(rootPath);
        if (rel.empty()) return Result::Failure(std::string("Cannot compute relative path for: ") + source);

        fs::path archiveName = rootName;
        if (rel != ".") archiveName /= rel;

        // The top-level directory itself has no name inside the archive:
        // don't write a header for it, but DO descend into it.
        if (archiveName.empty()) {
            if (archive_read_disk_can_descend(disk.get())) {
                result = archive_read_disk_descend(disk.get());
                if (isFatal(result))
                    return Result::Failure("Failed to descend into directory: " + archiveError(disk.get()));
            }
            continue;
        }

        archive_entry_set_pathname(entry, archiveName.generic_string().c_str());

        result = archive_write_header(output.get(), entry);
        if (isFatal(result)) return Result::Failure("Failed to write TAR header: " + archiveError(output.get()));

        if (archive_entry_filetype(entry) == AE_IFREG) {
            while (true) {
                la_ssize_t bytesRead = archive_read_data(disk.get(), buffer.get(), ARCHIVE_BUFFER_SIZE);
                if (bytesRead == 0) break;
                if (bytesRead < 0) return Result::Failure("Failed to read file data: " + archiveError(disk.get()));

                la_ssize_t bytesWritten =
                    archive_write_data(output.get(), buffer.get(), static_cast<size_t>(bytesRead));
                if (bytesWritten != bytesRead)
                    return Result::Failure("Failed to write file data: " + archiveError(output.get()));

                if (progressCallback && totalSize > 0) {
                    processedBytes += static_cast<size_t>(bytesRead);
                    if (processedBytes > totalSize) processedBytes = totalSize;
                    float percentage = static_cast<float>(processedBytes) * 100.0f / static_cast<float>(totalSize);
                    if (percentage > 100.0f) percentage = 100.0f;
                    progressCallback(percentage);
                }
            }
        }

        result = archive_write_finish_entry(output.get());
        if (isFatal(result)) return Result::Failure("Failed to finish TAR entry: " + archiveError(output.get()));

        // REQUIRED: archive_read_disk does NOT recurse on its own.
        if (archive_read_disk_can_descend(disk.get())) {
            result = archive_read_disk_descend(disk.get());
            if (isFatal(result))
                return Result::Failure("Failed to descend into directory: " + archiveError(disk.get()));
        }
    }

    result = archive_read_close(disk.get());
    if (isFatal(result)) return Result::Failure("Failed to close filesystem reader: " + archiveError(disk.get()));

    result = archive_write_close(output.get());
    if (isFatal(result)) return Result::Failure("Failed to finalize TAR: " + archiveError(output.get()));

    if (progressCallback) progressCallback(100.0f);
    return Result::Success();
}

} // namespace

Result tar_archive_engine::createTar(const std::string& inputPath, const std::string& outputPath,
                                     const std::function<void(float)>& progressCallback) {
    std::error_code ec;

    if (!fs::exists(inputPath, ec) || ec) return Result::Failure("Input path does not exist: " + inputPath);

    fs::path rootPath = fs::absolute(inputPath, ec).lexically_normal();
    if (ec) return Result::Failure("Failed to resolve input path: " + ec.message());
    if (!rootPath.has_filename()) rootPath = rootPath.parent_path(); // strip trailing slash

    // Directory  -> contents stored at the archive root (extract reproduces the directory).
    // Single file -> stored under its own file name.
    fs::path rootName;
    if (!fs::is_directory(rootPath, ec)) rootName = rootPath.filename();

    uintmax_t totalSize = calculateDirectorySize(rootPath);
    // Make sure the output directory exists.
    fs::path outParent = fs::path(outputPath).parent_path();
    if (!outParent.empty()) {
        fs::create_directories(outParent, ec);
        if (ec) return Result::Failure("Failed to create output directory: " + ec.message());
    }

    Result res = writeTar(rootPath, rootName, outputPath, progressCallback, totalSize);

    if (!res.success) {
        std::error_code rmEc;
        fs::remove(outputPath, rmEc); // don't leave a truncated archive behind
    }
    return res;
}

Result tar_archive_engine::extractTar(const std::string& inputPath, const std::string& outputPath,
                                      const std::function<void(float)>& progressCallback) {
    std::error_code ec;

    if (!fs::is_regular_file(inputPath, ec) || ec) return Result::Failure("TAR file does not exist: " + inputPath);

    uintmax_t totalSize = fs::file_size(inputPath, ec);

    fs::create_directories(outputPath, ec);
    if (ec) return Result::Failure("Failed to create output directory: " + ec.message());

    // Canonical root so the SECURE_* flags and our own prefix checks are reliable.
    fs::path root = fs::canonical(outputPath, ec);
    if (ec) return Result::Failure("Failed to resolve output directory: " + ec.message());

    ReadPtr input(archive_read_new());
    if (!input) return Result::Failure("Failed to create TAR reader");

    archive_read_support_format_tar(input.get());
    archive_read_support_filter_all(input.get()); // also accepts .tar.gz/.tar.xz/... (optional)

    int result = archive_read_open_filename(input.get(), inputPath.c_str(), ARCHIVE_BUFFER_SIZE);
    if (result != ARCHIVE_OK) return Result::Failure("Failed to open TAR file: " + archiveError(input.get()));

    WritePtr output(archive_write_disk_new());
    if (!output) return Result::Failure("Failed to create filesystem writer");

    archive_write_disk_set_options(output.get(), ARCHIVE_EXTRACT_TIME | ARCHIVE_EXTRACT_PERM | ARCHIVE_EXTRACT_ACL |
                                                     ARCHIVE_EXTRACT_FFLAGS | ARCHIVE_EXTRACT_SECURE_SYMLINKS |
                                                     ARCHIVE_EXTRACT_SECURE_NODOTDOT);
    archive_write_disk_set_standard_lookup(output.get());

    while (true) {
        struct archive_entry* entry = nullptr;

        result = archive_read_next_header(input.get(), &entry);
        if (result == ARCHIVE_EOF) break;
        if (isFatal(result)) return Result::Failure("Failed to read TAR entry: " + archiveError(input.get()));

        const char* pathname = archive_entry_pathname(entry);
        if (pathname == nullptr) return Result::Failure("TAR entry has no pathname");

        // Path traversal / absolute path protection ("Zip Slip").
        fs::path rel;
        if (!sanitizeEntryPath(pathname, rel))
            return Result::Failure(std::string("Unsafe path in TAR entry: ") + pathname);

        if (rel == ".") continue; // "./" root entry - nothing to extract

        archive_entry_set_pathname(entry, (root / rel).string().c_str());

        // Hard-link targets are paths too: they must be rebased and validated.
        if (const char* hardlink = archive_entry_hardlink(entry); hardlink != nullptr) {
            fs::path linkRel;
            if (!sanitizeEntryPath(hardlink, linkRel) || linkRel == ".")
                return Result::Failure(std::string("Unsafe hardlink target in TAR entry: ") + hardlink);
            archive_entry_set_hardlink(entry, (root / linkRel).string().c_str());
        }

        result = archive_write_header(output.get(), entry);
        if (isFatal(result)) return Result::Failure("Failed to extract TAR entry: " + archiveError(output.get()));

        if (archive_entry_size(entry) > 0) {
            std::string error;
            if (!copyDataToDisk(input.get(), output.get(), error, progressCallback, totalSize, input.get()))
                return Result::Failure(error);
        }

        result = archive_write_finish_entry(output.get());
        if (isFatal(result)) return Result::Failure("Failed to finish extracted entry: " + archiveError(output.get()));
    }

    result = archive_write_close(output.get());
    if (isFatal(result)) return Result::Failure("Failed to finalize extraction: " + archiveError(output.get()));

    result = archive_read_close(input.get());
    if (isFatal(result)) return Result::Failure("Failed to close TAR: " + archiveError(input.get()));

    if (progressCallback) progressCallback(100.0f);
    return Result::Success();
}
