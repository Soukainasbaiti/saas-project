import { Injectable, signal } from '@angular/core';

export type Lang = 'fr' | 'en';

// Rule: FR = messages/UI in French, technical keywords stay English
//       EN = everything in English
const T: Record<string, Record<Lang, string>> = {

  // ── Common ──────────────────────────────────────────────────────
  'loading':            { fr: 'Chargement...', en: 'Loading...' },
  'btn.cancel':         { fr: 'Annuler',       en: 'Cancel' },
  'btn.close':          { fr: 'Fermer',        en: 'Close' },
  'btn.delete':         { fr: 'Supprimer',     en: 'Delete' },
  'btn.add':            { fr: 'Ajouter',       en: 'Add' },
  'btn.back':           { fr: 'Retour',        en: 'Back' },
  'btn.logout':         { fr: 'Déconnexion',   en: 'Log Out' },
  'yes':                { fr: 'Oui',           en: 'Yes' },
  'no':                 { fr: 'Non',           en: 'No' },
  'irreversible':       { fr: 'Cette action est irréversible.', en: 'This action is irreversible.' },

  // ── Topbar / Notifications ───────────────────────────────────────
  'notif.bum.title':    { fr: 'Projets à valider',       en: 'Projects to validate' },
  'notif.bum.empty':    { fr: 'Aucun projet en attente.', en: 'No pending projects.' },
  'notif.bum.pending':  { fr: 'En attente',              en: 'Pending' },
  'notif.pm.title':     { fr: 'Projets rejetés',         en: 'Rejected projects' },
  'notif.pm.empty':     { fr: 'Aucun projet rejeté.',    en: 'No rejected projects.' },
  'notif.pm.rejectedby':{ fr: 'Rejeté par',             en: 'Rejected by' },

  // ── Dashboard ────────────────────────────────────────────────────
  'dash.hero.title':     { fr: 'Portfolio de Projets',           en: 'Project Portfolio' },
  'dash.hero.sub':       { fr: 'projet(s) · Marge moyenne :',   en: 'project(s) · Average margin:' },
  'dash.hero.loading':   { fr: 'Chargement des statistiques...', en: 'Loading statistics...' },
  'dash.hero.new':       { fr: '+ Nouveau Projet',              en: '+ New Project' },

  'kpi.total_projects':  { fr: 'Total Projets',    en: 'Total Projects' },
  'kpi.active':          { fr: 'Actifs',           en: 'Active' },
  'kpi.total_revenue':   { fr: 'Revenu Total',     en: 'Total Revenue' },
  'kpi.revenue_budget':  { fr: 'CA Budget',        en: 'Revenue Budget' },
  'kpi.total_cost':      { fr: 'Coût Total',       en: 'Total Cost' },
  'kpi.cumul_cost':      { fr: 'Coûts cumulés',    en: 'Cumulative costs' },
  'kpi.total_margin':    { fr: 'Marge Totale',     en: 'Total Margin' },

  'filter.title':        { fr: 'Filtres Avancés',    en: 'Advanced Filters' },
  'filter.project_status':{ fr: 'Statut du Projet', en: 'Project Status' },
  'filter.all_bu':       { fr: 'Tous les BU',        en: 'All BU' },
  'filter.all_status':   { fr: 'Tous les statuts',   en: 'All statuses' },
  'filter.all_customers':{ fr: 'Tous les clients',   en: 'All customers' },

  'table.title':         { fr: 'Liste des Projets',  en: 'Project List' },
  'table.found':         { fr: 'projets trouvés',    en: 'projects found' },
  'table.empty':         { fr: 'Aucun projet trouvé', en: 'No projects found' },
  'table.col.revenue':   { fr: 'Revenu',             en: 'Revenue' },
  'table.col.cost':      { fr: 'Coût',               en: 'Cost' },
  'table.col.margin_eur':{ fr: 'Marge €',            en: 'Margin €' },
  'table.col.margin_pct':{ fr: 'Marge %',            en: 'Margin %' },
  'table.action.view':   { fr: '👁 Voir',            en: '👁 View' },

  // ── Project detail modal ──────────────────────────────────────────
  'modal.detail.title':  { fr: 'Détail du Projet',   en: 'Project Details' },
  'modal.detail.major':  { fr: 'Majeur',             en: 'Major' },
  'modal.detail.org':    { fr: 'Organisation',       en: 'Organization' },
  'modal.detail.year':   { fr: 'Année',              en: 'Year' },
  'modal.detail.major_project': { fr: 'Projet Majeur',  en: 'Major Project' },
  'modal.detail.market': { fr: 'Client et Marché',   en: 'Client & Market' },
  'modal.detail.client_group':  { fr: 'Groupe Client',   en: 'Client Group' },
  'modal.detail.client_tri':    { fr: 'Trigramme Client', en: 'Client Trigram' },
  'modal.detail.industry_tri':  { fr: 'Trigramme Industrie', en: 'Industry Trigram' },
  'modal.detail.engage_type':   { fr: 'Type Engagement',  en: 'Engagement Type' },
  'modal.detail.technical':     { fr: 'Technique',         en: 'Technical' },
  'modal.detail.pm':            { fr: 'Chef de Projet',    en: 'Project Manager' },
  'modal.detail.tech_office':   { fr: 'Bureau Tech.',      en: 'Tech. Office' },
  'modal.detail.planning':      { fr: 'Planning',          en: 'Planning' },
  'modal.detail.start':         { fr: 'Date Début',        en: 'Start Date' },
  'modal.detail.end':           { fr: 'Date Fin',          en: 'End Date' },
  'modal.detail.status':        { fr: 'Statut',            en: 'Status' },
  'modal.detail.created':       { fr: 'Créé le',           en: 'Created' },
  'modal.detail.updated':       { fr: 'Modifié le',        en: 'Updated' },
  'modal.detail.forecasts':     { fr: '📅 Prévisions Mensuelles', en: '📅 Monthly Forecasts' },
  'modal.detail.month':         { fr: 'Mois',              en: 'Month' },
  'modal.detail.cost':          { fr: 'Coût',              en: 'Cost' },
  'modal.detail.ca':            { fr: 'CA Budget',         en: 'Revenue Budget' },
  'modal.detail.pm_action':     { fr: '📊 Gestion de Projet', en: '📊 Project Management' },

  // ── PM Header KPIs ───────────────────────────────────────────────
  'pm.kpi.direct_cost':  { fr: 'Coût Direct Total', en: 'Total Direct Cost' },
  'pm.kpi.revenue':      { fr: 'Revenu Total',      en: 'Total Revenue' },
  'pm.kpi.margin':       { fr: 'Marge',             en: 'Margin' },
  'pm.kpi.margin_pct':   { fr: 'Marge %',           en: 'Margin %' },

  // ── Project Management ───────────────────────────────────────────
  'pm.title':            { fr: 'Gestion de Projet',  en: 'Project Management' },
  'pm.loading':          { fr: 'Chargement...',       en: 'Loading...' },
  'pm.legend.past':      { fr: '■ Période passée',   en: '■ Past period' },
  'pm.legend.current':   { fr: '■ Période courante', en: '■ Current period' },
  'pm.legend.future':    { fr: '■ Période future',   en: '■ Future period' },

  // Validation bar
  'pm.val.draft':        { fr: 'Brouillon — saisie en cours',           en: 'Draft — data entry in progress' },
  'pm.val.submitted':    { fr: 'En attente de validation BUM',          en: 'Pending BUM validation' },
  'pm.val.validated':    { fr: 'Validé par',                            en: 'Validated by' },
  'pm.val.rejected':     { fr: 'Rejeté —',                              en: 'Rejected —' },
  'pm.val.submit_btn':   { fr: 'Soumettre pour validation →',           en: 'Submit for Validation →' },

  // Confirm modals
  'pm.modal.submit.title': { fr: 'Soumettre pour validation',  en: 'Submit for Validation' },
  'pm.modal.submit.msg':   {
    fr: 'Vous êtes sur le point de soumettre les données de ce projet à <strong>{0}</strong> pour validation BUM.<br><br>Une fois soumis, vous ne pourrez plus modifier les données tant que le BUM n\'aura pas rendu sa décision.',
    en: 'You are about to submit this project\'s data to <strong>{0}</strong> for BUM validation.<br><br>Once submitted, you will not be able to edit the data until the BUM has made a decision.'
  },
  'pm.modal.submit.btn':   { fr: 'Soumettre',   en: 'Submit' },

  'pm.modal.validate.title': { fr: 'Valider les données du projet', en: 'Validate Project Data' },
  'pm.modal.validate.msg':   {
    fr: 'Vous confirmez la validation définitive des données de gestion de ce projet.<br><br>Cette action clôture la saisie côté PM. Elle peut être annulée uniquement via un rejet.',
    en: 'You confirm the final validation of this project\'s management data.<br><br>This action closes PM data entry. It can only be reversed via a rejection.'
  },
  'pm.modal.validate.btn':   { fr: 'Valider',   en: 'Validate' },

  'pm.modal.delete.title':   { fr: 'Confirmer la suppression', en: 'Confirm Deletion' },
  'pm.modal.delete.msg':     {
    fr: 'Vous êtes sur le point de supprimer <strong>« {0} »</strong> ainsi que toutes les données associées.<br><br>',
    en: 'You are about to delete <strong>"{0}"</strong> and all associated data.<br><br>'
  },

  'pm.modal.reject.title':   { fr: 'Rejeter les données du projet', en: 'Reject Project Data' },
  'pm.modal.reject.reason':  { fr: 'Motif de rejet *',              en: 'Rejection reason *' },
  'pm.modal.reject.placeholder': { fr: 'Expliquer ce qui doit être corrigé...', en: 'Explain what needs to be corrected...' },
  'pm.modal.reject.btn':     { fr: 'Rejeter',   en: 'Reject' },

  // Granularity modal
  'pm.gran.title':      { fr: 'Choisir la granularité de suivi', en: 'Choose Tracking Granularity' },
  'pm.gran.warning':    { fr: '🔒 Ce paramètre est <strong>permanent</strong> une fois confirmé.', en: '🔒 This setting is <strong>permanent</strong> once confirmed.' },
  'pm.gran.monthly':    { fr: 'Colonnes mensuelles (Jan, Fév...)', en: 'Monthly columns (Jan, Feb...)' },
  'pm.gran.weekly':     { fr: 'Colonnes hebdomadaires (S01, S02...)', en: 'Weekly columns (W01, W02...)' },
  'pm.gran.daily':      { fr: 'Colonnes journalières (01/01, 02/01...)', en: 'Daily columns (01/01, 02/01...)' },
  'pm.gran.confirm':    { fr: 'Confirmer',   en: 'Confirm' },

  // Panels / tables
  'pm.no_consultant':   { fr: 'Aucun consultant — cliquez sur "Add Consultant"', en: 'No consultant — click "Add Consultant"' },
  'pm.no_category':     { fr: 'Aucune catégorie — cliquez sur "Add Category" pour démarrer', en: 'No category — click "Add Category" to start' },
  'pm.no_consultant_short': { fr: 'Aucun consultant.', en: 'No consultant.' },
  'pm.rebill_legend':   { fr: '= Refacturé au client',  en: '= Rebilled to client' },

  // SDH import
  'pm.sdh.btn':         { fr: 'Import SDH',      en: 'Import SDH' },
  'pm.sdh.title':       { fr: 'Résultat Import SDH', en: 'SDH Import Result' },
  'pm.sdh.imported':    { fr: 'ligne(s) importée(s)', en: 'row(s) imported' },
  'pm.sdh.skipped':     { fr: 'ignorée(s)',       en: 'skipped' },
  'pm.sdh.errors':      { fr: 'Erreurs :',        en: 'Errors:' },

  // Category modal
  'pm.cat.title':       { fr: 'Ajouter une catégorie de coût', en: 'Add Cost Category' },
  'pm.cat.custom_placeholder': { fr: 'Nom de la catégorie...', en: 'Enter category name...' },

  // Add consultant modal
  'pm.add.title':       { fr: 'Ajouter un consultant', en: 'Add Consultant' },
  'pm.add.fullname':    { fr: 'Nom complet *',         en: 'Full name *' },
  'pm.add.fullname_ph': { fr: 'Ex: Jean Dupont',       en: 'Ex: John Smith' },
  'pm.add.staffid':     { fr: 'Matricule',             en: 'Staff ID' },
  'pm.add.contract':    { fr: 'Type de contrat',       en: 'Contract type' },
  'pm.add.btn':         { fr: 'Ajouter',               en: 'Add' },

  // ── Risk Register ─────────────────────────────────────────────────
  'rr.add':          { fr: '+ Nouveau Risk',        en: '+ New Risk' },
  'rr.kpi.total':    { fr: 'Total Risques',         en: 'Total Risks' },
  'rr.kpi.open':     { fr: 'Ouverts',               en: 'Open' },
  'rr.kpi.critical': { fr: 'Critique',              en: 'Critical' },
  'rr.empty':        { fr: 'Aucun risk — cliquez sur', en: 'No risk — click on' },
  'rr.col.risk':     { fr: 'Risk',                  en: 'Risk' },
  'rr.col.category': { fr: 'Catégorie',             en: 'Category' },
  'rr.col.prob':     { fr: 'Probabilité',           en: 'Probability' },
  'rr.col.impact':   { fr: 'Impact',                en: 'Impact' },
  'rr.col.rating':   { fr: 'Rating',                en: 'Rating' },
  'rr.col.strategy': { fr: 'Stratégie',             en: 'Strategy' },
  'rr.col.status':   { fr: 'Statut',                en: 'Status' },
  'rr.col.owner':    { fr: 'Responsable',           en: 'Owner' },
  'rr.col.deadline': { fr: 'Échéance',              en: 'Deadline' },
  'rr.col.res_time': { fr: 'Rés.',                  en: 'Res.' },
  'rr.sec.ident':    { fr: '📋 Identification',     en: '📋 Identification' },
  'rr.sec.categ':    { fr: '🎯 Catégorisation du Risk', en: '🎯 Risk Categorization' },
  'rr.sec.mgmt':     { fr: '🛡️ Risk Management',    en: '🛡️ Risk Management' },
  'rr.sec.followup': { fr: '📅 Follow Up',           en: '📅 Follow Up' },
  'rr.modal.new':    { fr: 'Nouveau Risk',           en: 'New Risk' },
  'rr.modal.view':   { fr: '👁 Lecture seule — cliquez sur la ligne pour modifier', en: '👁 View only — click on the row to edit' },
  'rr.field.risk':      { fr: 'Risk * (Si... Alors... Avec...)', en: 'Risk * (If... Then... With...)' },
  'rr.field.phase':     { fr: 'Phase',               en: 'Phase' },
  'rr.field.category':  { fr: 'Catégorie',           en: 'Category' },
  'rr.field.prob':      { fr: 'Probabilité',         en: 'Probability' },
  'rr.field.impact':    { fr: 'Impact',              en: 'Impact' },
  'rr.field.strategy':  { fr: 'Management Strategy', en: 'Management Strategy' },
  'rr.field.owner':     { fr: 'Responsable',         en: 'Owner' },
  'rr.field.mitigation':{ fr: 'Action de Mitigation', en: 'Mitigation Action' },
  'rr.field.costs':     { fr: 'Coûts (€)',           en: 'Costs (€)' },
  'rr.field.residual':  { fr: 'Probabilité Résiduelle (%)', en: 'Probability Residual (%)' },
  'rr.field.contingency':{ fr: 'Action de Contingence', en: 'Contingency Action' },
  'rr.field.trigger':   { fr: 'Déclencheur',         en: 'Trigger' },
  'rr.field.res_action':{ fr: 'Action Résiduelle',   en: 'Residual Action' },
  'rr.field.deadline':  { fr: 'Échéance',            en: 'Deadline' },
  'rr.field.status':    { fr: 'Statut',              en: 'Status' },
  'rr.field.closure':   { fr: 'Date de Clôture',     en: 'Closure Date' },

  // ── Issue Register ─────────────────────────────────────────────────
  'ir.add':          { fr: '+ Nouvelle Issue',       en: '+ New Issue' },
  'ir.kpi.total':    { fr: 'Total',                  en: 'Total' },
  'ir.kpi.open':     { fr: 'Ouvertes',               en: 'Open' },
  'ir.kpi.critical': { fr: 'Critiques',              en: 'Critical' },
  'ir.kpi.closed':   { fr: 'Clôturées',              en: 'Closed' },
  'ir.empty':        { fr: 'Aucune issue',           en: 'No issues' },
  'ir.col.issue':    { fr: 'Issue',                  en: 'Issue' },
  'ir.col.severity': { fr: 'Sévérité',               en: 'Severity' },
  'ir.col.priority': { fr: 'Priorité',               en: 'Priority' },
  'ir.col.impacts':  { fr: 'Impacts',                en: 'Impacts' },
  'ir.col.status':   { fr: 'Statut',                 en: 'Status' },
  'ir.col.dte':      { fr: 'DTE',                    en: 'DTE' },
  'ir.col.deadline': { fr: 'Échéance',               en: 'Deadline' },
  'ir.col.owner':    { fr: 'Responsable',            en: 'Owner' },
  'ir.col.res_time': { fr: 'Tps Rés.',               en: 'Res. Time' },
  'ir.sec.ident':    { fr: '📋 Identification',      en: '📋 Identification' },
  'ir.sec.resolution':{ fr: '🔧 Plan de Résolution', en: '🔧 Resolution Plan' },
  'ir.sec.owner':    { fr: '👤 Responsable & Communication', en: '👤 Owner & Communication' },
  'ir.sec.escalade': { fr: '🚨 Escalade & Clôture',  en: '🚨 Escalation & Closure' },
  'ir.modal.new':    { fr: 'Nouvelle Issue',         en: 'New Issue' },
  'ir.modal.view':   { fr: '👁 Lecture seule',       en: '👁 View only' },
  'ir.field.issue':  { fr: 'Issue * (FAIT / IMPACT / CONSÉQUENCE)', en: 'Issue * (FACT / IMPACT / CONSEQUENCE)' },
  'ir.field.severity':{ fr: 'Sévérité',              en: 'Severity' },
  'ir.field.priority':{ fr: 'Priorité',              en: 'Priority' },
  'ir.field.impacts': { fr: 'Impacts',               en: 'Impacts' },
  'ir.field.dte':    { fr: 'DTE (date informé)',     en: 'DTE (date informed)' },
  'ir.field.dta':    { fr: 'DTA (date acknowled.)',  en: 'DTA (date acknowledged)' },
  'ir.field.deadline':{ fr: 'ETA / Échéance *',      en: 'ETA / Deadline *' },
  'ir.field.lockdown':{ fr: 'Verrouillage',          en: 'Lockdown' },
  'ir.field.investigation':{ fr: 'Investigation',    en: 'Investigation' },
  'ir.field.resolution':{ fr: 'Résolution Durable',  en: 'Sustainable Resolution' },
  'ir.field.exit':   { fr: 'Critères de Sortie',     en: 'Exit Criteria' },
  'ir.field.owner':  { fr: 'Responsable',            en: 'Owner' },
  'ir.field.quality_leader':{ fr: 'Responsable Qualité', en: 'Quality Leader' },
  'ir.field.communication':{ fr: 'Communication',    en: 'Communication' },
  'ir.field.escalade_level':{ fr: 'Niveau Escalade', en: 'Escalation Level' },
  'ir.field.decision':{ fr: 'Décision',              en: 'Decision' },
  'ir.field.link':   { fr: 'Lien (N/A ou R_ID)',     en: 'Link (N/A or R_ID)' },
  'ir.field.status': { fr: 'Statut',                 en: 'Status' },
  'ir.field.dtr':    { fr: 'DTR (Date Résolution)',  en: 'DTR (Date Resolution)' },
  'ir.field.res_time':{ fr: 'Temps de Résolution',   en: 'Resolution Time' },
  'ir.field.remarks':{ fr: 'Remarques',              en: 'Remarks' },

  // ── Opportunity Register ─────────────────────────────────────────────────
  'or.add':          { fr: '+ Nouvelle Opportunité', en: '+ New Opportunity' },
  'or.kpi.total':    { fr: 'Total',                  en: 'Total' },
  'or.kpi.benefit':  { fr: 'Bénéfice Estimé',        en: 'Estimated Benefit' },
  'or.kpi.won':      { fr: 'Gagnées',                en: 'Won' },
  'or.kpi.overdue':  { fr: 'En Retard',              en: 'Overdue' },
  'or.empty':        { fr: 'Aucune opportunité',     en: 'No opportunities' },
  'or.col.desc':     { fr: 'Description',            en: 'Description' },
  'or.col.category': { fr: 'Catégorie',              en: 'Category' },
  'or.col.benefit':  { fr: 'Bénéfice Estimé',        en: 'Estimated Benefit' },
  'or.col.pct':      { fr: '% New PM',               en: '% New PM' },
  'or.col.status':   { fr: 'Statut',                 en: 'Status' },
  'or.col.copil':    { fr: 'COPIL',                  en: 'COPIL' },
  'or.col.deadline': { fr: 'Échéance',               en: 'Deadline' },
  'or.col.overdue':  { fr: 'En Retard',              en: 'Overdue' },
  'or.col.onhold':   { fr: 'En Attente',             en: 'On Hold' },
  'or.col.owner':    { fr: 'Responsable',            en: 'Owner' },
  'or.sec.ident':    { fr: '📋 Identification',      en: '📋 Identification' },
  'or.sec.financial':{ fr: '💰 Financier',           en: '💰 Financial' },
  'or.sec.action':   { fr: '🎯 Action',              en: '🎯 Action' },
  'or.sec.tracking': { fr: '📊 Suivi & Validation',  en: '📊 Tracking & Validation' },
  'or.modal.new':    { fr: 'Nouvelle Opportunité',   en: 'New Opportunity' },
  'or.field.desc':   { fr: 'Description * (SI... Alors... Avec...)', en: 'Description * (IF... Then... With...)' },
  'or.field.date':   { fr: 'Date d\'identification', en: 'Identification Date' },
  'or.field.deadline':{ fr: 'Échéance *',            en: 'Deadline *' },
  'or.field.category':{ fr: 'Catégorie',             en: 'Category' },
  'or.field.costs':  { fr: 'Coûts (€)',              en: 'Costs (€)' },
  'or.field.price':  { fr: 'Prix (€)',               en: 'Price (€)' },
  'or.field.benefit':{ fr: 'Bénéfice Estimé',        en: 'Estimated Benefit' },
  'or.field.pct':    { fr: '% New PM',               en: '% New PM' },
  'or.field.owner':  { fr: 'Responsable',            en: 'Owner' },
  'or.field.action': { fr: 'Action à mener',         en: 'Action to be taken' },
  'or.field.status': { fr: 'Statut',                 en: 'Status' },
  'or.field.copil':  { fr: 'Validation COPIL',       en: 'COPIL Validation' },
  'or.field.onhold': { fr: 'En Attente ? (Non par défaut)', en: 'On Hold? (No by default)' },
  'or.field.overdue':{ fr: 'En Retard',              en: 'Overdue' },
  'or.field.comments':{ fr: 'Commentaires',          en: 'Comments' },

  // ── MIP Register ─────────────────────────────────────────────────
  'mr.add':          { fr: '+ Nouvelle Action MIP',  en: '+ New MIP Action' },
  'mr.kpi.total':    { fr: 'Total MIP',              en: 'Total MIP' },
  'mr.kpi.completed':{ fr: 'Terminées',              en: 'Completed' },
  'mr.kpi.planned':  { fr: 'Gain Planifié',          en: 'Planned Gain' },
  'mr.kpi.realized': { fr: 'Gain Réalisé',           en: 'Realized Gain' },
  'mr.empty':        { fr: 'Aucune action MIP',      en: 'No MIP actions' },
  'mr.col.lever':    { fr: 'Levier',                 en: 'Lever' },
  'mr.col.action':   { fr: 'Action (description)',   en: 'Action (description)' },
  'mr.col.accountable':{ fr: 'Responsable',          en: 'Accountable' },
  'mr.col.client_impact':{ fr: 'Impact Client',      en: 'Client Impact' },
  'mr.col.due_date': { fr: 'Échéance',               en: 'Due Date' },
  'mr.col.priority': { fr: 'Priorité',               en: 'Priority' },
  'mr.col.planned':  { fr: 'Gain Planifié',          en: 'Planned Gain' },
  'mr.col.realized': { fr: 'Gain Réalisé',           en: 'Realized Gain' },
  'mr.col.status':   { fr: 'Statut',                 en: 'Status' },
  'mr.sec.ident':    { fr: '📋 Identification',      en: '📋 Identification' },
  'mr.sec.eval':     { fr: '📊 Évaluation & Priorité', en: '📊 Evaluation & Priority' },
  'mr.sec.gains':    { fr: '💰 Gains & Statut',      en: '💰 Gains & Status' },
  'mr.modal.new':    { fr: 'Nouvelle Action MIP',    en: 'New MIP Action' },
  'mr.field.action': { fr: 'Action * (Verbe + action précise + objectif mesurable)', en: 'Action * (Verb + precise action + measurable objective)' },
  'mr.field.date':   { fr: 'Date d\'identification', en: 'Identification Date' },
  'mr.field.lever':  { fr: 'Levier',                 en: 'Lever' },
  'mr.field.accountable':{ fr: 'Responsable',        en: 'Accountable' },
  'mr.field.client_impact':{ fr: 'Impact Client',    en: 'Client Impact' },
  'mr.field.due_date':{ fr: 'Échéance',              en: 'Due Date' },
  'mr.field.priority':{ fr: 'Priorité',              en: 'Priority' },
  'mr.field.risks':  { fr: 'Risques / Prérequis',    en: 'Risks / Prerequisites' },
  'mr.field.planned':{ fr: 'Gain Planifié (€)',      en: 'Planned Gain (€)' },
  'mr.field.realized':{ fr: 'Gain Réalisé (€)',      en: 'Realized Gain (€)' },
  'mr.field.status': { fr: 'Statut',                 en: 'Status' },

  // ── WIP Register ────────────────────────────────────────────────
  'wip.title':           { fr: 'WIP — Work In Progress',         en: 'WIP — Work In Progress' },
  'wip.btn.import':      { fr: 'Importer un BL Signé',           en: 'Import Signed Delivery Note' },
  'wip.btn.confirm':     { fr: 'Confirmer & Enregistrer',        en: 'Confirm & Save' },
  'wip.modal.title':     { fr: 'Données extraites du BL',        en: 'Extracted BL Data' },
  'wip.modal.subtitle':  { fr: 'Vérifiez et corrigez si nécessaire', en: 'Check and correct if needed' },
  'wip.col.wipId':       { fr: 'WIP-ID',                         en: 'WIP-ID' },
  'wip.col.blNumber':    { fr: 'N° BL',                          en: 'BL Number' },
  'wip.col.period':      { fr: 'Période',                        en: 'Period' },
  'wip.col.bu':          { fr: 'Business Unit',                  en: 'Business Unit' },
  'wip.col.blDays':      { fr: 'Jours BL',                       en: 'BL Days' },
  'wip.col.billedDays':  { fr: 'Jours Facturés',                 en: 'Billed Days' },
  'wip.col.wipDays':     { fr: 'WIP (jours)',                    en: 'WIP (days)' },
  'wip.col.blAmount':    { fr: 'Montant BL (€)',                 en: 'BL Amount (€)' },
  'wip.col.wipAmount':   { fr: 'WIP (€)',                        en: 'WIP (€)' },
  'wip.col.status':      { fr: 'Statut',                         en: 'Status' },
  'wip.col.file':        { fr: 'Fichier',                        en: 'File' },
  'wip.col.actions':     { fr: 'Actions',                        en: 'Actions' },
  'wip.status.open':     { fr: 'Open',                           en: 'Open' },
  'wip.status.closed':   { fr: 'Closed',                         en: 'Closed' },
  'wip.status.disputed': { fr: 'Disputed',                       en: 'Disputed' },
  'wip.kpi.total_bl':    { fr: 'Total Jours BL',                 en: 'Total BL Days' },
  'wip.kpi.total_billed':{ fr: 'Total Jours Facturés',           en: 'Total Billed Days' },
  'wip.kpi.total_wip':   { fr: 'WIP Total (jours)',              en: 'Total WIP (days)' },
  'wip.kpi.wip_amount':  { fr: 'WIP Total (€)',                  en: 'Total WIP (€)' },
  'wip.empty':           { fr: 'Aucun BL importé pour ce projet', en: 'No delivery note imported for this project' },
  'wip.parsing':         { fr: 'Extraction en cours...',         en: 'Extracting data...' },
  'wip.field.notes':     { fr: 'Notes',                          en: 'Notes' },
  'wip.field.blNumber':  { fr: 'N° Bon de Livraison',            en: 'Delivery Note Number' },
  'wip.field.period':    { fr: 'Période (YYYY-MM)',              en: 'Period (YYYY-MM)' },
  'wip.field.bu':        { fr: 'Business Unit',                  en: 'Business Unit' },
  'wip.field.blDays':    { fr: 'Jours BL Signé',                 en: 'Signed BL Days' },
  'wip.field.billedDays':{ fr: 'Jours Facturés (auto)',          en: 'Billed Days (auto)' },
  'wip.field.wipDays':   { fr: 'WIP = Facturés − BL (auto)',     en: 'WIP = Billed − BL (auto)' },
  'wip.field.blAmount':  { fr: 'Montant BL (€)',                 en: 'BL Amount (€)' },
  'wip.positive':        { fr: 'Jours non encore livrés',        en: 'Days not yet delivered' },
  'wip.negative':        { fr: 'BL en avance sur facturation',   en: 'BL ahead of billing' },
  'wip.zero':            { fr: 'Équilibré',                      en: 'Balanced' },

  // Commun
  'common.save':     { fr: 'Enregistrer',            en: 'Save' },
  'common.saving':   { fr: 'Enregistrement...',      en: 'Saving...' },
  'common.cancel':   { fr: 'Annuler',                en: 'Cancel' },
  'common.create':   { fr: 'Créer',                  en: 'Create' },
  'common.creating': { fr: 'Création...',            en: 'Creating...' },
  'common.delete':   { fr: 'Supprimer',              en: 'Delete' },
  'common.confirm_delete': { fr: 'Cette action est irréversible.', en: 'This action is irreversible.' },
  'common.view_only':{ fr: '👁 Lecture seule',       en: '👁 View only' },
  'common.irreversible': { fr: 'Cette action est irréversible.', en: 'This action is irreversible.' },
  'common.auto':     { fr: 'auto',                   en: 'auto' },
  'common.bum':      { fr: 'BUM',                    en: 'BUM' },
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  readonly lang = signal<Lang>((localStorage.getItem('app_lang') as Lang) || 'fr');

  setLang(l: Lang): void {
    this.lang.set(l);
    localStorage.setItem('app_lang', l);
  }

  toggle(): void {
    this.setLang(this.lang() === 'fr' ? 'en' : 'fr');
  }

  t(key: string, ...args: string[]): string {
    const entry = T[key];
    if (!entry) return key;
    let str = entry[this.lang()] ?? entry['fr'];
    args.forEach((a, i) => { str = str.replace(`{${i}}`, a); });
    return str;
  }
}
