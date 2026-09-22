#include "cpp_result.h"

Result Result::Success() { return {.success = true, .error = {}}; }

Result Result::Failure(std::string message) { return {.success = false, .error = std::move(message)}; }
