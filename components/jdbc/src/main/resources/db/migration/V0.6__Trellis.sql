--
-- binary TABLE
--

CREATE TABLE public.binary (
    id character varying(1024) PRIMARY KEY,
    data bytea NOT NULL
);

COMMENT ON TABLE public.binary IS 'This table can be used to store binary data for a resource.';

COMMENT ON COLUMN public.binary.id IS 'This value is the unique identifier for the binary.';
COMMENT ON COLUMN public.binary.data IS 'This holds the binary data itself, limited to 1GB in size';

