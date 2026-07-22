-- ================================================================
-- Remplissage complet pour video demo PM
-- Projet : SMAF - I50001 - MEC - BUS - BMW - System Integrati (id=53)
-- PM : Fadoua HABABOU / Client : BMW / Engagement : T&M
-- ================================================================

DO $$
DECLARE
    v_project_id       BIGINT := 53;
    v_country_fr_id    BIGINT;
    v_pm_id            BIGINT := 7; -- Fadoua HABABOU
    v_res1_id          BIGINT;
    v_res2_id          BIGINT;
    v_res3_id          BIGINT;
BEGIN

SELECT id INTO v_country_fr_id FROM country WHERE iso_code = 'FR' LIMIT 1;

-- -- 1. Pays du projet (France, chef de file) ------------------
INSERT INTO project_country (project_id, country_id, pm_id, is_lead, display_order, added_by, created_at)
SELECT v_project_id, v_country_fr_id, v_pm_id, true, 1, v_pm_id, NOW()
WHERE NOT EXISTS (SELECT 1 FROM project_country WHERE project_id = v_project_id);

-- -- 2. One Pager (Executive Summary) --------------------------
UPDATE project_management_config SET
    validation_status = 'DRAFT',
    delivery_confidence_level = 'MINOR_RISKS',
    health_score_value = 81,
    health_score_status = 'MINOR_RISKS',
    pm_remarks = 'Projet System Integration BMW en bonne progression. Livraisons conformes au planning. Equipe stable, bonne relation client. Vigilance sur la montee en charge prevue T3.',
    tops = 'Client tres satisfait des premieres livraisons
Equipe technique stable et experimentee
Marge en ligne avec le budget initial',
    flops = 'Legere tension sur la disponibilite des bancs de test
Delai fournisseur composants electroniques a surveiller',
    variance_actual_comment = 'Couts reels alignes avec le budget, ecart de moins de 4% sur les 3 premiers mois.',
    variance_trend_comment = 'Tendance stable, aucune derive notable sur les couts directs.',
    variance_landing_comment = 'Landing prevu a 91.4K EUR de marge, soit 72% de PM - tres au-dessus de la cible BU.'
WHERE project_id = v_project_id;

-- -- 3. Previsions mensuelles 2026 ------------------------------
INSERT INTO project_monthly_forecast (project_id, month, revenue, cost, cov)
SELECT v_project_id, mois, rev, cost, cov FROM (VALUES
    ('2026-01', 4444.00,  1111.00, 4356.00),
    ('2026-02', 55373.00, 5366.00, 7887.00),
    ('2026-03', 3234.00,  1234.00, 776.00),
    ('2026-04', 6728.00,  4524.00, 342.00),
    ('2026-05', 5615.00,  2897.00, 2567.00),
    ('2026-06', 7889.00,  2315.00, 565.00),
    ('2026-07', 6777.00,  3333.00, 156.00),
    ('2026-08', 4545.00,  1212.00, 675.00),
    ('2026-09', 9898.00,  3231.00, 6565.00),
    ('2026-10', 3333.00,  1211.00, 6767.00),
    ('2026-11', 9890.00,  5654.00, 343.00),
    ('2026-12', 8989.00,  3232.00, 777.00)
) AS t(mois, rev, cost, cov)
WHERE NOT EXISTS (
    SELECT 1 FROM project_monthly_forecast WHERE project_id = v_project_id AND month = t.mois
);

-- -- 4. Consultants (France) ------------------------------------
INSERT INTO project_resource (project_id, matricule, person_name, country_id, is_active)
VALUES
    (v_project_id, 'BMW-001', 'Nicolas Perrin',    v_country_fr_id, true),
    (v_project_id, 'BMW-002', 'Claire Fontaine',   v_country_fr_id, true),
    (v_project_id, 'BMW-003', 'Thomas Girard',     v_country_fr_id, true);

SELECT id INTO v_res1_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'BMW-001';
SELECT id INTO v_res2_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'BMW-002';
SELECT id INTO v_res3_id FROM project_resource WHERE project_id = v_project_id AND matricule = 'BMW-003';

