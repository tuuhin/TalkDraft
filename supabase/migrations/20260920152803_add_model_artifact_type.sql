CREATE TYPE public.model_artifact_type AS ENUM (
    'BINARY',
    'ZIP',
    'TAR_BZ2'
);

ALTER TABLE public.model_artifact
ADD COLUMN artifact_type public.model_artifact_type
NOT NULL DEFAULT 'BINARY';
