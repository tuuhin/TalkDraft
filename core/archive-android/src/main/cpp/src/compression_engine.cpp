#include <archive.h>
#include <archive_entry.h>

#include <cerrno>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <string>
#include <utility>

#include "compression_engine.h"

namespace {

constexpr std::size_t ARCHIVE_BUFFER_SIZE = 64 * 1024;

std::string archiveError(struct archive* archive) {
    const char* error = archive_error_string(archive);

    if (error != nullptr) return error;
    return "Unknown internal library error";
}

bool hasBzip2Header(const std::string& path) {
    std::ifstream input(path, std::ios::binary);

    if (!input.is_open()) return false;
    char header[4];
    input.read(header, sizeof(header));

    return input.gcount() == sizeof(header) && header[0] == 'B' && header[1] == 'Z' && header[2] == 'h' &&
           header[3] >= '1' && header[3] <= '9';
}

} // namespace

Result compression_engine::compressBzip2(const std::string& inputPath, const std::string& outputPath,
                                         const std::function<void(float)>& progressCallback) {
    std::ifstream input(inputPath, std::ios::binary);

    if (!input.is_open()) return Result::Failure("Failed to open input file: " + inputPath);

    uintmax_t totalSize = 0;
    std::error_code ec;
    totalSize = std::filesystem::file_size(inputPath, ec);

    struct archive* archive = archive_write_new();

    if (archive == nullptr) return Result::Failure("Failed to create libarchive writer");

    // Only use the BZip2 compression filter.
    int result = archive_write_add_filter_bzip2(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to initialize BZip2 filter: " + error);
    }

    result = archive_write_set_format_raw(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to configure raw format: " + error);
    }

    result = archive_write_open_filename(archive, outputPath.c_str());

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to open output file: " + error);
    }

    struct archive_entry* entry = archive_entry_new();

    if (entry == nullptr) {
        archive_write_close(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to create archive entry");
    }

    archive_entry_set_pathname(entry, "data");
    archive_entry_set_filetype(entry, AE_IFREG);

    result = archive_write_header(archive, entry);

    archive_entry_free(entry);

    if (result != ARCHIVE_OK && result != ARCHIVE_WARN) {
        std::string error = archiveError(archive);

        archive_write_close(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to write archive header: " + error);
    }

    char buffer[ARCHIVE_BUFFER_SIZE];
    uintmax_t processedBytes = 0;

    while (input.good()) {
        input.read(buffer, sizeof(buffer));

        std::streamsize bytesRead = input.gcount();

        if (bytesRead <= 0) break;

        la_ssize_t written = archive_write_data(archive, buffer, static_cast<size_t>(bytesRead));

        if (written < 0) {
            std::string error = archiveError(archive);

            archive_write_close(archive);
            archive_write_free(archive);

            return Result::Failure("Failed while compressing: " + error);
        }

        if (written != bytesRead) {
            archive_write_close(archive);
            archive_write_free(archive);

            return Result::Failure("Incomplete write while compressing");
        }

        processedBytes += static_cast<size_t>(bytesRead);

        if (progressCallback && totalSize > 0) {
            if (processedBytes > totalSize) {
                processedBytes = totalSize;
            }
            float percentage = static_cast<float>(processedBytes) * 100.0f / static_cast<float>(totalSize);
            if (percentage > 100.0f) percentage = 100.0f;
            progressCallback(percentage);
        }
    }

    if (input.bad()) {
        archive_write_close(archive);
        archive_write_free(archive);

        return Result::Failure("Failed while reading input file");
    }

    result = archive_write_close(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_write_free(archive);

        return Result::Failure("Failed to finalize BZip2 stream: " + error);
    }

    archive_write_free(archive);

    if (progressCallback) progressCallback(100.0f);

    return Result::Success();
}

Result compression_engine::decompressBzip2(const std::string& inputPath, const std::string& outputPath,
                                           const std::function<void(float)>& progressCallback) {

    if (!hasBzip2Header(inputPath)) return Result::Failure("Input file is not a valid BZip2 stream");

    uintmax_t totalSize = 0;
    std::error_code ec;
    totalSize = std::filesystem::file_size(inputPath, ec);

    struct archive* archive = archive_read_new();

    if (archive == nullptr) return Result::Failure("Failed to create libarchive reader");

    int result = archive_read_support_filter_bzip2(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_read_free(archive);

        return Result::Failure("Failed to initialize BZip2 reader: " + error);
    }

    result = archive_read_support_format_raw(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_read_free(archive);

        return Result::Failure("Failed to initialize raw reader: " + error);
    }

    result = archive_read_support_format_empty(archive);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_read_free(archive);

        return Result::Failure("Failed to initialize empty format: " + error);
    }

    result = archive_read_open_filename(archive, inputPath.c_str(), ARCHIVE_BUFFER_SIZE);

    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);
        archive_read_free(archive);

        return Result::Failure("Failed to open BZip2 file: " + error);
    }

    std::filesystem::path outputFile(outputPath);
    std::filesystem::path parent = outputFile.parent_path();

    if (!parent.empty() && !std::filesystem::exists(parent)) {
        archive_read_close(archive);
        archive_read_free(archive);
        return Result::Failure("Output parent directory does not exist: " + parent.string());
    }

    std::ofstream output(outputPath, std::ios::binary | std::ios::trunc);

    if (!output.is_open()) {
        archive_read_close(archive);
        archive_read_free(archive);

        return Result::Failure("Failed to open output file: " + outputPath + ", errno=" + std::to_string(errno) +
                               ", error=" + std::strerror(errno));
    }

    struct archive_entry* entry = nullptr;

    result = archive_read_next_header(archive, &entry);
    if (result == ARCHIVE_EOF) {
        result = archive_read_close(archive);

        if (result != ARCHIVE_OK) {
            std::string error = archiveError(archive);

            archive_read_free(archive);
            output.close();

            std::error_code ecRemove;
            std::filesystem::remove(outputPath, ecRemove);

            return Result::Failure("Failed to finalize empty BZip2 decompression: " + error);
        }

        archive_read_free(archive);
        output.close();

        if (!output.good()) {
            std::error_code ecRemove;
            std::filesystem::remove(outputPath, ecRemove);
            return Result::Failure("Failed to finalize output file");
        }

        // eof reached progress is 100
        if (progressCallback) progressCallback(100.0f);

        return Result::Success();
    }

    if (result != ARCHIVE_OK && result != ARCHIVE_WARN) {
        std::string error = archiveError(archive);

        archive_read_close(archive);
        archive_read_free(archive);
        output.close();

        std::error_code ecRemove;
        std::filesystem::remove(outputPath, ecRemove);

        return Result::Failure("Failed to read archive header: " + error);
    }

    char buffer[ARCHIVE_BUFFER_SIZE];
    while (true) {
        la_ssize_t bytesRead = archive_read_data(archive, buffer, sizeof(buffer));

        if (bytesRead == 0) break;

        if (bytesRead < 0) {
            std::string error = archiveError(archive);

            archive_read_close(archive);
            archive_read_free(archive);

            output.close();

            std::error_code ecRemove;
            std::filesystem::remove(outputPath, ecRemove);

            return Result::Failure("Failed while decompressing: " + error);
        }

        output.write(buffer, bytesRead);

        if (!output.good()) {
            archive_read_close(archive);
            archive_read_free(archive);

            output.close();

            std::error_code ecRemove;
            std::filesystem::remove(outputPath, ecRemove);

            return Result::Failure("Failed while writing decompressed output");
        }

        if (progressCallback && totalSize > 0) {
            auto processedBytes = archive_filter_bytes(archive, 0);
            if (processedBytes < 0) processedBytes = 0;
            if (static_cast<uintmax_t>(processedBytes) > totalSize) {
                processedBytes = static_cast<la_int64_t>(totalSize);
            }
            float percentage = static_cast<float>(processedBytes) * 100.0f / static_cast<float>(totalSize);
            if (percentage > 100.0f) percentage = 100.0f;
            progressCallback(percentage);
        }
    }

    result = archive_read_close(archive);
    if (result != ARCHIVE_OK) {
        std::string error = archiveError(archive);

        archive_read_free(archive);
        output.close();
        std::error_code ecRemove;
        std::filesystem::remove(outputPath, ecRemove);

        return Result::Failure("Failed to finalize BZip2 decompression: " + error);
    }

    archive_read_free(archive);
    output.close();

    if (!output.good()) {
        std::error_code ecRemove;
        std::filesystem::remove(outputPath, ecRemove);
        return Result::Failure("Failed to finalize output file");
    }

    // all done send a 100% complete
    if (progressCallback) progressCallback(100.0f);
    return Result::Success();
}