-- Nicolas Perrin (Senior, 680 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res1_id, '2026-01', 680.00, 20.00, 19.00, 780.00),
    (v_res1_id, '2026-02', 680.00, 18.00, 18.00, 780.00),
    (v_res1_id, '2026-03', 680.00, 22.00, 21.00, 780.00),
    (v_res1_id, '2026-04', 680.00, 21.00, 20.00, 780.00),
    (v_res1_id, '2026-05', 680.00, 20.00, 20.00, 780.00),
    (v_res1_id, '2026-06', 680.00, 22.00, 22.00, 780.00),
    (v_res1_id, '2026-07', 680.00, 17.00, 17.00, 780.00),
    (v_res1_id, '2026-08', 680.00, 12.00, 12.00, 780.00),
    (v_res1_id, '2026-09', 680.00, 21.00, 21.00, 780.00),
    (v_res1_id, '2026-10', 680.00, 22.00, 22.00, 780.00),
    (v_res1_id, '2026-11', 680.00, 20.00, 20.00, 780.00),
    (v_res1_id, '2026-12', 680.00, 19.00, 18.00, 780.00);

-- Claire Fontaine (500 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res2_id, '2026-01', 500.00, 20.00, 20.00, 580.00),
    (v_res2_id, '2026-02', 500.00, 18.00, 17.00, 580.00),
    (v_res2_id, '2026-03', 500.00, 22.00, 22.00, 580.00),
    (v_res2_id, '2026-04', 500.00, 21.00, 21.00, 580.00),
    (v_res2_id, '2026-05', 500.00, 20.00, 19.00, 580.00),
    (v_res2_id, '2026-06', 500.00, 22.00, 22.00, 580.00),
    (v_res2_id, '2026-07', 500.00, 17.00, 16.00, 580.00),
    (v_res2_id, '2026-08', 500.00, 10.00, 10.00, 580.00),
    (v_res2_id, '2026-09', 500.00, 21.00, 21.00, 580.00),
    (v_res2_id, '2026-10', 500.00, 22.00, 22.00, 580.00),
    (v_res2_id, '2026-11', 500.00, 20.00, 20.00, 580.00),
    (v_res2_id, '2026-12', 500.00, 19.00, 19.00, 580.00);

-- Thomas Girard (420 EUR/j)
INSERT INTO project_resource_entry (resource_id, month, daily_cost, worked_days, billed_days, daily_rate) VALUES
    (v_res3_id, '2026-01', 420.00, 15.00, 15.00, 480.00),
    (v_res3_id, '2026-02', 420.00, 12.00, 12.00, 480.00),
    (v_res3_id, '2026-03', 420.00, 18.00, 18.00, 480.00),
    (v_res3_id, '2026-04', 420.00, 16.00, 16.00, 480.00),
    (v_res3_id, '2026-05', 420.00, 14.00, 14.00, 480.00),
    (v_res3_id, '2026-06', 420.00, 18.00, 17.00, 480.00),
    (v_res3_id, '2026-07', 420.00,  8.00,  8.00, 480.00),
    (v_res3_id, '2026-08', 420.00,  5.00,  5.00, 480.00),
    (v_res3_id, '2026-09', 420.00, 14.00, 14.00, 480.00),
    (v_res3_id, '2026-10', 420.00, 16.00, 16.00, 480.00),
    (v_res3_id, '2026-11', 420.00, 14.00, 14.00, 480.00),
    (v_res3_id, '2026-12', 420.00, 12.00, 12.00, 480.00);

-- -- 5. Autres couts (Other Costs + Rebilled) ------------------
INSERT INTO project_other_cost (project_id, category, month, amount, is_rebill)
SELECT v_project_id, cat, mois, amt, false
FROM (VALUES
    ('Frais de deplacement', '2026-01', 850.00),
    ('Frais de deplacement', '2026-04', 1050.00),
    ('Frais de deplacement', '2026-07', 700.00),
    ('Frais de deplacement', '2026-10', 900.00),
    ('Licences & Outillage',  '2026-01', 1800.00),
    ('Licences & Outillage',  '2026-06', 1800.00),
    ('Licences & Outillage',  '2026-12', 1800.00),
    ('Formation',             '2026-03', 1200.00),
    ('Sous-traitance',        '2026-05', 4500.00)
) AS t(cat, mois, amt);

