-- Migration : ajout de la gestion multi-pays sur les projets
-- A executer manuellement sur la base existante (Hibernate ddl-auto=update ne gere pas les DROP COLUMN / NOT NULL sur colonnes existantes).

-- 1. Referentiel Pays
CREATE TABLE IF NOT EXISTS public.country (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name character varying(100) NOT NULL UNIQUE,
    iso_code character varying(2) NOT NULL UNIQUE,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

COMMENT ON TABLE public.country IS 'Referentiel des pays pouvant etre rattaches a un projet.';

-- 2. Association Projet <-> Pays (porte le PM, le flag pays chef de file, l'ordre d'affichage)
CREATE TABLE IF NOT EXISTS public.project_country (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id bigint NOT NULL REFERENCES public.project(id) ON DELETE CASCADE,
    country_id bigint NOT NULL REFERENCES public.country(id) ON DELETE RESTRICT,
    pm_id bigint REFERENCES public.app_user(id),
    is_lead boolean NOT NULL DEFAULT false,
    display_order integer NOT NULL DEFAULT 1,
    added_by bigint REFERENCES public.app_user(id),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    UNIQUE (project_id, country_id)
);

-- Un seul pays "chef de file" (Front Office) par projet
CREATE UNIQUE INDEX IF NOT EXISTS uq_project_lead_country
    ON public.project_country (project_id) WHERE is_lead = true;

COMMENT ON TABLE public.project_country IS 'Rattachement des pays a un projet, avec leur PM dedie et leur ordre (pays chef de file = is_lead).';

-- 3. project_resource : on remplace contract_type par country_id (Option B validee)
ALTER TABLE public.project_resource ADD COLUMN IF NOT EXISTS country_id bigint REFERENCES public.country(id);

-- ATTENTION - etape manuelle avant de forcer la contrainte NOT NULL :
-- backfiller country_id sur les lignes existantes de project_resource
-- (ex: a partir du pays "chef de file" du projet auquel la ligne appartient),
-- puis executer :
-- ALTER TABLE public.project_resource ALTER COLUMN country_id SET NOT NULL;
-- ALTER TABLE public.project_resource DROP COLUMN contract_type;
