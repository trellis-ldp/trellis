--
-- binary TABLE
--

CREATE TABLE public.nonrdf (
    id character varying(1024) PRIMARY KEY,
    data bytea NOT NULL
);

COMMENT ON TABLE public.nonrdf IS 'This table can be used to store non-RDF data for a resource.';

COMMENT ON COLUMN public.nonrdf.id IS 'This value is the unique identifier for the non-RDF resource.';
COMMENT ON COLUMN public.nonrdf.data IS 'This holds the non-RDF data itself, limited to 1GB in size';

