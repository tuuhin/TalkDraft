#pragma once

#include "onnxruntime_cxx_api.h"
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>

class zip_former {
public:
    zip_former();
    ~zip_former();

    bool Initialize(const std::string& encoder_path, const std::string& decoder_path, const std::string& joiner_path,
                    const std::string& tokens_path);

    std::string ProcessPCM(const float* pcm_data, int sample_count);
    void Reset();

private:
    void load_tokens(const std::string& tokens_path);

    Ort::Env _env{ORT_LOGGING_LEVEL_WARNING, "zip_former"};
    Ort::SessionOptions _session_options;

    std::unique_ptr<Ort::Session> _encoder_session;
    std::unique_ptr<Ort::Session> _decoder_session;
    std::unique_ptr<Ort::Session> _joiner_session;

    std::unordered_map<int, std::string> _token_map;
    std::string _acc_transcript;
    bool _isReady = false;
};
