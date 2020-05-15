--
-- extension TABLE
--

CREATE TABLE public.extension (
    resource_id bigint NOT NULL,
    key character varying(255) NOT NULL,
    data text NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES public.resource(id) ON UPDATE RESTRICT ON DELETE CASCADE
);


COMMENT ON TABLE public.extension IS 'This table stores any extension metadata for each relevant resource.';

COMMENT ON COLUMN public.extension.resource_id IS 'This value points to the relevant item in the resource table.';
COMMENT ON COLUMN public.extension.key IS 'The extension key for the metadata.';
COMMENT ON COLUMN public.extension.data IS 'The contents of the metadata resource.';

CREATE UNIQUE INDEX idx_extension ON public.extension (resource_id, key);


