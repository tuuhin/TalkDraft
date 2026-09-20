
UPDATE public.model_catalog_metadata
SET
    display_name = 'Whisper Base',
    description = 'Balanced transcription with good accuracy and moderate resource usage.',
    updated_at = now()
WHERE model_family = 'WHISPER'
  AND variant = 'base-q5_1'
  AND local_version = '1.0.0';


UPDATE public.model_catalog_metadata
SET
    display_name = 'Whisper Small',
    description = 'Higher transcription accuracy with increased processing and storage requirements.',
    updated_at = now()
WHERE model_family = 'WHISPER'
  AND variant = 'small-q5_1'
  AND local_version = '1.0.0';


UPDATE public.model_catalog_metadata
SET
    display_name = 'Fast Realtime',
    description = 'Quick realtime transcription designed for low latency and low resource usage.',
    updated_at = now()
WHERE model_family = 'ZIPFORMER'
  AND variant = 'transducer_small'
  AND local_version = '1.0.0';


UPDATE public.model_catalog_metadata
SET
    display_name = 'Realtime',
    description = 'Realtime transcription using a larger model for potentially better recognition.',
    updated_at = now()
WHERE model_family = 'ZIPFORMER'
  AND variant = 'transducer_standard'
  AND local_version = '1.0.0';
