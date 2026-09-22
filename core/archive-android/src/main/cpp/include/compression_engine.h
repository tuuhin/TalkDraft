#include "cpp_result.h"
#include <string>

class compression_engine {
public:
    static Result compressBzip2(const std::string& inputPath, const std::string& outputPath);
    static Result decompressBzip2(const std::string& inputPath, const std::string& outputPath);
};
