--
-- binary TABLE
--

CREATE TABLE public.binary (
    resource_id bigint NOT NULL,
    data bytea NOT NULL
);

COMMENT ON TABLE public.binary IS 'This table can be used to store binary data for a resource.';

COMMENT ON COLUMN public.binary.resource_id IS 'This value points to the relevant row in the resource table.';
COMMENT ON COLUMN public.binary.data IS 'This holds the binary data itself, limited to 1GB in size';

