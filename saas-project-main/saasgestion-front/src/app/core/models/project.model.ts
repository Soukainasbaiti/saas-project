// src/app/core/models/project.model.ts

export interface ProjectListDto {
  id: number;
  projectCode: string | null;
  projectId: string | null; // ✅ ajouté
  projectName: string | null;
  projectYear: number;
  frontFinancier: string;
  buId: string;
  buName: string;
  buTrigram: string;
  customerName: string;
  customerTrigram: string;
  customerGroup: string;
  industryId: number;
  industryName: string;
  industryTrigram: string;
  projectManager: string;
  engagementType: string;
  technicalOffice: string;
  activity: string;
  status: 'On Going' | 'Closed';
  majorProject: boolean;
  startDate: string | null;
  endDate: string | null;
  revenueBudget: number;
  costBudget: number;
  marginBudget: number;
  projectMargin: number;
  countries?: ProjectCountryDto[];
}

export interface MonthlyForecastDto {
  month: string;     // "2026-03"
  revenue: number;
  cost: number;
  cov: number | null; // COV : Client Order Value
}

export interface ProjectDetailDto extends ProjectListDto {
  projectId: string | null; // ✅ ajouté
  projectNameLegacy: string | null;
  frontFinancierId: number;
  frontFinancierLabel: string;
  projectManagerId: number;
  projectManager: string;
  pmEmail: string;
  bumName: string;
  customerId: number;
  customerGroup: string;
  industryId: number;
  industryName: string;
  industryTrigram: string;
  engineeringDisciplineId: number;
  engineeringDiscipline: string;
  functionId: number | null;
  functionName: string | null;
  engagementId: number;
  engagement: string;
  engagementType: string;
  technicalOffice: 'Front Office' | 'Back Office';
  createdById: number;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  monthlyForecasts: MonthlyForecastDto[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DashboardStats {
  totalProjects: number;
  activeProjects: number;
  closedProjects: number;
  totalRevenue: number;
  totalCost: number;
  totalMargin: number;
  avgMarginRate: number;
  year: number;
}

export interface ReferenceDto {
  id: string | number;
  label: string;
  code: string | null;
}

export interface ProjectCreateRequest {
  projectCode?: string;
  frontFinancier: string;
  projectManagerId: number;
  buId: string;
  customerId: number;
  industryId: number;
  engineeringDisciplineId: number;
  functionName?: string;
  engagementId: number;
  activity: string;
  revenueBudget: number;
  costBudget: number;
  startDate?: string;
  endDate?: string;
  majorProject: boolean;
  technicalOffice: string;
  status: string;
  projectYear?: number;
  projectNameLegacy?: string;
  projectId?: string;
  monthlyForecasts?: MonthlyForecastDto[];
  frontOfficeCountryId: number;
  backOfficeCountries?: { countryId: number; pmId: number | null }[];
}

export interface ProjectCountryDto {
  id: number;
  countryId: number;
  countryName: string;
  countryIsoCode: string;
  pmId: number | null;
  pmName: string | null;
  isLead: boolean;
  displayOrder: number;
}