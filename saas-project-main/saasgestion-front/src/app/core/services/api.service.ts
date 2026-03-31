import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProjectListDto, ProjectDetailDto, PagedResponse,
  DashboardStats, ReferenceDto, ProjectCreateRequest
} from '../models/project.model';

export interface ProjectSubmitResponse {
  message: string;
  approvalToken: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly base = 'http://localhost:8080/api';

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
}
