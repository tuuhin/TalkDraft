#include <string>

struct Result {
    bool success{};
    std::string error{};

    static Result Success();
    static Result Failure(std::string message);
};
