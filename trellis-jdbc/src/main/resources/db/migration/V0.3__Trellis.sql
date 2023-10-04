--
-- resource TABLE
--

CREATE TABLE public.resource (
    id bigint PRIMARY KEY,
    subject character varying(1024) NOT NULL,
    interaction_model character varying(255) NOT NULL,
    modified bigint NOT NULL,
    is_part_of character varying(1024),
    deleted boolean DEFAULT false,
    acl boolean DEFAULT false,
    binary_location character varying(1024),
    binary_modified bigint,
    binary_format character varying(255),
    binary_size bigint,
    ldp_member character varying(1024),
    ldp_membership_resource character varying(1024),
    ldp_has_member_relation character varying(1024),
    ldp_is_member_of_relation character varying(1024),
    ldp_inserted_content_relation character varying(1024)
);

CREATE SEQUENCE public.resource_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.resource ALTER COLUMN id SET DEFAULT nextval('public.resource_id_seq');

COMMENT ON TABLE public.resource IS 'This table keeps track of every resource, along with any server-managed properties for the resource.';

COMMENT ON COLUMN public.resource.id IS 'A unique numerical ID for each resource.';
COMMENT ON COLUMN public.resource.subject IS 'The internal IRI for each resource.';
COMMENT ON COLUMN public.resource.interaction_model IS 'The LDP type of each resource.';
COMMENT ON COLUMN public.resource.modified IS 'The modification date for each resource, stored as a long representation of epoch-milliseconds.';
COMMENT ON COLUMN public.resource.is_part_of IS 'The parent resource IRI, if one exists.';
COMMENT ON COLUMN public.resource.deleted IS 'Whether this resource has been deleted (HTTP 410).';
COMMENT ON COLUMN public.resource.acl IS 'Whether this resource has an ACL resource.';
COMMENT ON COLUMN public.resource.binary_location IS 'If this resource is a LDP-NR, this column holds the location of the binary resource.';
COMMENT ON COLUMN public.resource.binary_modified IS 'If this resource is a LDP-NR, this column holds the modification date of the binary resource, stored as a long representation of epoch-milliseconds.';
COMMENT ON COLUMN public.resource.binary_format IS 'If this resource is a LDP-NR, this column holds the MIMEtype of the resource, if known.';
COMMENT ON COLUMN public.resource.binary_size IS 'If this resource is a LDP-NR, this column holds the size of the binary resource, if known.';
COMMENT ON COLUMN public.resource.ldp_member IS 'If this is a LDP-DC or LDP-IC, this column holds the value of ldp:membershipResource but with any fragment IRI removed.';
COMMENT ON COLUMN public.resource.ldp_membership_resource IS 'If this resource is a LDP-DC or LDP-IC, this column holds the ldp:membershipResource value.';
COMMENT ON COLUMN public.resource.ldp_has_member_relation IS 'If this resource is a LDP-DC or LDP-IC, this column holds the ldp:hasMemberRelation value, if present.';
COMMENT ON COLUMN public.resource.ldp_is_member_of_relation IS 'If this resource is a LDP-DC or LDP-IC, this column holds the ldp:isMemberOfRelation value, if present.';
COMMENT ON COLUMN public.resource.ldp_inserted_content_relation IS 'If this resource is a LDP-DC or LDP-IC, this column holds the ldp:insertedContentRelation value.';

INSERT INTO public.resource (id, subject, interaction_model, modified, deleted, acl) VALUES
(0,'trellis:data/','http://www.w3.org/ns/ldp#BasicContainer',0,'f','f');

CREATE INDEX idx_resource_ldp ON public.resource (ldp_member);
CREATE INDEX idx_resource_parent ON public.resource (is_part_of);
CREATE UNIQUE INDEX idx_resource_subject ON public.resource (subject);



--
-- acl TABLE
--

CREATE TABLE public.acl (
    resource_id bigint NOT NULL,
    subject character varying(1024) NOT NULL,
    predicate character varying(1024) NOT NULL,
    object character varying(16383) NOT NULL,
    lang character varying(20),
    datatype character varying(255),
    FOREIGN KEY (resource_id) REFERENCES public.resource(id) ON UPDATE RESTRICT ON DELETE CASCADE
);


COMMENT ON TABLE public.acl IS 'This table stores the WebACL triples for each relevant resource.';

COMMENT ON COLUMN public.acl.resource_id IS 'This value points to the relevant item in the resource table.';
COMMENT ON COLUMN public.acl.subject IS 'The RDF subject for the triple.';
COMMENT ON COLUMN public.acl.predicate IS 'The RDF predicate for the triple.';
COMMENT ON COLUMN public.acl.object IS 'The RDF object for the triple.';
COMMENT ON COLUMN public.acl.lang IS 'If the object is a string literal, this holds the language tag, if relevant.';
COMMENT ON COLUMN public.acl.datatype IS 'If the object is a literal, this holds the datatype IRI of that literal value.';

CREATE INDEX idx_acl ON public.acl (resource_id);



--
-- description TABLE
--