INSERT INTO project_other_cost (project_id, category, month, amount, is_rebill)
SELECT v_project_id, cat, mois, amt, true
FROM (VALUES
    ('Frais de deplacement rebilles', '2026-02', 600.00),
    ('Equipements rebilles',          '2026-08', 2200.00)
) AS t(cat, mois, amt);

-- -- 6. Risks --------------------------------------------------
INSERT INTO project_risk (
    project_id, r_id, identification_date, phase,
    risk, category, probability, prob_eval, percent_probability,
    impact, impact_eval, rating, management_strategy,
    owner, mitigation_action, costs, probability_residual, net,
    deadline, status
) VALUES
(
    v_project_id, 'R_SMAF-I50001_2026_0001', '2026-01-15', 'Integration',
    'Si les bancs de test partages sont indisponibles au moment critique, alors les jalons de validation seront decales de 2 a 3 semaines.',
    'Ressources materielles', 'Possible', 3, 50.00,
    'Significant', 4, 'High', 'Mitigate',
    'Nicolas Perrin',
    'Reservation anticipee des creneaux bancs de test + solution de secours identifiee chez un sous-traitant.',
    12000.00, 20.00, 2400.00,
    '2026-05-31', 'Mitigation in progress'
),
(
    v_project_id, 'R_SMAF-I50001_2026_0002', '2026-02-10', 'Approvisionnement',
    'Si le fournisseur de composants electroniques cle prend du retard, alors le planning d integration global sera impacte.',
    'Supply Chain', 'Likely', 4, 65.00,
    'Moderate', 3, 'Medium', 'Mitigate',
    'Claire Fontaine',
    'Double sourcing engage, stock de securite constitue sur les references critiques.',
    8000.00, 25.00, 2000.00,
    '2026-06-30', 'Identified'
),
(
    v_project_id, 'R_SMAF-I50001_2026_0003', '2026-03-05', 'Validation',
    'Si les exigences client evoluent en cours de validation, alors le perimetre de tests sera revu a la hausse.',
    'Scope', 'Unlikely', 2, 20.00,
    'Minor', 2, 'Low', 'Accept',
    'Fadoua HABABOU',
    'Gel du perimetre valide en comite de pilotage, clause de gestion des evolutions activee.',
    3000.00, 10.00, 300.00,
    '2026-04-30', 'Closed'
);

-- -- 7. Issues ------------------------------------------------
INSERT INTO project_issue (
    project_id, i_id, issue, severity, priority, impacts,
    dte, dta, lockdown, investigation, sustainable_resolution,
    exit_criteria, owner, deadline, status, remarks
) VALUES
(
    v_project_id, 'I_SMAF-I50001_2026_0001',
    'FAIT: Retard de livraison de 5 jours sur le module capteurs par le sous-traitant. IMPACT: Decalage de la phase d integration. CONSEQUENCE: Risque de glissement du jalon Q2 si non rattrape.',
    'Medium', 'P2-Medium', 'High Impact',
    '2026-02-20', '2026-02-22',
    'Priorisation des taches d integration non dependantes du module capteurs.',
    'Analyse : sous-effectif temporaire chez le sous-traitant.',
    'Renfort ressource sous-traitant valide, nouveau planning accepte par le client.',
    'Module capteurs livre et valide sans reserve.',
    'Nicolas Perrin', '2026-04-10', 'Open',
    'Client informe, plan de rattrapage valide en reunion hebdomadaire.'
),
(
    v_project_id, 'I_SMAF-I50001_2026_0002',
    'FAIT: Depassement de 10% des jours factures sur la phase de conception. IMPACT: Pression sur la marge du trimestre. CONSEQUENCE: Vigilance necessaire sur les phases suivantes.',
    'Low', 'P3-Low', 'Moderate Impact',
    '2026-03-15', '2026-03-16',
    'Aucun blocage, suivi renforce.',
    'Revue des feuilles de temps, ajustement estimation initiale sous-evaluee.',
    'Mise a jour du budget previsionnel sur les phases restantes.',
    'Ecart ramene sous 5% sur le trimestre suivant.',
    'Claire Fontaine', '2026-06-30', 'Open',
    'Point budgetaire fait avec le BUM, pas d impact sur la marge globale.'
);

