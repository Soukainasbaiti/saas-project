-- ================================================================
-- PROJET DEMO MULTI-PAYS : Time & Material (T&M)
-- Front Office = France (chef de file) / Back Office = Maroc
-- Execute ce script dans pgAdmin sur ta base locale/railway
-- Pre-requis : avoir execute seed_countries.sql avant (pays France/Maroc)
-- ================================================================

DO $$
DECLARE
    v_project_id       BIGINT;
    v_ff_id            BIGINT;
    v_bu_id            TEXT;
    v_customer_id      BIGINT;
    v_industry_id      BIGINT;
    v_discipline_id    BIGINT;
    v_engagement_id    BIGINT;
    v_country_fr_id    BIGINT;
    v_country_ma_id    BIGINT;
    v_pm_front_id      BIGINT;
    v_pm_back_id       BIGINT;
    v_admin_id         BIGINT;
    v_res1_id          BIGINT; -- FR
    v_res2_id          BIGINT; -- FR
    v_res3_id          BIGINT; -- MA
    v_res4_id          BIGINT; -- MA
BEGIN

-- -- 0. RESOLUTION DES REFERENTIELS (par nom/code, pas par ID en dur) --
SELECT id   INTO v_ff_id         FROM front_financier        WHERE is_active = true ORDER BY id LIMIT 1;
SELECT id   INTO v_bu_id         FROM bu                     WHERE id = 'I52700' AND is_active = true LIMIT 1;
IF v_bu_id IS NULL THEN SELECT id INTO v_bu_id FROM bu WHERE is_active = true ORDER BY id LIMIT 1; END IF;
SELECT id   INTO v_customer_id   FROM customer                WHERE name = 'Stellantis' AND is_active = true LIMIT 1;
IF v_customer_id IS NULL THEN SELECT id INTO v_customer_id FROM customer WHERE is_active = true ORDER BY id LIMIT 1; END IF;
SELECT id   INTO v_industry_id   FROM industry                WHERE is_active = true ORDER BY id LIMIT 1;
SELECT id   INTO v_discipline_id FROM engineering_discipline   WHERE is_active = true ORDER BY id LIMIT 1;
SELECT id   INTO v_engagement_id FROM engagement               WHERE engagement_type = 'T&M' AND is_active = true LIMIT 1;

SELECT id INTO v_country_fr_id FROM country WHERE iso_code = 'FR' LIMIT 1;
SELECT id INTO v_country_ma_id FROM country WHERE iso_code = 'MA' LIMIT 1;
IF v_country_fr_id IS NULL OR v_country_ma_id IS NULL THEN
    RAISE EXCEPTION 'Pays FR/MA introuvables - execute seed_countries.sql avant ce script.';
END IF;

SELECT id INTO v_pm_front_id FROM app_user WHERE email = 'traore.adama@segulagrp.com' AND is_active = true LIMIT 1;
IF v_pm_front_id IS NULL THEN SELECT id INTO v_pm_front_id FROM app_user WHERE role IN ('PM','ADMIN') AND is_active = true ORDER BY id LIMIT 1; END IF;

SELECT id INTO v_pm_back_id FROM app_user WHERE email = 'soukaina.sbaiti@segulagrp.com' AND is_active = true LIMIT 1;
IF v_pm_back_id IS NULL THEN SELECT id INTO v_pm_back_id FROM app_user WHERE role = 'PM' AND is_active = true AND id <> v_pm_front_id ORDER BY id LIMIT 1; END IF;
IF v_pm_back_id IS NULL THEN v_pm_back_id := v_pm_front_id; END IF;

SELECT id INTO v_admin_id FROM app_user WHERE role = 'ADMIN' AND is_active = true ORDER BY id LIMIT 1;

