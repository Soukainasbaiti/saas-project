import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProjectListDto, ProjectDetailDto, PagedResponse,
  DashboardStats, ReferenceDto, ProjectCreateRequest, MonthlyForecastDto
} from '../models/project.model';

export interface ProjectSubmitResponse {
  message: string;
  approvalToken: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly base = '/api';

  constructor(private http: HttpClient) {}

  // ── Projects ──────────────────────────────────────────────────
  getProjects(filters: {
    buId?: string;
    customerId?: number;
    status?: string;
    year?: number;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PagedResponse<ProjectListDto>> {
    let params = new HttpParams();
    if (filters.buId)       params = params.set('buId', filters.buId);
    if (filters.customerId) params = params.set('customerId', filters.customerId);
    if (filters.status)     params = params.set('status', filters.status);
    if (filters.year)       params = params.set('year', filters.year);
    if (filters.search)     params = params.set('search', filters.search);
    params = params.set('page', filters.page ?? 0);
    params = params.set('size', filters.size ?? 20);
    return this.http.get<PagedResponse<ProjectListDto>>(`${this.base}/projects`, { params });
  }

  getProject(id: number): Observable<ProjectDetailDto> {
    return this.http.get<ProjectDetailDto>(`${this.base}/projects/${id}`);
  }

  // ── Retourne 202 Accepted (soumis pour approbation) ──────────
  createProject(req: ProjectCreateRequest): Observable<ProjectSubmitResponse> {
    return this.http.post<ProjectSubmitResponse>(`${this.base}/projects`, req);
  }

  archiveProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${id}`);
  }

  updateProject(id: number, req: ProjectCreateRequest): Observable<ProjectDetailDto> {
    return this.http.put<ProjectDetailDto>(`${this.base}/projects/${id}`, req);
  }

  updateMonthlyForecasts(id: number, updates: { month: string; revenue?: number | null; cost?: number | null; cov: number | null }[]): Observable<MonthlyForecastDto[]> {
    return this.http.put<MonthlyForecastDto[]>(`${this.base}/projects/${id}/monthly-forecasts`, updates);
  }

  // ── Admin ─────────────────────────────────────────────────────
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/admin/users`);
  }

  createUser(req: { fullName: string; email: string; password: string; role: string }): Observable<any> {
    return this.http.post<any>(`${this.base}/admin/users`, req);
  }

  updateUser(id: number, req: { fullName?: string; email?: string; role?: string; isActive?: boolean }): Observable<any> {
    return this.http.put<any>(`${this.base}/admin/users/${id}`, req);
  }

  deleteUser(id: number): Observable<any> {
    return this.http.delete<any>(`${this.base}/admin/users/${id}`);
  }

