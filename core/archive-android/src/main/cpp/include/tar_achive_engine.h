#include "cpp_result.h"

class tar_archive_engine {
public:
    static Result createTar(const std::string& inputPath, const std::string& outputPath);
    static Result extractTar(const std::string& inputPath, const std::string& outputPath);
};