-- -- 1. PROJET (Time & Material, multi-pays) --------------------
INSERT INTO project (
    project_code, project_id, project_name, project_year,
    front_financier_id, project_manager_id, bu_id, customer_id,
    industry_id, engineering_discipline_id, engagement_id,
    activity, revenue_budget, cost_budget,
    start_date, end_date, major_project, technical_office, status,
    created_by_id, created_at, updated_at
) VALUES (
    'TM-DEMO-2026',
    'SMAF-TM000001',
    'SMAF-TM000001 - Time & Material Multi-Pays FR-MA',
    2026,
    v_ff_id, v_pm_front_id, v_bu_id, v_customer_id,
    v_industry_id, v_discipline_id, v_engagement_id,
    'Assistance technique Time & Material reparti entre la France et le Maroc',
    720000.00, 480000.00,
    '2026-01-01', '2026-12-31',
    true, 'Front Office', 'On Going',
    v_pm_front_id, NOW(), NOW()
) RETURNING id INTO v_project_id;

RAISE NOTICE 'Projet insere avec ID = %', v_project_id;

-- -- 2. PAYS DU PROJET (France = chef de file, Maroc = back office) --
INSERT INTO project_country (project_id, country_id, pm_id, is_lead, display_order, added_by, created_at)
VALUES
    (v_project_id, v_country_fr_id, v_pm_front_id, true,  1, v_pm_front_id, NOW()),
    (v_project_id, v_country_ma_id, v_pm_back_id,  false, 2, v_pm_front_id, NOW());

