--
-- PostgreSQL database dump
--

-- Dumped from database version 10.4
-- Dumped by pg_dump version 10.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: project_status; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.project_status AS ENUM (
    'On Going',
    'Closed',
    'Planned',
    'On Hold',
    'Canceled'
);


--
-- Name: technical_office_type; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.technical_office_type AS ENUM (
    'Front Office',
    'Back Office'
);


--
-- Name: fn_calc_project_margin(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_calc_project_margin() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.margin_budget  := NEW.revenue_budget - NEW.cost_budget;
    NEW.project_margin := CASE
        WHEN NEW.revenue_budget = 0 THEN 0
        ELSE (NEW.revenue_budget - NEW.cost_budget) / NEW.revenue_budget
    END;
    RETURN NEW;
END;
$$;


--
-- Name: fn_guard_soft_deleted_project(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_guard_soft_deleted_project() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot update an archived project (id=%). Restore it first.', OLD.id;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_set_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: app_user; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_user (
    id bigint NOT NULL,
    full_name character varying(200) NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(255),
    role character varying(50) DEFAULT 'USER'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_app_user_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying, 'PM'::character varying, 'BUM'::character varying])::text[])))
);


--
-- Name: TABLE app_user; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.app_user IS 'Utilisateurs de application.';


--
-- Name: COLUMN app_user.role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.app_user.role IS 'Role: USER | PM | BUM | ADMIN.';


--
-- Name: COLUMN app_user.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.app_user.deleted_at IS 'Soft delete.';


--
-- Name: app_user_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.app_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: app_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.app_user_id_seq OWNED BY public.app_user.id;


