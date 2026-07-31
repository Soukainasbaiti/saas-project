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
-- Compte admin de test, pour se connecter en local uniquement.
-- Identifiants : admin@segula.fr / Admin123!
-- Ne JAMAIS utiliser ce hash/mot de passe en production.
--

INSERT INTO public.app_user (full_name, email, password_hash, role, is_active)
VALUES ('Admin Local', 'admin@segula.fr', '$2a$10$u1UKxce1JQEI40xO4TP.E.6ZIQaQOg6MAc7CWFhl9Rr0L4tovEcuC', 'ADMIN', true);


--
-- PostgreSQL database dump complete
--

