INSERT INTO public.model_artifact (
    catalog_id,
    source,
    repository,
    revision,
    artifact_path,
    format,
    languages,
    size_bytes,
    sha256
)
SELECT
    id,
    'GITHUB',
    'k2-fsa/sherpa-onnx',
    'asr-models',
    'sherpa-onnx-streaming-zipformer-en-2023-06-21.tar.bz2',
    'ONNX',
    ARRAY['en'],
    506956414,
    NULL
FROM public.model_catalog_metadata
WHERE model_family = 'ZIPFORMER'
  AND variant = 'transducer_small'
  AND local_version = '1.0.0';


INSERT INTO public.model_artifact (
    catalog_id,
    source,
    repository,
    revision,
    artifact_path,
    format,
    languages,
    size_bytes,
    sha256
)
SELECT
    id,
    'GITHUB',
    'k2-fsa/sherpa-onnx',
    'asr-models',
    'sherpa-onnx-streaming-zipformer-en-2023-06-26.tar.bz2',
    'ONNX',
    ARRAY['en'],
    310414022,
    NULL
FROM public.model_catalog_metadata
WHERE model_family = 'ZIPFORMER'
  AND variant = 'transducer_standard'
  AND local_version = '1.0.0';