--
-- Name: bu; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bu (
    id character varying(10) NOT NULL,
    name character varying(150) NOT NULL,
    trigram character varying(5) NOT NULL,
    bum_name character varying(150) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE bu; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.bu IS 'Business Units de la societe.';


--
-- Name: COLUMN bu.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bu.id IS 'Code ERP naturel (ex: I50001).';


--
-- Name: COLUMN bu.trigram; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bu.trigram IS 'Code court utilise dans les noms de projets.';


--
-- Name: COLUMN bu.bum_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bu.bum_name IS 'Nom complet du Business Unit Manager.';


--
-- Name: COLUMN bu.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.bu.is_active IS 'FALSE = BU archivee, masquee dans les dropdowns.';


--
-- Name: customer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    trigram character varying(10) NOT NULL,
    customer_group character varying(150),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE customer; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.customer IS 'Clients / donneurs ordre.';


--
-- Name: COLUMN customer.trigram; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.customer.trigram IS 'Code court utilise dans les noms de projets.';


--
-- Name: COLUMN customer.customer_group; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.customer.customer_group IS 'Groupe corporate parent.';


--
-- Name: customer_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_id_seq OWNED BY public.customer.id;


--
-- Name: engagement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.engagement (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    engagement_type character varying(10) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE engagement; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.engagement IS 'Modele engagement / facturation.';


--
-- Name: COLUMN engagement.engagement_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.engagement.engagement_type IS 'Code court (T&M, UoW, TK, WP).';


--
-- Name: engagement_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.engagement_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: engagement_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.engagement_id_seq OWNED BY public.engagement.id;


--
-- Name: engineering_discipline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.engineering_discipline (
    id bigint NOT NULL,
    name character varying(200) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE engineering_discipline; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.engineering_discipline IS 'Domaine technique du projet.';


--
-- Name: engineering_discipline_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.engineering_discipline_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: engineering_discipline_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.engineering_discipline_id_seq OWNED BY public.engineering_discipline.id;


--
-- Name: front_financier; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.front_financier (
    id bigint NOT NULL,
    code character varying(10) NOT NULL,
    label character varying(150),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE front_financier; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.front_financier IS 'Entites legales de la societe.';


--
-- Name: COLUMN front_financier.code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.front_financier.code IS 'Code court (SMA, SMAF, STS...).';


--
-- Name: front_financier_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.front_financier_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: front_financier_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.front_financier_id_seq OWNED BY public.front_financier.id;


--
-- Name: industry; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.industry (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    trigram character varying(5) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE industry; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.industry IS 'Secteur industriel du projet.';


--
-- Name: industry_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.industry_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: industry_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.industry_id_seq OWNED BY public.industry.id;


--
-- Name: project; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project (
    id bigint NOT NULL,
    project_code character varying(30),
    project_name character varying(300),
    project_year smallint DEFAULT (date_part('year'::text, now()))::smallint NOT NULL,
    project_name_legacy character varying(300),
    front_financier_id bigint NOT NULL,
    activity character varying(200) NOT NULL,
    revenue_budget numeric(15,2) DEFAULT 0 NOT NULL,
    cost_budget numeric(15,2) DEFAULT 0 NOT NULL,
    margin_budget numeric(15,2) DEFAULT 0 NOT NULL,
    project_margin numeric(7,6) DEFAULT 0 NOT NULL,
    start_date date,
    end_date date,
    project_manager_id bigint NOT NULL,
    bu_id character varying(10) NOT NULL,
    customer_id bigint NOT NULL,
    industry_id bigint NOT NULL,
    engineering_discipline_id bigint NOT NULL,
    function_id bigint,
    engagement_id bigint NOT NULL,
    major_project boolean DEFAULT false NOT NULL,
    technical_office public.technical_office_type DEFAULT 'Back Office'::public.technical_office_type NOT NULL,
    status public.project_status DEFAULT 'On Going'::public.project_status NOT NULL,
    deleted_at timestamp with time zone,
    created_by_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    project_id character varying(50),
    CONSTRAINT ck_project_budgets CHECK (((revenue_budget >= (0)::numeric) AND (cost_budget >= (0)::numeric))),
    CONSTRAINT ck_project_dates CHECK (((end_date IS NULL) OR (start_date IS NULL) OR (end_date >= start_date))),
    CONSTRAINT ck_project_year CHECK (((project_year >= 2020) AND (project_year <= 2100)))
);


--
-- Name: TABLE project; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.project IS 'Table centrale des projets.';


--
-- Name: COLUMN project.project_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.project.project_code IS 'Code saisi par utilisateur. NULL pour lignes BizDev.';


--
-- Name: COLUMN project.margin_budget; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.project.margin_budget IS 'Calcule par backend: revenue_budget - cost_budget.';


--
-- Name: COLUMN project.project_margin; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.project.project_margin IS 'Calcule par backend: margin / revenue.';


--
-- Name: COLUMN project.deleted_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.project.deleted_at IS 'Soft delete.';


--
-- Name: project_audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_audit_log (
    id bigint NOT NULL,
    project_id bigint NOT NULL,
    changed_by bigint,
    change_type character varying(10) NOT NULL,
    changed_at timestamp with time zone DEFAULT now() NOT NULL,
    old_values jsonb,
    new_values jsonb
);


--
-- Name: TABLE project_audit_log; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.project_audit_log IS 'Trail audit append-only des modifications projets.';


--
-- Name: project_audit_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_audit_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_audit_log_id_seq OWNED BY public.project_audit_log.id;


--
-- Name: project_function; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_function (
    id bigint NOT NULL,
    name character varying(250) NOT NULL,
    engineering_discipline_id bigint,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE project_function; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.project_function IS 'Referentiel des postes / fonctions ingenierie.';


--
-- Name: project_function_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_function_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_function_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_function_id_seq OWNED BY public.project_function.id;


--
-- Name: project_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.project_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.project_id_seq OWNED BY public.project.id;


--
-- Name: refresh_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_token (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token_hash character varying(255) NOT NULL,
    issued_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked_at timestamp with time zone,
    device_info character varying(255)
);


--
-- Name: TABLE refresh_token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.refresh_token IS 'Refresh tokens JWT pour rotation securisee.';


--
-- Name: refresh_token_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_token_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_token_id_seq OWNED BY public.refresh_token.id;


--
-- Name: vw_project_detail; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_project_detail AS
 SELECT p.id,
    p.project_code,
    p.project_name,
    p.project_year,
    p.project_name_legacy,
    ff.id AS front_financier_id,
    ff.code AS front_financier,
    ff.label AS front_financier_label,
    u.full_name AS project_manager,
    u.email AS pm_email,
    b.id AS bu_id,
    b.name AS bu_name,
    b.trigram AS bu_trigram,
    b.bum_name,
    c.id AS customer_id,
    c.name AS customer_name,
    c.trigram AS customer_trigram,
    c.customer_group,
    i.id AS industry_id,
    i.name AS industry_name,
    i.trigram AS industry_trigram,
    ed.id AS engineering_discipline_id,
    ed.name AS engineering_discipline,
    pf.id AS function_id,
    pf.name AS function_name,
    en.id AS engagement_id,
    en.name AS engagement,
    en.engagement_type,
    p.activity,
    p.major_project,
    p.technical_office,
    p.status,
    p.start_date,
    p.end_date,
    p.revenue_budget,
    p.cost_budget,
    p.margin_budget,
    p.project_margin,
    creator.full_name AS created_by,
    p.created_at,
    p.updated_at,
    p.deleted_at
   FROM (((((((((public.project p
     JOIN public.front_financier ff ON ((ff.id = p.front_financier_id)))
     JOIN public.app_user u ON ((u.id = p.project_manager_id)))
     JOIN public.bu b ON (((b.id)::text = (p.bu_id)::text)))
     JOIN public.customer c ON ((c.id = p.customer_id)))
     JOIN public.industry i ON ((i.id = p.industry_id)))
     JOIN public.engineering_discipline ed ON ((ed.id = p.engineering_discipline_id)))
     LEFT JOIN public.project_function pf ON ((pf.id = p.function_id)))
     JOIN public.engagement en ON ((en.id = p.engagement_id)))
     JOIN public.app_user creator ON ((creator.id = p.created_by_id)));


--
-- Name: VIEW vw_project_detail; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.vw_project_detail IS 'Vue detail complete projet.';


--
-- Name: vw_project_archive; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_project_archive AS
 SELECT vw_project_detail.id,
    vw_project_detail.project_code,
    vw_project_detail.project_name,
    vw_project_detail.project_year,
    vw_project_detail.project_name_legacy,
    vw_project_detail.front_financier_id,
    vw_project_detail.front_financier,
    vw_project_detail.front_financier_label,
    vw_project_detail.project_manager,
    vw_project_detail.pm_email,
    vw_project_detail.bu_id,
    vw_project_detail.bu_name,
    vw_project_detail.bu_trigram,
    vw_project_detail.bum_name,
    vw_project_detail.customer_id,
    vw_project_detail.customer_name,
    vw_project_detail.customer_trigram,
    vw_project_detail.customer_group,
    vw_project_detail.industry_id,
    vw_project_detail.industry_name,
    vw_project_detail.industry_trigram,
    vw_project_detail.engineering_discipline_id,
    vw_project_detail.engineering_discipline,
    vw_project_detail.function_id,
    vw_project_detail.function_name,
    vw_project_detail.engagement_id,
    vw_project_detail.engagement,
    vw_project_detail.engagement_type,
    vw_project_detail.activity,
    vw_project_detail.major_project,
    vw_project_detail.technical_office,
    vw_project_detail.status,
    vw_project_detail.start_date,
    vw_project_detail.end_date,
    vw_project_detail.revenue_budget,
    vw_project_detail.cost_budget,
    vw_project_detail.margin_budget,
    vw_project_detail.project_margin,
    vw_project_detail.created_by,
    vw_project_detail.created_at,
    vw_project_detail.updated_at,
    vw_project_detail.deleted_at
   FROM public.vw_project_detail
  WHERE (vw_project_detail.deleted_at IS NOT NULL);


--
-- Name: VIEW vw_project_archive; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.vw_project_archive IS 'Vue projets archives (soft-deleted).';


--
-- Name: vw_project_list; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.vw_project_list AS
 SELECT p.id,
    p.project_code,
    p.project_name,
    p.project_year,
    ff.code AS front_financier,
    b.name AS bu_name,
    b.trigram AS bu_trigram,
    c.name AS customer_name,
    c.trigram AS customer_trigram,
    p.activity,
    p.revenue_budget,
    p.cost_budget,
    p.margin_budget,
    p.project_margin,
    p.status,
    p.major_project,
    p.start_date,
    p.end_date
   FROM (((public.project p
     JOIN public.front_financier ff ON ((ff.id = p.front_financier_id)))
     JOIN public.bu b ON (((b.id)::text = (p.bu_id)::text)))
     JOIN public.customer c ON ((c.id = p.customer_id)))
  WHERE (p.deleted_at IS NULL);


--
-- Name: VIEW vw_project_list; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.vw_project_list IS 'Vue liste projets actifs.';


--
-- Name: app_user id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user ALTER COLUMN id SET DEFAULT nextval('public.app_user_id_seq'::regclass);


--
-- Name: customer id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.customer_id_seq'::regclass);


--
-- Name: engagement id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engagement ALTER COLUMN id SET DEFAULT nextval('public.engagement_id_seq'::regclass);


--
-- Name: engineering_discipline id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engineering_discipline ALTER COLUMN id SET DEFAULT nextval('public.engineering_discipline_id_seq'::regclass);


--
-- Name: front_financier id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.front_financier ALTER COLUMN id SET DEFAULT nextval('public.front_financier_id_seq'::regclass);


--
-- Name: industry id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.industry ALTER COLUMN id SET DEFAULT nextval('public.industry_id_seq'::regclass);


--
-- Name: project id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project ALTER COLUMN id SET DEFAULT nextval('public.project_id_seq'::regclass);


--
-- Name: project_audit_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_audit_log ALTER COLUMN id SET DEFAULT nextval('public.project_audit_log_id_seq'::regclass);


--
-- Name: project_function id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_function ALTER COLUMN id SET DEFAULT nextval('public.project_function_id_seq'::regclass);


--
-- Name: refresh_token id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token ALTER COLUMN id SET DEFAULT nextval('public.refresh_token_id_seq'::regclass);


--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user (id, full_name, email, password_hash, role, is_active, deleted_at, created_at, updated_at) FROM stdin;
2	Zakarya BENDAHMANE	zakarya.bendahmane@segulagrp.com	$2b$10$QMjfzGTV9Bxti.yHH0jhauwfgF1vqrdrgKbTGjQOyWDFq6SF2QTcC	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
3	Achraf MAKBOUL	achraf.makboul@segulagrp.com	$2b$10$soGDKW0m1TqKiFiUfec7ke11Bv/i2ql/I1pmfoWHKxkK/emlsYjVi	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
4	Ali TARRASS FILALI	alifilali.tarrass@segulagrp.com	$2b$10$.82lI.7l471eXoHr01LWqe8tZbVjZMMWamiFmDei1umzpXgBwUmPi	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
5	Ayoub EL KHAIRI	ayoub.elkhairi@segulagrp.com	$2b$10$XYsmDoq/bEC5iGr04snetuvpOy73.SfohSt24wfSCzcvwcKDKj6Bi	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
6	El Mehdi ALBAZ	Elmehdi.albaz@segulagrp.com	$2b$10$KVHCrR3HDhwCm1/MoFj5SuzpQDpohpIw32BEHu13dVJnzz2wvUt7C	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
7	Fadoua HABABOU	fadoua.hababou@segulagrp.com	$2b$10$uDvPecgdxtXAZZBFNoKlT.CPBXjfdN.5t6lIYx8/lKYiP957YEGTa	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
8	Fadwa HARTI	fadwa.harti@segulagrp.com	$2b$10$NliAdYb9aFQ9rKLu0.3pS.4C7Kr80jmAg/YKS5qWtcd/HGFEDUoKy	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
9	Fatima Zahra ZIDANI ALAOUI	fatimazahra.zidani@segulagrp.com	$2b$10$WHJVGskNPxss0pP/tYJd0u09/c9rl3SeN6LNNfKt0bjHuK65.t3fu	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
10	Faycal EL ASRI	faycal.elasri@segulagrp.com	$2b$10$X4YrXW/YoRxGf8p90aN/vOnjcdDknoqvSq4emWsj/ub8QeBobS6Ri	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
11	Ghita BENADDI	ghita.benaddi@segulagrp.com	$2b$10$zFhabsZV8MNjg8BCNNjHx.UGFGVbWONQ5wL7ivkDY.h/a4jOr5FzO	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
12	Hamza BENNIS	hamza.bennis@segulagrp.com	$2b$10$qicn.jAHpIlb05bdHRWcZu4gDOAhOdTHBnsJLOjmvNWNgI3o0vRZS	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
13	Hamza BERRADA	hamza.berrada@segulagrp.com	$2b$10$0gdWNOwHLtLze84tyfP/dOB5mrAhDKeiAJ8newS1cnw9z3p/qRrK6	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
14	Imane BENAJIBA	imane.benajiba@segulagrp.com	$2b$10$MlCL2wv5vrf4FleE74IO5.ft/li3hANDm..9/wVs9Bh8N9bOPdmnO	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
15	Imane RABEH	imane.rabeh@segulagrp.com	$2b$10$GCqd0TaeVgMBU.Eo3oTp2eqn1U4PmpXgq6lGWt4YhGkMlXHeibjiu	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
16	Ismail AKOUAR	ismail.akouar@segulagrp.com	$2b$10$2Jslr4TzHPvIS6aic8CYS.c4pLHezmP/w3YXwAPEtwVNLd57t/7wK	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
17	Jihane AZIZI	jihane.azizi@segulagrp.com	$2b$10$5fJHzxhydHFtUPS1jUUHa.QO0sakEvvY5b3Mlg0qjxkE3oPWmQSC6	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
18	Lamyae EL HANKAR	lamyae.elhankar@segulagrp.com	$2b$10$XiIVlKIARN1QkXy.C8pXK.pJzqzBjC.EvGorIS9hqHDGyaQfaX7LO	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
19	Hafid Mohamed ZAOUG	Hafidmohamed.zaoug@segulagrp.com	$2b$10$9f.ljWVRXjUnE6PsCCTTTuSEaWxpEWYE6eNQH7Xvmu35eA21J2twq	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
20	Omar LEMDEGHRI	omar.lemdeghri@segulagrp.com	$2b$10$EtTrW/FyAfubP6fagqhDAe0tvduPHodfhXT5OBYUspkB4sMmo8VsS	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
21	Safaa SNAIKI	safaa.snaiki@segulagrp.com	$2b$10$G58VScpryJ8LarOSX2C55.xf4oj9rl69FSYyD1VXLxrMl1bLx6mxS	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
22	Sahar BOUTAYEB	sahar.boutayeb@segulagrp.com	$2b$10$SAD0w7B6bJt1IzOt4y/A1.RL4ia.ZiGOPWIBCXdml0xB.gQwBAVu2	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
23	Hamza MOUBARIKI	hamza.moubariki@segulagrp.com	$2b$10$W8.TF58Py0ae/3Gv29MgHuQaJAwuSG1yWh63EmFNBAHIFf2x7sDF.	PM	t	\N	2026-03-17 09:30:22.162422+00	2026-03-17 09:30:22.162422+00
24	Administrateur	admin.segula@segulgrp.com	$2b$10$8Qn6z9W0mB6M7gZP7o9dOeZpP9B7s9m8nQz9WkF0JmYp3lH6sX2yK	ADMIN	t	\N	2026-03-17 10:07:29.89222+00	2026-03-17 10:07:29.89222+00
\.


--
-- Data for Name: bu; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bu (id, name, trigram, bum_name, is_active, created_at, updated_at) FROM stdin;
I50001	Mechanical Engineering	MEC	Ghita BENADDI	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I50002	Performance & Data Management	PDM	Jihane AZIZI	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I50003	System Development & Validation	SDV	Hamza BENNIS	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I52003	System Development & Validation Alt	SDV	Hamza BENNIS	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I52500	Diversification	DIV	Achraf MAKBOUL	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I52706	Supply Chain & Logistique	SCL	Hamza BERRADA	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I50ACT	Manufacturing & Industrial Services	MIS	Fatima Zahra ZIDANI	t	2026-03-11 15:35:04.123758+00	2026-03-11 15:35:04.123758+00
I52704	Manufacturing & Industrial Services	MIS	Fatima Zahra ZIDANI	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
I52700	System Development & Validation	SDV	Hamza BENNIS	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
I52701	Mechanical Engineering	MEC	Ghita BENADDI	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
I52702	Performance & Data Management	PDM	Jihane AZIZI	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
I52703	System Development & Validation	SDV	Hamza BENNIS	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
I52705	Mechanical Engineering	MEC	Ghita BENADDI	t	2026-03-16 13:02:05.013059+00	2026-03-16 13:02:05.013059+00
\.


--
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customer (id, name, trigram, customer_group, is_active, created_at, updated_at) FROM stdin;
1	Stellantis	STL	Stellantis	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
2	Renault	RSA	Renault	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
3	SEAT	SEAT	Volkswagen	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
4	Alpine	ALP	Renault	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
5	Alstom	ALS	Alstom	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
6	Technicon	TCN	Technicon	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
7	SEWS	SWS	SEWS	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
8	VAY	VAY	VAY	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
9	Martur	MAR	Martur	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
10	MAN	MAN	Volkswagen	t	2026-03-11 15:35:04.131033+00	2026-03-11 15:35:04.131033+00
11	ACC	ACC	ACC	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
12	Africa Technical Center	ATC	Stellantis	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
13	Airbus	ARB	Airbus	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
14	BMW	BMW	Bayerische Motoren Werke AG	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
15	Bombardier	BOM	Bombardier	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
16	CAF	CAF	CAF	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
17	DHL	DHL	DHL	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
18	Hexcel	HEX	Hexcel	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
19	InnovX	INX	OCP	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
20	Mercedes	DAI	Daimler	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
21	SOMACA	SOM	Renault	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
22	Total Energie	TEN	Total Energie	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
23	Volkswagen	VAG	Volkswagen	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
24	CARIAD	CAR	Volkswagen	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
25	SKODA	SKO	Volkswagen	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
26	Polydesign	PDE	Exco Technologies	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
27	MASEN	MAS	MASEN	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
28	SkyFuel	SFI	SkyFuel, Inc.	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
29	Nicomatic	NIG	Nicomatic Group	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
30	Shanghai Jinxiu Shanhe	SJS	Shanghai Jinxiu Shanhe Automotive	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
31	NARSA	NAR	NARSA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
32	FIBERZONE	FZN	FiberZone Networks	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
33	CSEE	CSE	MERMEC	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
34	CITE D'INNOVATION	CDI	OTHERS	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
35	TBD	TBD	OTHERS	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
36	Hyundai Rothem	ROT	Hyundai	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
37	ONCF	ONC	ONCF	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
38	Segula Australia	SAU	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
39	Technicon French	TFR	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
40	Technicon Design Limited	TDL	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
41	TECHNICON DESIGN DEUTSCHLAND	TDD	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
42	TECHNICON DESIGN CORPORATION	TDC	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
43	TECHNICON DESIGN AUTOMOTIVE CONSULTING LTD SHANGHAI	TDS	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
44	Technicon Design	TDE	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
45	SMA	SMA	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
46	Segula, France	SEF	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
47	Segula USA	SUS	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
48	Segula US	SU2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
49	Segula UK	SUK	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
50	Segula Turkey	STT	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
51	Segula Tecnologias Procesos SI.	STP	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
52	SEGULA TECNOLOGIAS I+D	ST1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
53	SEGULA TECNOLOGIAS ESPANA, S.A.	SE1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
54	SEGULA TECNOLOGIAS ESPANA, S.A. I+D	SE2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
55	SEGULA TECNOLOGIAS ESPANA SAU	SE3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
56	SEGULA TECNOLOGIAS ESPANA, S,A,	SE4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
57	SEGULA TECNOLOGIAS	SE5	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
58	Segula Technology Israel Ltd.	STI	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
59	SEGULA TECNOLOGIAS WUHAN CO.LTD Shanghai BU	SW1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
60	Segula Technologies Wuhan Co.,Ltd Shanghai Branch	SW2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
61	Segula Technologies Wuhan Co.,Ltd	SW3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
62	Segula Technologies USA, Inc	SU3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
63	Segula Technologies USA	SU4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
64	Segula Technologies UK Limited	SK1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
65	Segula Technologies Spain	SS1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
66	Segula Technologies Services GmbH	SS2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
67	Segula Technologies Service GmbH (Portage)	SS3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
68	SEGULA TECHNOLOGIES POLAND	SP1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
69	SEGULA TECHNOLOGIES LTD	SL1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
70	SEGULA TECHNOLOGIES ITALIE	SI1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
71	SEGULA TECHNOLOGIES ITALIA SRL	SI2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
72	Segula Technologies Hungary Kft.	SH1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
73	SEGULA Technologies Experts AG (Portage)	SE6	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
74	SEGULA Technologies Engineering	SE7	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
75	SEGULA Technologies Experts AG	SE8	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
76	SEGULA TECHNOLOGIES CANADA INC.	SC1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
77	SEGULA TECHNOLOGIES AUSTRIA GMBH	SA1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
78	SEGULA TECHNOLOGIES AUSTRALIA Pty LTD	SA2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
79	SEGULA Technologies Australia	SA3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
80	SEGULA Technologies AB	SA4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
81	SEGULA TECHNOLOGIE USA, (GRISWOLD)	SG1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
82	SEGULA TECHNOLOGIE TUNISIE	SG2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
83	SEGULA Russia	SR1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
84	SEGULA RUS	SR2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
85	SEGULA MATRA I+D	SM1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
86	SEGULA MATRA FRANCE	SM2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
87	SEGULA MATRA AUTOMOTIVE -NO USAR	SM3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
88	SEGULA MATRA AUTOMOTIVE	SM4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
89	Segula Matra Automotive	SM5	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
90	SEGULA MATRA	SM6	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
91	NPU - SEGULA MATRA AUTOMOTIVE	NP1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
92	SEGULA MAROC AFRICA SA	SM7	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
93	Segula Italy	SI3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
94	SEGULA INTEGRATION S.R.L.	SI4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
95	Segula Indien	SI5	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
96	SEGULA GROUPE	SG3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
97	Segula GMBH	SG4	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
98	Segula Germany	SG5	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
99	SEGULA FRANCIA	SF1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
100	SEGULA FRANCE	SF2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
101	SEGULA DENMARK	SD1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
102	SEGULA ESPANA	SE9	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
103	Segula Engineering Services	SE0	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
104	SEGULA ENGINEERING	SE10	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
105	Segula China	SC2	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
106	Segula Canada	SC3	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
107	Segula Austria	SA5	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
108	SEGULA AUS	SA6	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
109	Matra TAS	MT1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
110	HARDWARE INFOGERANCE	HI1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
111	EK Design Koln GmbH	EK1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
112	TD CHINE	TC1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
113	TD EUR	TE1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
114	TD US	TU1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
115	GROUPE SEGULA	GS1	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
116	SEGULA	SEG	SEGULA	t	2026-03-16 11:48:55.099631+00	2026-03-16 11:48:55.099631+00
\.


--
-- Data for Name: engagement; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.engagement (id, name, engagement_type, is_active, created_at, updated_at) FROM stdin;
1	Time & Material	T&M	t	2026-03-11 15:35:04.150915+00	2026-03-11 15:35:04.150915+00
2	Unit of work (deliverables)	UoW	t	2026-03-11 15:35:04.150915+00	2026-03-11 15:35:04.150915+00
3	Turnkey	TK	t	2026-03-11 15:35:04.150915+00	2026-03-11 15:35:04.150915+00
4	Work Package	WP	t	2026-03-11 15:35:04.150915+00	2026-03-11 15:35:04.150915+00
\.


--
-- Data for Name: engineering_discipline; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.engineering_discipline (id, name, is_active, created_at, updated_at) FROM stdin;
1	Conception et developpement produit	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
2	Simulation, calculs et modelisation	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
3	Industrialisation, production et maintenance	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
4	Gestion, management et support technique	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
5	Data & Digital Engineering	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
6	System Engineering	t	2026-03-11 15:35:04.145531+00	2026-03-11 15:35:04.145531+00
7	Electrical / Electronic	t	2026-03-16 09:25:39.124501+00	2026-03-16 09:25:39.124501+00
8	Software Engineering	t	2026-03-16 09:25:39.124501+00	2026-03-16 09:25:39.124501+00
9	R&D avancee	t	2026-03-16 09:25:39.124501+00	2026-03-16 09:25:39.124501+00
\.


--
-- Data for Name: front_financier; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.front_financier (id, code, label, is_active, created_at, updated_at) FROM stdin;
1	SMA	Segula Maroc Automotive	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
2	SMAF	Segula Maroc Automotive Fournisseur	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
3	STS	Segula Tech Spain	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
4	SE	Segula Engineering	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
5	STG	Segula Technologies Germany	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
6	TEC	Technicon	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
7	SUS	Segula US	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
8	STI	Segula Tech Italy	t	2026-03-11 15:35:04.155934+00	2026-03-11 15:35:04.155934+00
\.


--
-- Data for Name: industry; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.industry (id, name, trigram, is_active, created_at, updated_at) FROM stdin;
1	Automobile	AUT	t	2026-03-11 15:35:04.138117+00	2026-03-11 15:35:04.138117+00
2	Ferroviaire	FER	t	2026-03-11 15:35:04.138117+00	2026-03-11 15:35:04.138117+00
3	Trucks	TRU	t	2026-03-11 15:35:04.138117+00	2026-03-11 15:35:04.138117+00
4	Administration & Secteur Public	ADM	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
5	Aeronautique	AER	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
6	Autres Secteurs Industriels	AS1	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
7	Autres Services, Sante, Transport, Commerce	AS2	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
8	Banque & Assurance	BAN	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
9	Bus & Poids-lourds	BUS	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
10	Defense & Securite	DEF	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
11	Energie, Environnement & Autres Utilites	ENE	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
12	Genie Civil & BTP	GEN	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
13	Helicoptere	HEL	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
14	Naval	NAV	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
15	Oil & Gaz, raffinage, petrochimie	OIL	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
16	Pharmacie, Chimie, Sante	PHA	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
17	Semiconducteur	SEM	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
18	Spatial	SPA	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
19	Technologies de l'information (IT, SSII, ...)	TEC	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
20	Telecommunication	TEL	t	2026-03-16 09:48:15.32509+00	2026-03-16 09:48:15.32509+00
\.


--
-- Data for Name: project; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.project (id, project_code, project_name, project_year, project_name_legacy, front_financier_id, activity, revenue_budget, cost_budget, margin_budget, project_margin, start_date, end_date, project_manager_id, bu_id, customer_id, industry_id, engineering_discipline_id, function_id, engagement_id, major_project, technical_office, status, deleted_at, created_by_id, created_at, updated_at, project_id) FROM stdin;
2		SMA - I50003 - SDV - OIL - INX - APV	2026		1	APV	836389.00	539232.00	297157.00	0.355286	2026-01-18	2026-12-18	13	I50003	19	15	3	1	1	f	Back Office	Planned	\N	13	2026-03-18 10:21:48.788336+00	2026-03-22 01:53:29.965771+00	\N
\.


--
-- Data for Name: project_audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.project_audit_log (id, project_id, changed_by, change_type, changed_at, old_values, new_values) FROM stdin;
\.


--
-- Data for Name: project_function; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.project_function (id, name, engineering_discipline_id, is_active, created_at, updated_at) FROM stdin;
1	Ingenieur conception mecanique	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
2	Ingenieur conception systemes	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
3	Ingenieur chassis / structure	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
4	Ingenieur Powertrain / propulsion / motorisation	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
5	Ingenieur calcul structurel (FEM / CFD)	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
6	Ingenieur supply chain / logistique	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
7	Ingenieur stamping / assemblage / bodyshop / paintshop	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
8	Ingenieur packaging / integration mecanique electronique	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
9	Ingenieur design produit interieur exterieur Classe A	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
10	Coordinateur validation / homologation	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
11	Ingenieur gestion de donnees / PLM	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
12	Ingenieur optimisation des couts / performance	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
13	Ingenieur gestion de la complexite	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
14	Ingenieur methodes / process	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
15	Ingenieur industrialisation	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
16	Ingenieur conception plasturgie / composites	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
17	Formateur technique	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
18	Data Engineering	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
19	Architecture systeme MBSE SysML Capella MagicDraw	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
20	Surete Safety RAMS ISO 26262 ARP4754A EN 50126 IEC 61508	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
21	Ingenierie des exigences DOORS Polarion Jama	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
22	Ingenieur configuration management	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
23	Support et assistance Technique	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
24	Coordinateur Technique EDS	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
25	Achat	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
26	Ingenieur gestion de projet / PMO	\N	t	2026-03-11 15:35:04.162891+00	2026-03-11 15:35:04.162891+00
\.


--
-- Data for Name: refresh_token; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_token (id, user_id, token_hash, issued_at, expires_at, revoked_at, device_info) FROM stdin;
\.


--
-- Name: app_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.app_user_id_seq', 24, true);


--
-- Name: customer_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customer_id_seq', 116, true);


--
-- Name: engagement_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.engagement_id_seq', 4, true);


--
-- Name: engineering_discipline_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.engineering_discipline_id_seq', 9, true);


--
-- Name: front_financier_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.front_financier_id_seq', 8, true);


--
-- Name: industry_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.industry_id_seq', 20, true);


--
-- Name: project_audit_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.project_audit_log_id_seq', 1, false);


--
-- Name: project_function_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.project_function_id_seq', 26, true);


--
-- Name: project_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.project_id_seq', 2, true);


--
-- Name: refresh_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refresh_token_id_seq', 1, false);


--
-- Name: app_user pk_app_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT pk_app_user PRIMARY KEY (id);


--
-- Name: bu pk_bu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bu
    ADD CONSTRAINT pk_bu PRIMARY KEY (id);


--
-- Name: customer pk_customer; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT pk_customer PRIMARY KEY (id);


--
-- Name: engagement pk_engagement; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engagement
    ADD CONSTRAINT pk_engagement PRIMARY KEY (id);


--
-- Name: engineering_discipline pk_engineering_discipline; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engineering_discipline
    ADD CONSTRAINT pk_engineering_discipline PRIMARY KEY (id);


--
-- Name: front_financier pk_front_financier; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.front_financier
    ADD CONSTRAINT pk_front_financier PRIMARY KEY (id);


--
-- Name: industry pk_industry; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.industry
    ADD CONSTRAINT pk_industry PRIMARY KEY (id);


--
-- Name: project pk_project; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT pk_project PRIMARY KEY (id);


--
-- Name: project_audit_log pk_project_audit_log; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_audit_log
    ADD CONSTRAINT pk_project_audit_log PRIMARY KEY (id);


--
-- Name: project_function pk_project_function; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_function
    ADD CONSTRAINT pk_project_function PRIMARY KEY (id);


--
-- Name: refresh_token pk_refresh_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT pk_refresh_token PRIMARY KEY (id);


--
-- Name: app_user uq_app_user_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT uq_app_user_email UNIQUE (email);


--
-- Name: customer uq_customer_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT uq_customer_name UNIQUE (name);


--
-- Name: customer uq_customer_trigram; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT uq_customer_trigram UNIQUE (trigram);


--
-- Name: engagement uq_engagement_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engagement
    ADD CONSTRAINT uq_engagement_name UNIQUE (name);


--
-- Name: engagement uq_engagement_type; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engagement
    ADD CONSTRAINT uq_engagement_type UNIQUE (engagement_type);


--
-- Name: engineering_discipline uq_engineering_discipline_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engineering_discipline
    ADD CONSTRAINT uq_engineering_discipline_name UNIQUE (name);


--
-- Name: front_financier uq_front_financier_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.front_financier
    ADD CONSTRAINT uq_front_financier_code UNIQUE (code);


--
-- Name: industry uq_industry_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.industry
    ADD CONSTRAINT uq_industry_name UNIQUE (name);


--
-- Name: industry uq_industry_trigram; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.industry
    ADD CONSTRAINT uq_industry_trigram UNIQUE (trigram);


--
-- Name: project uq_project_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT uq_project_code UNIQUE (project_code);


--
-- Name: project_function uq_project_function_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_function
    ADD CONSTRAINT uq_project_function_name UNIQUE (name);


--
-- Name: refresh_token uq_refresh_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash);


--
-- Name: idx_audit_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_changed_at ON public.project_audit_log USING btree (changed_at DESC);


--
-- Name: idx_audit_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_project_id ON public.project_audit_log USING btree (project_id);


--
-- Name: idx_project_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_active ON public.project USING btree (status) WHERE (deleted_at IS NULL);


--
-- Name: idx_project_bu_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_bu_id ON public.project USING btree (bu_id);


--
-- Name: idx_project_customer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_customer_id ON public.project USING btree (customer_id);


--
-- Name: idx_project_front_financier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_front_financier ON public.project USING btree (front_financier_id);


--
-- Name: idx_project_name; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_name ON public.project USING btree (project_name);


--
-- Name: idx_project_pm_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_pm_id ON public.project USING btree (project_manager_id);


--
-- Name: idx_project_start_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_start_date ON public.project USING btree (start_date);


--
-- Name: idx_project_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_status ON public.project USING btree (status);


--
-- Name: idx_project_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_project_year ON public.project USING btree (project_year);


--
-- Name: idx_refresh_token_expires; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_token_expires ON public.refresh_token USING btree (expires_at);


--
-- Name: idx_refresh_token_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_token_user_id ON public.refresh_token USING btree (user_id);


--
-- Name: app_user trg_app_user_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_app_user_updated_at BEFORE UPDATE ON public.app_user FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: bu trg_bu_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_bu_updated_at BEFORE UPDATE ON public.bu FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: customer trg_customer_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_customer_updated_at BEFORE UPDATE ON public.customer FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: engagement trg_engagement_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_engagement_updated_at BEFORE UPDATE ON public.engagement FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: engineering_discipline trg_engineering_discipline_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_engineering_discipline_updated_at BEFORE UPDATE ON public.engineering_discipline FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: front_financier trg_front_financier_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_front_financier_updated_at BEFORE UPDATE ON public.front_financier FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: industry trg_industry_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_industry_updated_at BEFORE UPDATE ON public.industry FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: project trg_project_calc_margin; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_project_calc_margin BEFORE INSERT OR UPDATE OF revenue_budget, cost_budget ON public.project FOR EACH ROW EXECUTE PROCEDURE public.fn_calc_project_margin();


--
-- Name: project_function trg_project_function_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_project_function_updated_at BEFORE UPDATE ON public.project_function FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: project trg_project_soft_delete_guard; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_project_soft_delete_guard BEFORE UPDATE ON public.project FOR EACH ROW EXECUTE PROCEDURE public.fn_guard_soft_deleted_project();


--
-- Name: project trg_project_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_project_updated_at BEFORE UPDATE ON public.project FOR EACH ROW EXECUTE PROCEDURE public.fn_set_updated_at();


--
-- Name: project_audit_log fk_audit_changed_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_audit_log
    ADD CONSTRAINT fk_audit_changed_by FOREIGN KEY (changed_by) REFERENCES public.app_user(id) ON DELETE SET NULL;


--
-- Name: project_audit_log fk_audit_project; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_audit_log
    ADD CONSTRAINT fk_audit_project FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE CASCADE;


--
-- Name: project_function fk_function_discipline; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_function
    ADD CONSTRAINT fk_function_discipline FOREIGN KEY (engineering_discipline_id) REFERENCES public.engineering_discipline(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: project fk_project_bu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_bu FOREIGN KEY (bu_id) REFERENCES public.bu(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_created_by; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_created_by FOREIGN KEY (created_by_id) REFERENCES public.app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_customer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_customer FOREIGN KEY (customer_id) REFERENCES public.customer(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_eng_discipline; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_eng_discipline FOREIGN KEY (engineering_discipline_id) REFERENCES public.engineering_discipline(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_engagement; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_engagement FOREIGN KEY (engagement_id) REFERENCES public.engagement(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_front_financier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_front_financier FOREIGN KEY (front_financier_id) REFERENCES public.front_financier(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_function; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_function FOREIGN KEY (function_id) REFERENCES public.project_function(id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: project fk_project_industry; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_industry FOREIGN KEY (industry_id) REFERENCES public.industry(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: project fk_project_pm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT fk_project_pm FOREIGN KEY (project_manager_id) REFERENCES public.app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: refresh_token fk_refresh_token_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES public.app_user(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

