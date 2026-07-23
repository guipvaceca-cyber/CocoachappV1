-- Table: labo_situationpropose
-- Stores training situations proposed by coaches.

CREATE TABLE public.labo_situationpropose (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    author_id UUID REFERENCES auth.users(id),
    author_nickname TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT NOT NULL, -- TECHNIQUE, TACTIQUE, PHYSIQUE, MENTAL
    description TEXT NOT NULL,
    focal_points JSONB DEFAULT '[]'::jsonb, -- Array of FocalPoint objects
    crash_tests JSONB DEFAULT '[]'::jsonb, -- Array of CrashTestFeedback objects
    difficulty INTEGER DEFAULT 1,
    setup_image_url TEXT,
    versions_count INTEGER DEFAULT 1,
    is_validated BOOLEAN DEFAULT false,
    club_id TEXT -- Optional, for filtering by club if needed
);

-- RLS (Row Level Security)
ALTER TABLE public.labo_situationpropose ENABLE ROW LEVEL SECURITY;

-- Allow all authenticated users to read validated resources
CREATE POLICY "Allow public read for validated resources"
ON public.labo_situationpropose FOR SELECT
TO authenticated
USING (is_validated = true);

-- Allow authors to read their own (even if not validated)
CREATE POLICY "Allow authors to read own resources"
ON public.labo_situationpropose FOR SELECT
TO authenticated
USING (auth.uid() = author_id);

-- Allow all authenticated users to insert a new resource
CREATE POLICY "Allow authenticated insert"
ON public.labo_situationpropose FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = author_id);
