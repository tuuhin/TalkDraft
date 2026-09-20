ALTER TABLE public.model_catalog_metadata
ADD COLUMN description text;

ALTER TABLE public.model_catalog_metadata
ADD COLUMN is_default boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX model_catalog_one_default_per_type
ON public.model_catalog_metadata (transcription_type)
WHERE is_default = true;