-- -- 8. Opportunities --------------------------------------------
INSERT INTO project_opportunity (
    project_id, o_id, identification_date,
    opportunity_description, category, costs, price, estimated_benefit,
    percent_new_pm, action_to_be_taken, owner, deadline,
    overdue, on_hold, status, copil_validation, comments
) VALUES
(
    v_project_id, 'O_SMAF-I50001_2026_0001', '2026-03-01',
    'SI le client valide l extension du perimetre aux tests d homologation complementaires, ALORS nous generons 45K EUR de revenue additionnel avec une marge de 62%.',
    'Extension de perimetre',
    17000.00, 45000.00, 28000.00,
    62,
    'Preparer chiffrage detaille pour le prochain comite de pilotage client.',
    'Fadoua HABABOU', '2026-07-31',
    false, false, 'In Progress', 'Pending',
    'Client interesse, premier echange positif lors du dernier point mensuel.'
),
(
    v_project_id, 'O_SMAF-I50001_2026_0002', '2026-04-10',
    'SI nous automatisons les scripts de validation repetitifs, ALORS nous reduisons les couts de test de 15K EUR sur la duree du projet.',
    'Optimisation productive',
    5000.00, 20000.00, 15000.00,
    75,
    'POC outillage a lancer en Q3, validation par le lead technique.',
    'Thomas Girard', '2026-09-30',
    false, false, 'Open', 'Pending',
    'Etude de faisabilite en cours.'
);

-- -- 9. MIP ----------------------------------------------------
INSERT INTO project_mip (
    project_id, mip_id, identification_date,
    lever, action_description, accountable, client_impact,
    due_date, priority, risques_prerequis, planned_gain, realized_gain, status
) VALUES
(
    v_project_id, 'MIP_SMAF-I50001_2026_0001', '2026-02-01',
    'Productivity',
    'Mettre en place un tableau de bord partage pour suivre l avancement des tests en temps reel, reduisant les reunions de synchro de 3h a 1h par semaine.',
    'Nicolas Perrin', 'No',
    '2026-05-31', 'P1-High',
    'Validation outil par le DSI.',
    9000.00, 5000.00, 'In Progress'
),
(
    v_project_id, 'MIP_SMAF-I50001_2026_0002', '2026-03-10',
    'Costs',
    'Renegociation du contrat de location des bancs de test pour obtenir un tarif degressif sur les 6 prochains mois.',
    'Claire Fontaine', 'No',
    '2026-06-30', 'P2-Medium',
    'Accord de la direction achats requis.',
    6000.00, 6000.00, 'Completed'
);

-- -- 10. WIP --------------------------------------------------
INSERT INTO project_wip (
    project_id, wip_id, bl_number, period, business_unit,
    bl_days, bl_amount, billed_days, wip_days, wip_amount,
    imported_at, status, notes
) VALUES
(
    v_project_id, 'WIP_SMAF-I50001_2026_01', 'BL-2026-BMW-001', '2026-01', 'I50001',
    57.00, 4444.00, 55.00, 2.00, 156.00,
    NOW(), 'Open', 'Janvier 2026 - livraison partielle'
),
(
    v_project_id, 'WIP_SMAF-I50001_2026_02', 'BL-2026-BMW-002', '2026-02', 'I50001',
    52.00, 55373.00, 50.00, 2.00, 2129.73,
    NOW(), 'Open', 'Fevrier 2026 - forecast eleve, BL en attente signature'
),
(
    v_project_id, 'WIP_SMAF-I50001_2026_03', 'BL-2026-BMW-003', '2026-03', 'I50001',
    60.00, 3234.00, 57.00, 3.00, 161.70,
    NOW(), 'Open', 'Mars 2026 - solde en cours de facturation'
);

RAISE NOTICE '=== PROJET I50001 (BMW System Integration) REMPLI POUR VIDEO DEMO ===';
RAISE NOTICE 'Project ID : %', v_project_id;
RAISE NOTICE 'PM : Fadoua HABABOU / Pays : France (chef de file)';
RAISE NOTICE '3 consultants, 3 risks, 2 issues, 2 opportunities, 2 MIP, 3 WIP.';

END $$;