  // ── Admin Ref Data ────────────────────────────────────────────
  getAdminRef(type: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/admin/ref/${type}`);
  }
  createAdminRef(type: string, body: any): Observable<any> {
    return this.http.post<any>(`${this.base}/admin/ref/${type}`, body);
  }
  updateAdminRef(type: string, id: any, body: any): Observable<any> {
    return this.http.put<any>(`${this.base}/admin/ref/${type}/${id}`, body);
  }
  toggleAdminRef(type: string, id: any): Observable<any> {
    return this.http.patch<any>(`${this.base}/admin/ref/${type}/${id}/toggle`, {});
  }

  createProjectDirect(req: ProjectCreateRequest): Observable<ProjectDetailDto> {
    return this.http.post<ProjectDetailDto>(`${this.base}/admin/projects/direct`, req);
  }

  getAdminModuleStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/admin/module-stats`);
  }

  getAdminWipGlobal(): Observable<any> {
    return this.http.get<any>(`${this.base}/admin/wip-global`);
  }

  getAdminPending(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/admin/pending`);
  }

  approveProject(token: string): Observable<any> {
    return this.http.post<any>(`${this.base}/admin/approve/${token}`, { action: 'approve' });
  }

  rejectProjectByToken(token: string, reason: string): Observable<any> {
    return this.http.post<any>(`${this.base}/admin/approve/${token}`, { action: 'reject', reason });
  }

  getDashboardStats(year?: number): Observable<DashboardStats> {
    let params = new HttpParams();
    if (year) params = params.set('year', year);
    return this.http.get<DashboardStats>(`${this.base}/projects/stats/dashboard`, { params });
  }

  // ── Reference Data ────────────────────────────────────────────
  getBus():             Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/bus`); }
  getCustomers():       Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/customers`); }
  getIndustries():      Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/industries`); }
  getDisciplines():     Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/disciplines`); }
  getEngagements():     Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/engagements`); }
  getFunctions(disciplineId?: number): Observable<ReferenceDto[]> {
    let params = new HttpParams();
    if (disciplineId) params = params.set('disciplineId', disciplineId);
    return this.http.get<ReferenceDto[]>(`${this.base}/ref/functions`, { params });
  }
  getFrontFinanciers(): Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/front-financiers`); }
  getPMs():             Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/pms`); }
  getCountries():       Observable<ReferenceDto[]> { return this.http.get<ReferenceDto[]>(`${this.base}/ref/countries`); }

  // ── Pays d'un projet (multi-pays) ─────────────────────────────
  getProjectCountries(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/countries`);
  }
  addProjectCountry(projectId: number, req: { countryId: number; pmId?: number | null }): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/countries`, req);
  }
  assignCountryPm(projectId: number, countryId: number, pmId: number): Observable<any> {
    return this.http.patch<any>(`${this.base}/projects/${projectId}/countries/${countryId}/pm`, { pmId });
  }

  // ── Project Management ────────────────────────────────────────
  getProjectManagement(projectId: number): Observable<any> {
    return this.http.get<any>(`${this.base}/projects/${projectId}/management`);
  }
  addResource(req: { projectId: number; matricule: string; personName: string; countryId: number }): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/management/resource`, req);
  }
  deleteResource(resourceId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/management/resource/${resourceId}`);
  }
  updateResourceCountry(resourceId: number, countryId: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/projects/management/resource/${resourceId}/country`, { countryId });
  }
  saveResourceEntry(req: { resourceId: number; month: string; dailyCost?: number; workedDays?: number; billedDays?: number; dailyRate?: number }): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/management/entry`, req);
  }
  saveOtherCost(req: { projectId: number; category: string; month: string; amount: number; isRebill?: boolean }): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/management/cost`, req);
  }
  addOtherCostCategory(req: { projectId: number; category: string }): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/management/cost/category`, req);
  }
  deleteOtherCostCategory(req: { projectId: number; category: string }): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/management/cost/category`, { body: req });
  }
  setCategoryRebill(req: { projectId: number; category: string; isRebill: boolean }): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/management/cost/rebill`, req);
  }
  setGranularity(projectId: number, granularity: string, currency: string): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/management/granularity`, { granularity, currency });
  }
  updateMonthStatus(projectId: number, updates: { month: string; status: 'REAL' | 'FORECAST' }[]): Observable<{ month: string; status: string }[]> {
    return this.http.put<{ month: string; status: string }[]>(`${this.base}/projects/${projectId}/management/month-status`, updates);
  }
  updateOnePagerExtras(projectId: number, payload: {
    deliveryConfidenceLevel?: string | null;
    healthScoreValue?: number | null;
    healthScoreStatus?: string | null;
    pmRemarks?: string | null;
    varianceActualComment?: string | null;
    varianceTrendComment?: string | null;
    varianceLandingComment?: string | null;
    tops?: string | null;
    flops?: string | null;
  }): Observable<void> {
    return this.http.put<void>(`${this.base}/projects/${projectId}/management/onepager-extras`, payload);
  }

  // ── Validation workflow ────────────────────────────────────────
  submitForValidation(projectId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/${projectId}/management/submit`, {});
  }
  validateProject(projectId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/${projectId}/management/validate`, {});
  }
  rejectProject(projectId: number, comment: string): Observable<void> {
    return this.http.post<void>(`${this.base}/projects/${projectId}/management/reject`, { comment });
  }
  getBumPending(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/management/bum/pending`);
  }
  getPmRejected(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/management/pm/rejected`);
  }
  getBumHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/management/bum/history`);
  }

  // ── SDH Import ─────────────────────────────────────────────────
  importSdhFile(projectId: number, file: File): Observable<any> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<any>(`${this.base}/projects/${projectId}/management/import-sdh`, form);
  }

  // ── Work Package — Deliverables ──────────────────────────────────
  getDeliverables(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/deliverables`);
  }

  createDeliverable(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/deliverables`, req);
  }

  updateDeliverable(projectId: number, delivId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/deliverables/${delivId}`, req);
  }

  deleteDeliverable(projectId: number, delivId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/deliverables/${delivId}`);
  }

  // ── Risk Register ───────────────────────────────────────────────
  getRisks(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/risks`);
  }
  createRisk(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/risks`, req);
  }
  updateRisk(projectId: number, riskId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/risks/${riskId}`, req);
  }
  deleteRisk(projectId: number, riskId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/risks/${riskId}`);
  }

  // ── MIP Register ─────────────────────────────────────────────────
  getMips(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/mips`);
  }
  createMip(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/mips`, req);
  }
  updateMip(projectId: number, mipId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/mips/${mipId}`, req);
  }
  deleteMip(projectId: number, mipId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/mips/${mipId}`);
  }

  // ── Opportunity Register ─────────────────────────────────────────
  getOpportunities(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/opportunities`);
  }
  createOpportunity(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/opportunities`, req);
  }
  updateOpportunity(projectId: number, oppId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/opportunities/${oppId}`, req);
  }
  deleteOpportunity(projectId: number, oppId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/opportunities/${oppId}`);
  }

  // ── Issue Register ───────────────────────────────────────────────
  getIssues(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/issues`);
  }
  createIssue(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/issues`, req);
  }
  updateIssue(projectId: number, issueId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/issues/${issueId}`, req);
  }
  deleteIssue(projectId: number, issueId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/issues/${issueId}`);
  }

  changeEngagement(projectId: number, engagementType: string): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/engagement`, { engagementType });
  }

  // ── Unit of Work — Work Types ────────────────────────────────────
  getWorkTypes(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/work-types`);
  }
  createWorkType(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/work-types`, req);
  }
  deleteWorkType(projectId: number, typeId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/work-types/${typeId}`);
  }

  // ── Unit of Work — Tickets ───────────────────────────────────────
  getWorkTickets(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/work-tickets`);
  }
  createWorkTicket(projectId: number, req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/work-tickets`, req);
  }
  updateWorkTicket(projectId: number, ticketId: number, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/work-tickets/${ticketId}`, req);
  }
  deleteWorkTicket(projectId: number, ticketId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/work-tickets/${ticketId}`);
  }

  // ── WIP — Nouvelle API (tableau mensuel + documents) ─────────────
  getWipTable(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/wip/table`);
  }
  uploadWipDocument(projectId: number, year: number, month: number, type: string, file: File): Observable<any> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<any>(
      `${this.base}/projects/${projectId}/wip/months/${year}/${month}/upload?type=${type}`, form);
  }
  confirmWipDocumentAmount(projectId: number, docId: number, confirmedAmount: number): Observable<any> {
    return this.http.put<any>(
      `${this.base}/projects/${projectId}/wip/documents/${docId}/confirm`,
      { confirmedAmount });
  }
  deleteWipDocument(projectId: number, docId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/wip/documents/${docId}`);
  }
  downloadWipDocument(projectId: number, docId: number): Observable<Blob> {
    return this.http.get(
      `${this.base}/projects/${projectId}/wip/documents/${docId}/download`,
      { responseType: 'blob' });
  }

  // ── WIP — Ancienne API (conservée) ───────────────────────────────
  getWips(projectId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/projects/${projectId}/wip`);
  }
  parseBlPdf(projectId: number, file: File): Observable<any[]> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<any[]>(`${this.base}/projects/${projectId}/wip/parse`, form);
  }
  saveAllWip(projectId: number, dtos: any[]): Observable<any[]> {
    return this.http.post<any[]>(`${this.base}/projects/${projectId}/wip/save-all`, dtos);
  }
  createWip(projectId: number, dto: any): Observable<any> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/wip`, dto);
  }
  updateWip(projectId: number, id: number, dto: any): Observable<any> {
    return this.http.put<any>(`${this.base}/projects/${projectId}/wip/${id}`, dto);
  }
  deleteWip(projectId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/wip/${id}`);
  }
}