CREATE TABLE public.description (
    resource_id bigint NOT NULL,
    subject character varying(1024) NOT NULL,
    predicate character varying(1024) NOT NULL,
    object character varying(16383) NOT NULL,
    lang character varying(20),
    datatype character varying(255),
    FOREIGN KEY (resource_id) REFERENCES public.resource(id) ON UPDATE RESTRICT ON DELETE CASCADE
);


COMMENT ON TABLE public.description IS 'This table stores all of the user-managed RDF triples on a resource.';

COMMENT ON COLUMN public.description.resource_id IS 'This value points to the relevant item in the resource table.';
COMMENT ON COLUMN public.description.subject IS 'The RDF subject for the triple.';
COMMENT ON COLUMN public.description.predicate IS 'The RDF predicate for the triple.';
COMMENT ON COLUMN public.description.object IS 'The RDF object for the triple.';
COMMENT ON COLUMN public.description.lang IS 'If the object is a string literal, this holds the language tag, if relevant.';
COMMENT ON COLUMN public.description.datatype IS 'If the object is a literal, this holds the datatype IRI of that literal value.';

CREATE INDEX idx_description ON public.description (resource_id);



--
-- extra TABLE
--

CREATE TABLE public.extra (
    resource_id bigint NOT NULL,
    predicate character varying(1024) NOT NULL,
    object character varying(1024) NOT NULL,
    FOREIGN KEY (resource_id) REFERENCES public.resource(id) ON UPDATE RESTRICT ON DELETE CASCADE
);

COMMENT ON TABLE public.extra IS 'This table stores copies of certain user-managed triples for use in response headers.';

COMMENT ON COLUMN public.extra.resource_id IS 'This value points to the relevant item in the resource table.';
COMMENT ON COLUMN public.extra.predicate IS 'The RDF predicate, which becomes the rel value in a Link header.';
COMMENT ON COLUMN public.extra.object IS 'The RDF object, which becomes the URI value in a Link header.';

CREATE INDEX idx_extra ON public.extra (resource_id);



--
-- log TABLE
--

CREATE TABLE public.log (
    id character varying(1024) NOT NULL,
    subject character varying(1024) NOT NULL,
    predicate character varying(1024) NOT NULL,
    object character varying(16383) NOT NULL,
    lang character varying(20),
    datatype character varying(255)
);

COMMENT ON TABLE public.log IS 'This table stores the complete audit log for each resource.';

COMMENT ON COLUMN public.log.id IS 'The id column uses the internal IRI for the resource (resource.subject) since the resource.id value changes across updates.';
COMMENT ON COLUMN public.log.subject IS 'The RDF subject for the triple.';
COMMENT ON COLUMN public.log.predicate IS 'The RDF predicate for the triple.';
COMMENT ON COLUMN public.log.object IS 'The RDF object for the triple.';
COMMENT ON COLUMN public.log.lang IS 'If the object is a string literal, this holds the language tag, if relevant.';
COMMENT ON COLUMN public.log.datatype IS 'If the object is a literal, this holds the datatype IRI of that literal value.';

CREATE INDEX idx_log ON public.log (id);



--
-- memento TABLE
--

CREATE TABLE public.memento (
    id bigint PRIMARY KEY,
    subject character varying(1024) NOT NULL,
    moment bigint NOT NULL
);

CREATE SEQUENCE public.memento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.memento ALTER COLUMN id SET DEFAULT nextval('public.memento_id_seq');

COMMENT ON TABLE public.memento IS 'This table keeps a record of memento locations';

COMMENT ON COLUMN public.memento.id IS 'A unique numerical ID for each memento.';
COMMENT ON COLUMN public.memento.subject IS 'The internal IRI for each resource.';
COMMENT ON COLUMN public.memento.moment IS 'The time of each memento, stored as a long representation of epoch-second.';

CREATE UNIQUE INDEX idx_memento ON public.memento (subject, moment);



--
-- namespaces TABLE
--

CREATE TABLE public.namespaces (
    prefix character varying(255) PRIMARY KEY,
    namespace character varying(1024) NOT NULL
);


COMMENT ON TABLE public.namespaces IS 'This table keeps track of namespace prefixes.';

COMMENT ON COLUMN public.namespaces.prefix IS 'A unique prefix.';
COMMENT ON COLUMN public.namespaces.namespace IS 'The namespace IRI.';

--
-- Data for namespaces TABLE
--

INSERT INTO public.namespaces (prefix, namespace) VALUES
('acl','http://www.w3.org/ns/auth/acl#'),
('as','https://www.w3.org/ns/activitystreams#'),
('dc','http://purl.org/dc/terms/'),
('dc11','http://purl.org/dc/elements/1.1/'),
('geo','http://www.w3.org/2003/01/geo/wgs84_pos#'),
('ldp','http://www.w3.org/ns/ldp#'),
('owl','http://www.w3.org/2002/07/owl#'),
('prov','http://www.w3.org/ns/prov#'),
('rdf','http://www.w3.org/1999/02/22-rdf-syntax-ns#'),
('rdfs','http://www.w3.org/2000/01/rdf-schema#'),
('schema','http://schema.org/'),
('skos','http://www.w3.org/2004/02/skos/core#'),
('time','http://www.w3.org/2006/time#'),
('xsd','http://www.w3.org/2001/XMLSchema#');

