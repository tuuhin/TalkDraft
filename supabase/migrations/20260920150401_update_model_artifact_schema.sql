CREATE TYPE public.model_family AS ENUM (
    'WHISPER',
    'ZIPFORMER'
);

ALTER TABLE public.model_catalog_metadata
RENAME COLUMN version TO local_version;

ALTER TABLE public.model_catalog_metadata
ALTER COLUMN model_family TYPE public.model_family
USING upper(model_family)::public.model_family;

ALTER TYPE public.model_source
ADD VALUE IF NOT EXISTS 'GITHUB';

ALTER TYPE public.model_format
ADD VALUE IF NOT EXISTS 'ONNX';

ALTER TABLE public.model_artifact
ALTER COLUMN sha256 DROP NOT NULL;

ALTER TABLE public.model_artifact
DROP CONSTRAINT IF EXISTS model_artifact_sha256_check;

ALTER TABLE public.model_artifact
ADD CONSTRAINT model_artifact_sha256_check
CHECK (
    sha256 IS NULL
    OR sha256 ~ '^[a-fA-F0-9]{64}$'
);