-- Suffixe pays dans le nom (comme le ferait l'application automatiquement)
UPDATE project SET project_name = project_name || '-FR-MA' WHERE id = v_project_id;

-- -- 3. PROJECT MANAGEMENT CONFIG (One Pager) ------------------
INSERT INTO project_management_config (
    project_id, granularity, currency, validation_status,
    delivery_confidence_level, health_score_value, health_score_status,
    pm_remarks, tops, flops,
    variance_actual_comment, variance_trend_comment, variance_landing_comment
) VALUES (
    v_project_id, 'MONTHLY', 'EUR', 'VALIDATED',
    'MINOR_RISKS', 82, 'MINOR_RISKS',
    'Projet T&M multi-pays en bonne sante. Equipe France pilote le delivery, equipe Maroc assure le run et le support. Bonne synchronisation entre les deux PM.',
    'Bonne collaboration FR/MA sur le partage de charge
Marge stable sur les deux perimetres pays
Montee en competence rapide de l equipe Maroc',
    'Decalage horaire ponctuellement source de retard sur les points de synchro
Turnover leger sur l equipe Maroc a surveiller',
    'Couts alignes avec le budget sur les deux pays, ecart de moins de 3%.',
    'Tendance stable sur les 6 derniers mois, aucune derive notable.',
    'Landing prevu a 240K EUR de marge consolidee FR+MA, soit 33% de PM.'
);

-- -- 4. PREVISIONS MENSUELLES 2026 (consolidees projet) --------
INSERT INTO project_monthly_forecast (project_id, month, revenue, cost, cov) VALUES
    (v_project_id, '2026-01', 55000.00, 37000.00, 58000.00),
    (v_project_id, '2026-02', 58000.00, 38500.00, 60000.00),
    (v_project_id, '2026-03', 62000.00, 41000.00, 64000.00),
    (v_project_id, '2026-04', 60000.00, 40000.00, 62000.00),
    (v_project_id, '2026-05', 61000.00, 40500.00, 63000.00),
    (v_project_id, '2026-06', 65000.00, 43000.00, 67000.00),
    (v_project_id, '2026-07', 58000.00, 38500.00, 60000.00),
    (v_project_id, '2026-08', 50000.00, 33000.00, 52000.00),
    (v_project_id, '2026-09', 62000.00, 41000.00, 64000.00),
    (v_project_id, '2026-10', 64000.00, 42500.00, 66000.00),
    (v_project_id, '2026-11', 62000.00, 41000.00, 64000.00),
    (v_project_id, '2026-12', 63000.00, 42000.00, 82000.00);

-- -- 5. CONSULTANTS (2 France + 2 Maroc) ------------------------
INSERT INTO project_resource (project_id, matricule, person_name, country_id, is_active)
VALUES
    (v_project_id, 'TM-FR-001', 'Camille Dupont',  v_country_fr_id, true),
    (v_project_id, 'TM-FR-002', 'Julien Marchand',  v_country_fr_id, true),
    (v_project_id, 'TM-MA-001', 'Omar Lemdeghri',   v_country_ma_id, true),
    (v_project_id, 'TM-MA-002', 'Sara El Alaoui',   v_country_ma_id, true);

SELECT id INTO v_res1_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'TM-FR-001';
SELECT id INTO v_res2_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'TM-FR-002';
SELECT id INTO v_res3_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'TM-MA-001';
SELECT id INTO v_res4_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'TM-MA-002';

-- Camille Dupont (France, senior, 700 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res1_id, '2026-01', 700.00, 20.00, 19.00, 820.00),
    (v_res1_id, '2026-02', 700.00, 18.00, 18.00, 820.00),
    (v_res1_id, '2026-03', 700.00, 22.00, 21.00, 820.00),
    (v_res1_id, '2026-04', 700.00, 21.00, 20.00, 820.00),
    (v_res1_id, '2026-05', 700.00, 20.00, 20.00, 820.00),
    (v_res1_id, '2026-06', 700.00, 22.00, 22.00, 820.00),
    (v_res1_id, '2026-07', 700.00, 17.00, 17.00, 820.00),
    (v_res1_id, '2026-08', 700.00, 12.00, 12.00, 820.00),
    (v_res1_id, '2026-09', 700.00, 21.00, 21.00, 820.00),
    (v_res1_id, '2026-10', 700.00, 22.00, 22.00, 820.00),
    (v_res1_id, '2026-11', 700.00, 20.00, 20.00, 820.00),
    (v_res1_id, '2026-12', 700.00, 19.00, 18.00, 820.00);

-- Julien Marchand (France, 520 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res2_id, '2026-01', 520.00, 20.00, 20.00, 600.00),
    (v_res2_id, '2026-02', 520.00, 18.00, 17.00, 600.00),
    (v_res2_id, '2026-03', 520.00, 22.00, 22.00, 600.00),
    (v_res2_id, '2026-04', 520.00, 21.00, 21.00, 600.00),
    (v_res2_id, '2026-05', 520.00, 20.00, 19.00, 600.00),
    (v_res2_id, '2026-06', 520.00, 22.00, 22.00, 600.00),
    (v_res2_id, '2026-07', 520.00, 17.00, 16.00, 600.00),
    (v_res2_id, '2026-08', 520.00, 10.00, 10.00, 600.00),
    (v_res2_id, '2026-09', 520.00, 21.00, 21.00, 600.00),
    (v_res2_id, '2026-10', 520.00, 22.00, 22.00, 600.00),
    (v_res2_id, '2026-11', 520.00, 20.00, 20.00, 600.00),
    (v_res2_id, '2026-12', 520.00, 19.00, 19.00, 600.00);

-- Omar Lemdeghri (Maroc, 380 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res3_id, '2026-01', 380.00, 20.00, 18.00, 430.00),
    (v_res3_id, '2026-02', 380.00, 18.00, 18.00, 430.00),
    (v_res3_id, '2026-03', 380.00, 22.00, 20.00, 430.00),
    (v_res3_id, '2026-04', 380.00, 21.00, 21.00, 430.00),
    (v_res3_id, '2026-05', 380.00, 20.00, 20.00, 430.00),
    (v_res3_id, '2026-06', 380.00, 22.00, 21.00, 430.00),
    (v_res3_id, '2026-07', 380.00, 17.00, 15.00, 430.00),
    (v_res3_id, '2026-08', 380.00, 12.00, 12.00, 430.00),
    (v_res3_id, '2026-09', 380.00, 21.00, 20.00, 430.00),
    (v_res3_id, '2026-10', 380.00, 22.00, 22.00, 430.00),
    (v_res3_id, '2026-11', 380.00, 20.00, 19.00, 430.00),
    (v_res3_id, '2026-12', 380.00, 18.00, 18.00, 430.00);

-- Sara El Alaoui (Maroc, 340 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res4_id, '2026-01', 340.00, 15.00, 15.00, 390.00),
    (v_res4_id, '2026-02', 340.00, 12.00, 12.00, 390.00),
    (v_res4_id, '2026-03', 340.00, 18.00, 18.00, 390.00),
    (v_res4_id, '2026-04', 340.00, 16.00, 16.00, 390.00),
    (v_res4_id, '2026-05', 340.00, 14.00, 14.00, 390.00),
    (v_res4_id, '2026-06', 340.00, 18.00, 17.00, 390.00),
    (v_res4_id, '2026-07', 340.00,  8.00,  8.00, 390.00),
    (v_res4_id, '2026-08', 340.00,  5.00,  5.00, 390.00),
    (v_res4_id, '2026-09', 340.00, 14.00, 14.00, 390.00),
    (v_res4_id, '2026-10', 340.00, 16.00, 16.00, 390.00),
    (v_res4_id, '2026-11', 340.00, 14.00, 14.00, 390.00),
    (v_res4_id, '2026-12', 340.00, 12.00, 12.00, 390.00);

-- -- 6. AUTRES COUTS --------------------------------------------
INSERT INTO project_other_cost (project_id, category, month, amount, is_rebill)
SELECT v_project_id, cat, mois, amt, false
FROM (VALUES
    ('Frais de deplacement FR', '2026-01', 900.00),
    ('Frais de deplacement FR', '2026-04', 1100.00),
    ('Frais de deplacement FR', '2026-07', 800.00),
    ('Frais de deplacement FR', '2026-10', 950.00),
    ('Licences & Outillage',    '2026-01', 2200.00),
    ('Licences & Outillage',    '2026-06', 2200.00),
    ('Licences & Outillage',    '2026-12', 2200.00)
) AS t(cat, mois, amt);

INSERT INTO project_other_cost (project_id, category, month, amount, is_rebill)
SELECT v_project_id, cat, mois, amt, true
FROM (VALUES
    ('Frais de deplacement rebilles', '2026-03', 700.00),
    ('Frais de deplacement rebilles', '2026-09', 650.00)
) AS t(cat, mois, amt);

-- -- 7. RISKS --------------------------------------------------
INSERT INTO project_risk (
    project_id, r_id, identification_date, phase,
    risk, category, probability, prob_eval, percent_probability,
    impact, impact_eval, rating, management_strategy,
    owner, mitigation_action, costs, probability_residual, net,
    deadline, status
) VALUES
(
    v_project_id, 'R_SMAF-TM000001_2026_0001', '2026-01-20', 'Run',
    'Si le decalage horaire FR/MA cree des retards de synchronisation, alors les livraisons hebdomadaires seront impactees.',
    'Organisation', 'Possible', 3, 50.00,
    'Moderate', 3, 'Medium', 'Mitigate',
    'Camille Dupont',
    'Mise en place de creneaux de synchro fixes matin FR / apres-midi MA, documentation partagee.',
    5000.00, 20.00, 1000.00,
    '2026-04-30', 'Mitigation in progress'
),
(
    v_project_id, 'R_SMAF-TM000001_2026_0002', '2026-02-10', 'Run',
    'Si le turnover sur l equipe Maroc s accelere, alors la continuite de service sera degradee.',
    'Ressources Humaines', 'Possible', 3, 45.00,
    'Significant', 4, 'High', 'Mitigate',
    'Omar Lemdeghri',
    'Plan de retention et binomage systematique sur chaque poste critique.',
    8000.00, 25.00, 2000.00,
    '2026-07-31', 'Identified'
);

-- -- 8. ISSUES ------------------------------------------------
INSERT INTO project_issue (
    project_id, i_id, issue, severity, priority, impacts,
    dte, dta, lockdown, investigation, sustainable_resolution,
    exit_criteria, owner, deadline, status, remarks
) VALUES
(
    v_project_id, 'I_SMAF-TM000001_2026_0001',
    'FAIT: Retard de 3 jours sur la livraison mensuelle du cote Maroc. IMPACT: Decalage du reporting consolide FR/MA.',
    'Medium', 'P2-Medium', 'High Impact',
    '2026-03-05', '2026-03-06',
    'Priorisation des livrables critiques cote Maroc.',
    'Analyse : surcharge ponctuelle liee a un conge non anticipe.',
    'Renfort temporaire sur la periode + planning de conges partage entre PM FR/MA.',
    'Livraison mensuelle a jour et validee.',
    'Soukaina Sbaiti', '2026-04-15', 'Open',
    'PM France informe, plan d action valide en synchro hebdomadaire.'
);

-- -- 9. OPPORTUNITIES ------------------------------------------
INSERT INTO project_opportunity (
    project_id, o_id, identification_date,
    opportunity_description, category, costs, price, estimated_benefit,
    percent_new_pm, action_to_be_taken, owner, deadline,
    overdue, on_hold, status, copil_validation, comments
) VALUES
(
    v_project_id, 'O_SMAF-TM000001_2026_0001', '2026-03-01',
    'SI le client valide l extension du perimetre T&M avec 2 ressources Maroc supplementaires, ALORS nous generons 60K EUR de revenue additionnel.',
    'Extension de perimetre',
    22000.00, 60000.00, 38000.00,
    63,
    'Preparer chiffrage et proposition commerciale pour le prochain comite de pilotage.',
    'Adama Traore', '2026-06-30',
    false, false, 'In Progress', 'Pending',
    'Client interesse par le modele de delivery FR/MA, discussion en cours.'
);

-- -- 10. MIP (Margin Improvement Plan) ------------------------
INSERT INTO project_mip (
    project_id, mip_id, identification_date,
    lever, action_description, accountable, client_impact,
    due_date, priority, risques_prerequis, planned_gain, realized_gain, status
) VALUES
(
    v_project_id, 'MIP_SMAF-TM000001_2026_0001', '2026-02-01',
    'Costs',
    'Basculer une partie des taches de support de niveau 1 vers l equipe Maroc pour reduire le cout journalier moyen du delivery.',
    'Soukaina Sbaiti', 'No',
    '2026-05-31', 'P1-High',
    'Formation prealable de l equipe Maroc sur les outils France.',
    9000.00, 4500.00, 'In Progress'
);

-- -- 11. WIP --------------------------------------------------
INSERT INTO project_wip (
    project_id, wip_id, bl_number, period, business_unit,
    bl_days, bl_amount, billed_days, wip_days, wip_amount,
    imported_at, status, notes
) VALUES
(
    v_project_id, 'WIP_SMAF-TM000001_2026_01', 'BL-2026-TM-FR-001', '2026-01', v_bu_id,
    77.00, 55000.00, 74.00, 3.00, 2142.86,
    NOW(), 'Open', 'Janvier 2026 - consolidation FR+MA'
),
(
    v_project_id, 'WIP_SMAF-TM000001_2026_02', 'BL-2026-TM-FR-002', '2026-02', v_bu_id,
    69.00, 58000.00, 65.00, 4.00, 3362.32,
    NOW(), 'Open', 'Fevrier 2026 - retard partiel cote Maroc'
);

RAISE NOTICE '=== PROJET T&M MULTI-PAYS CREE AVEC SUCCES ===';
RAISE NOTICE 'Project ID : %', v_project_id;
RAISE NOTICE 'Project code : TM-DEMO-2026 / SMAF-TM000001-FR-MA';
RAISE NOTICE 'Pays : France (chef de file, PM id=%) + Maroc (PM id=%)', v_pm_front_id, v_pm_back_id;
RAISE NOTICE '4 consultants (2 FR + 2 MA), 2 risks, 1 issue, 1 opportunity, 1 MIP, 2 WIP.';

END $$;
