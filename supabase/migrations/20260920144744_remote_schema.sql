CREATE TYPE "public"."model_transcription_mode" AS ENUM (
  'BATCHED',
  'STREAMING'
);

ALTER TABLE "public"."model_catalog_metadata"
  ADD COLUMN "transcription_type" public.model_transcription_mode NOT NULL DEFAULT 'BATCHED'::public.model_transcription_mode;

GRANT USAGE ON TYPE "public"."model_transcription_mode" TO "postgres";

