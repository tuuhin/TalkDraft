#include "cpp_result.h"
#include <functional>
#include <string>

class compression_engine {
public:
    static Result compressBzip2(const std::string& inputPath, const std::string& outputPath,
                                const std::function<void(float)>& progressCallback = nullptr);
    static Result decompressBzip2(const std::string& inputPath, const std::string& outputPath,
                                  const std::function<void(float)>& progressCallback = nullptr);
};
