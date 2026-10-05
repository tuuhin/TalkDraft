#pragma once

#include "cpp_result.h"
#include <functional>
#include <string>

class tar_archive_engine {
public:
    static Result createTar(const std::string& inputPath, const std::string& outputPath,
                            const std::function<void(float)>& progressCallback = nullptr);
    static Result extractTar(const std::string& inputPath, const std::string& outputPath,
                             const std::function<void(float)>& progressCallback = nullptr);
};
