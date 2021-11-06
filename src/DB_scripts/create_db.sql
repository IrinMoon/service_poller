CREATE DATABASE "ServicePollerDB"
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_Israel.utf8'
    LC_CTYPE = 'English_Israel.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

CREATE TABLE IF NOT EXISTS public.services
(
    id integer NOT NULL DEFAULT nextval('services_service_id_seq'::regclass),
    name character varying(45) COLLATE pg_catalog."default" NOT NULL,
    url text COLLATE pg_catalog."default" NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT services_pkey PRIMARY KEY (id),
    CONSTRAINT "url uniqueness" UNIQUE (url)
)

TABLESPACE pg_default;

ALTER TABLE public.services
    OWNER to postgres;