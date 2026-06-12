import { Component, HostListener, Inject, inject } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HttpClient, HttpInterceptorFn, HttpParams, provideHttpClient, withInterceptors } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';

function resolveApiBaseUrl(): string {
  const override = localStorage.getItem('apiUrl');
  if (override && override.trim().length > 0) {
    return override.replace(/\/$/, '');
  }

  const host = window.location.hostname;

  // Desarrollo local Angular -> backend Spring Boot local.
  if (host === 'localhost' || host === '127.0.0.1') {
    return 'http://localhost:8086/api';
  }

  // Producción DSSM por dominio institucional.
  if (host === 'convergente.dssm.cl') {
    return 'https://convergente-api.dssm.cl/api';
  }

  // Acceso eventual por IP interna del servidor. Preferir dominio en producción,
  // pero esto permite pruebas internas sin recompilar el frontend.
  if (host === '10.8.74.156') {
    return `${window.location.protocol}//10.8.74.156:8086/api`;
  }

  // Fallback productivo.
  return 'https://convergente-api.dssm.cl/api';
}

const API = resolveApiBaseUrl();
console.info('[DAU DSSM] API base URL:', API);

const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

function pageData(response: any) {
  const data = response?.data || {};
  return {
    rows: data.content || [],
    total: data.totalElements || 0,
  };
}

function notEmpty(value: any): boolean {
  return value !== undefined && value !== null && value !== '';
}

function appendParams(params: HttpParams, values: Record<string, any>): HttpParams {
  Object.keys(values).forEach(key => {
    if (notEmpty(values[key])) params = params.set(key, String(values[key]));
  });
  return params;
}

@Component({
  selector: 'json-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="dialog-title-row">
      <div>
        <h2 mat-dialog-title>Payload JSON recibido</h2>
        <p class="dialog-subtitle">Evento {{data?.tipoEventoInferido || 'DAU'}} · DAU {{data?.idDau || '-'}}</p>
      </div>
      <button mat-icon-button mat-dialog-close matTooltip="Cerrar"><mat-icon>close</mat-icon></button>
    </div>
    <mat-dialog-content>
      <div class="json-actions">
        <button mat-stroked-button (click)="copy()"><mat-icon>content_copy</mat-icon>Copiar JSON</button>
      </div>
      <pre class="json-viewer">{{pretty}}</pre>
    </mat-dialog-content>
  `
})
class JsonDialogComponent {
  data: any;
  snack = inject(MatSnackBar);
  pretty = '';

  constructor(@Inject(MAT_DIALOG_DATA) public injectedData: any) {
    this.data = injectedData;
    const raw = injectedData?.payloadOriginalJson || injectedData?.payload || injectedData || {};
    try {
      this.pretty = typeof raw === 'string' ? JSON.stringify(JSON.parse(raw), null, 2) : JSON.stringify(raw, null, 2);
    } catch {
      this.pretty = String(raw);
    }
  }

  copy() {
    navigator.clipboard?.writeText(this.pretty);
    this.snack.open('JSON copiado al portapapeles', 'OK', { duration: 2200 });
  }
}

@Component({
  selector: 'detail-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatChipsModule, MatDialogModule, MatIconModule, MatTableModule, MatTabsModule, MatTooltipModule],
  template: `
    <div class="dialog-title-row">
      <div>
        <h2 mat-dialog-title>Detalle DAU {{attention.idDau}}</h2>
        <p class="dialog-subtitle">Atención consolidada por idDAU + idAtención · {{attention.idAtencion}}</p>
      </div>
      <button mat-icon-button mat-dialog-close matTooltip="Cerrar"><mat-icon>close</mat-icon></button>
    </div>

    <mat-dialog-content class="detail-dialog-content">
      <mat-tab-group animationDuration="160ms">
        <mat-tab label="Resumen">
          <div class="detail-grid padded">
            <div class="detail-item"><label>Estado</label><div><span class="status-chip" [ngClass]="statusClass(attention.estadoActual)">{{attention.estadoActual || '-'}}</span></div></div>
            <div class="detail-item"><label>Establecimiento</label><div>{{attention.codigoEstablecimiento || '-'}}</div></div>
            <div class="detail-item"><label>Motivo consulta</label><div>{{attention.motivoConsulta || '-'}}</div></div>
            <div class="detail-item"><label>Categoría</label><div>{{attention.ultimaCategorizacion || attention.primeraCategorizacion || '-'}}</div></div>
            <div class="detail-item"><label>Admisión</label><div>{{attention.fechaAdminision || '-'}} {{attention.horaAdmision || ''}}</div></div>
            <div class="detail-item"><label>Atención médica</label><div>{{attention.fechaAtencion || '-'}} {{attention.horaAtencion || ''}}</div></div>
            <div class="detail-item"><label>Alta</label><div>{{attention.fechaAlta || '-'}} {{attention.horaAlta || ''}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Paciente">
          <div class="detail-grid padded">
            <div class="detail-item"><label>ID Paciente</label><div>{{attention.idPaciente || '-'}}</div></div>
            <div class="detail-item"><label>BD Personas</label><div>{{attention.idBDPersonas || '-'}}</div></div>
            <div class="detail-item"><label>RUN</label><div>{{attention.run || '-'}}-{{attention.dv || ''}}</div></div>
            <div class="detail-item"><label>Sexo</label><div>{{attention.codSexo || '-'}}</div></div>
            <div class="detail-item"><label>Fecha nacimiento</label><div>{{attention.fechaNacimiento || '-'}}</div></div>
            <div class="detail-item"><label>Previsión</label><div>{{attention.prevision || '-'}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Admisión">
          <div class="detail-grid padded">
            <div class="detail-item"><label>Unidad</label><div>{{attention.unidadAtencion || '-'}}</div></div>
            <div class="detail-item"><label>Medio llegada</label><div>{{attention.medioLlegada || '-'}}</div></div>
            <div class="detail-item"><label>Procedencia</label><div>{{attention.procedenciaPaciente || '-'}}</div></div>
            <div class="detail-item"><label>Clasificación consulta</label><div>{{attention.clasificacionConsulta || '-'}}</div></div>
            <div class="detail-item wide"><label>Motivo</label><div>{{attention.motivoConsulta || '-'}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Categorización">
          <div class="detail-grid padded">
            <div class="detail-item"><label>ESI</label><div>{{attention.categorizacionESI || '-'}}</div></div>
            <div class="detail-item"><label>Primera</label><div>{{attention.primeraCategorizacion || '-'}} · {{attention.fechaPrimeraCategorizacion || '-'}} {{attention.horaPrimeraCategorizacion || ''}}</div></div>
            <div class="detail-item"><label>Última</label><div>{{attention.ultimaCategorizacion || '-'}} · {{attention.fechaUltimaCategorizacion || '-'}} {{attention.horaUltimaCategorizacion || ''}}</div></div>
            <div class="detail-item"><label>Número categorizaciones</label><div>{{attention.numCategorizacion || '-'}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Atención clínica">
          <div class="detail-grid padded">
            <div class="detail-item wide"><label>Hipótesis</label><div>{{attention.hipotesisDiagnostico || '-'}}</div></div>
            <div class="detail-item"><label>Código diagnóstico</label><div>{{attention.codigoDiagnistico || '-'}} · {{attention.tipoCodigoDiagnostico || '-'}}</div></div>
            <div class="detail-item wide"><label>Fármacos</label><div>{{attention.indicacionFarmacos || '-'}}</div></div>
            <div class="detail-item wide"><label>Medios diagnóstico</label><div>{{attention.solicitudMediosDiagnostico || '-'}} {{attention.descripcionMediosDiagnostico || ''}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Alta">
          <div class="detail-grid padded">
            <div class="detail-item wide"><label>Diagnóstico final</label><div>{{attention.diagnosticoFinal || '-'}}</div></div>
            <div class="detail-item"><label>Código alta</label><div>{{attention.codigoDiagnosticoAltaMedica || '-'}} · {{attention.tipoCodDiagnosticoAltaMedica || '-'}}</div></div>
            <div class="detail-item"><label>Destino</label><div>{{attention.destinoAlta || '-'}}</div></div>
            <div class="detail-item"><label>Profesional</label><div>{{attention.idProfesionalAlta || '-'}} · {{attention.runProfesional || '-'}}-{{attention.dvProfesional || ''}}</div></div>
          </div>
        </mat-tab>

        <mat-tab label="Eventos">
          <div class="padded">
            <table mat-table [dataSource]="events" class="dense-table">
              <ng-container matColumnDef="fecha"><th mat-header-cell *matHeaderCellDef>Fecha recepción</th><td mat-cell *matCellDef="let e">{{e.fechaRecepcion}}</td></ng-container>
              <ng-container matColumnDef="evento"><th mat-header-cell *matHeaderCellDef>Evento</th><td mat-cell *matCellDef="let e">{{displayTipoEvento(e.tipoEventoInferido)}}</td></ng-container>
              <ng-container matColumnDef="estado"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let e"><span class="status-chip" [ngClass]="statusClass(e.estadoProcesamiento)">{{e.estadoProcesamiento}}</span></td></ng-container>
              <ng-container matColumnDef="json"><th mat-header-cell *matHeaderCellDef>Payload</th><td mat-cell *matCellDef="let e"><button mat-button (click)="openJson(e)"><mat-icon>data_object</mat-icon>Ver JSON</button></td></ng-container>
              <tr mat-header-row *matHeaderRowDef="eventColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: eventColumns;"></tr>
            </table>
          </div>
        </mat-tab>
      </mat-tab-group>
    </mat-dialog-content>
  `
})
class DetailDialogComponent {
  dialog = inject(MatDialog);
  attention: any;
  events: any[] = [];
  eventColumns = ['fecha', 'evento', 'estado', 'json'];

  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {
    this.attention = data.attention;
    this.events = data.events || [];
  }

  statusClass(status: string) { return `status-${status || 'PENDIENTE'}`; }
  displayTipoEvento(value: any) {
    const map: any = { '01_ADMISION': '01 · Admisión', '02_CATEGORIZACION': '02 · Categorización', '03_ATENCION_MEDICA': '03 · Atención médica', '04_ALTA_MEDICA': '04 · Alta médica' };
    return map[value] || value || '-';
  }
  openJson(event: any) { this.dialog.open(JsonDialogComponent, { width: '920px', maxWidth: '95vw', data: event }); }
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSidenavModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatTableModule,
    MatTabsModule,
    MatToolbarModule,
    MatTooltipModule
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav [mode]="isMobile ? 'over' : 'side'" [opened]="!isMobile || mobileMenuOpen" class="sidenav">
        <div class="brand-panel">
          <div class="brand-mark">D</div>
          <div>
            <h1>DAU DSSM</h1>
            <p>Convergente Regional de Urgencia</p>
          </div>
          <button mat-icon-button class="mobile-close" *ngIf="isMobile" (click)="mobileMenuOpen = false"><mat-icon>close</mat-icon></button>
        </div>

        <div class="profile-card" *ngIf="token">
          <span class="profile-label">Usuario conectado</span>
          <strong>{{fullName || username}}</strong>
          <small>{{role}}<ng-container *ngIf="providerName"> · {{providerName}}</ng-container></small>
        </div>

        <nav class="nav" *ngIf="token">
          <button mat-button [class.active]="view==='dashboard'" (click)="go('dashboard'); closeMobileMenu()"><mat-icon>dashboard</mat-icon><span>Dashboard</span></button>
          <button mat-button [class.active]="view==='atenciones'" (click)="go('atenciones'); closeMobileMenu()"><mat-icon>monitor_heart</mat-icon><span>Monitor DAU</span></button>
          <button mat-button [class.active]="view==='eventos'" (click)="go('eventos'); closeMobileMenu()"><mat-icon>receipt_long</mat-icon><span>Eventos</span></button>
          <button mat-button [class.active]="view==='errores'" *ngIf="canAudit()" (click)="go('errores'); closeMobileMenu()"><mat-icon>report_problem</mat-icon><span>Errores</span></button>
          <button mat-button [class.active]="view==='usuarios'" *ngIf="isAdmin()" (click)="go('usuarios'); closeMobileMenu()"><mat-icon>manage_accounts</mat-icon><span>Usuarios</span></button>
          <button mat-button [class.active]="view==='gestion'" (click)="go('gestion'); closeMobileMenu()"><mat-icon>analytics</mat-icon><span>Gestión Red</span></button>
        </nav>

        <div class="sidenav-footer" *ngIf="token">
          <button mat-stroked-button class="logout-button" (click)="logout()"><mat-icon>logout</mat-icon>Salir</button>
        </div>
      </mat-sidenav>

      <mat-sidenav-content class="content">
        <mat-toolbar class="topbar" *ngIf="token">
          <button mat-icon-button class="mobile-menu-button" *ngIf="isMobile" (click)="mobileMenuOpen = true" matTooltip="Abrir menú"><mat-icon>menu</mat-icon></button>
          <div>
            <span class="topbar-title">{{title}}</span>
            <span class="topbar-subtitle">{{subtitle}}</span>
          </div>
          <span class="spacer"></span>
          <span class="env-chip"><mat-icon>dns</mat-icon>{{apiHost}}</span>
          <button mat-icon-button matTooltip="Actualizar vista" (click)="refreshCurrent()"><mat-icon>refresh</mat-icon></button>
        </mat-toolbar>

        <main class="page">
          <section *ngIf="!token" class="login-layout">
            <mat-card class="login-card">
              <mat-card-header>
                <mat-card-title>Ingreso monitoreo regional</mat-card-title>
                <mat-card-subtitle>Acceso seguro con usuario institucional o técnico de integración.</mat-card-subtitle>
              </mat-card-header>
              <mat-card-content>
                <mat-form-field appearance="outline" class="full"><mat-label>Usuario</mat-label><input matInput [(ngModel)]="username" autocomplete="username"></mat-form-field>
                <mat-form-field appearance="outline" class="full"><mat-label>Clave</mat-label><input matInput [(ngModel)]="password" type="password" autocomplete="current-password" (keyup.enter)="login()"></mat-form-field>
                <p class="error-text" *ngIf="error"><mat-icon>error</mat-icon>{{error}}</p>
              </mat-card-content>
              <mat-card-actions align="end">
                <button mat-flat-button color="primary" (click)="login()"><mat-icon>login</mat-icon>Ingresar</button>
              </mat-card-actions>
            </mat-card>
          </section>

          <section *ngIf="token && view==='dashboard'" class="page-section">
            <div class="section-header"><div><h2>Dashboard operacional</h2><p>Resumen regional de atenciones y eventos DAU recibidos.</p></div></div>
            <div class="kpi-grid">
              <mat-card class="kpi-card primary"><mat-card-content><mat-icon>clinical_notes</mat-icon><strong>{{dash?.totalAtenciones || 0}}</strong><span>Total atenciones</span></mat-card-content></mat-card>
              <mat-card class="kpi-card"><mat-card-content><mat-icon>how_to_reg</mat-icon><strong>{{dash?.admision || 0}}</strong><span>Admisión</span></mat-card-content></mat-card>
              <mat-card class="kpi-card"><mat-card-content><mat-icon>assignment_turned_in</mat-icon><strong>{{dash?.categorizadas || 0}}</strong><span>Categorizadas</span></mat-card-content></mat-card>
              <mat-card class="kpi-card"><mat-card-content><mat-icon>local_hospital</mat-icon><strong>{{dash?.atencionMedica || 0}}</strong><span>Atención médica</span></mat-card-content></mat-card>
              <mat-card class="kpi-card success"><mat-card-content><mat-icon>check_circle</mat-icon><strong>{{dash?.altaMedica || 0}}</strong><span>Alta médica</span></mat-card-content></mat-card>
              <mat-card class="kpi-card"><mat-card-content><mat-icon>receipt_long</mat-icon><strong>{{dash?.totalEventos || 0}}</strong><span>Eventos recibidos</span></mat-card-content></mat-card>
              <mat-card class="kpi-card danger"><mat-card-content><mat-icon>error</mat-icon><strong>{{dash?.eventosConError || 0}}</strong><span>Errores</span></mat-card-content></mat-card>
            </div>
          </section>

          <section *ngIf="token && view==='atenciones'" class="page-section">
            <div class="section-header"><div><h2>Monitor DAU</h2><p>Búsqueda y seguimiento de atenciones consolidadas.</p></div></div>
            <mat-card class="filter-card">
              <div class="filter-grid">
                <mat-form-field appearance="outline"><mat-label>Buscar</mat-label><input matInput placeholder="DAU, RUN, paciente, motivo" [(ngModel)]="attF.q" (keyup.enter)="loadAtenciones(0)"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Estado</mat-label><mat-select [(ngModel)]="attF.estado"><mat-option value="">Todos</mat-option><mat-option *ngFor="let e of estados" [value]="e">{{e}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Establecimiento</mat-label><input matInput type="number" [(ngModel)]="attF.establecimiento"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Categoría</mat-label><input matInput placeholder="01, 02, 03..." [(ngModel)]="attF.categoria"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha desde</mat-label><input matInput placeholder="DDMMAAAA" [(ngModel)]="attF.fechaDesde"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha hasta</mat-label><input matInput placeholder="DDMMAAAA" [(ngModel)]="attF.fechaHasta"></mat-form-field>
                <div class="filter-actions"><button mat-flat-button color="primary" (click)="loadAtenciones(0)"><mat-icon>search</mat-icon>Buscar</button><button mat-stroked-button (click)="clearAtenciones()">Limpiar</button></div>
              </div>
            </mat-card>
            <mat-card class="table-card">
              <table mat-table [dataSource]="atenciones">
                <ng-container matColumnDef="idDau"><th mat-header-cell *matHeaderCellDef>ID DAU</th><td mat-cell *matCellDef="let a"><strong>{{a.idDau}}</strong><small>{{a.idAtencion}}</small></td></ng-container>
                <ng-container matColumnDef="establecimiento"><th mat-header-cell *matHeaderCellDef>Estab.</th><td mat-cell *matCellDef="let a">{{displayEstablecimiento(a.codigoEstablecimiento)}}</td></ng-container>
                <ng-container matColumnDef="admision"><th mat-header-cell *matHeaderCellDef>Admisión</th><td mat-cell *matCellDef="let a">{{a.fechaAdminision}}<small>{{a.horaAdmision}}</small></td></ng-container>
                <ng-container matColumnDef="motivo"><th mat-header-cell *matHeaderCellDef>Motivo</th><td mat-cell *matCellDef="let a" class="wide-cell">{{a.motivoConsulta}}</td></ng-container>
                <ng-container matColumnDef="cat"><th mat-header-cell *matHeaderCellDef>Cat.</th><td mat-cell *matCellDef="let a">{{displayCategoria(a.ultimaCategorizacion || a.primeraCategorizacion)}}</td></ng-container>
                <ng-container matColumnDef="estado"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let a"><span class="status-chip" [ngClass]="statusClass(a.estadoActual)">{{displayEstado(a.estadoActual)}}</span></td></ng-container>
                <ng-container matColumnDef="acciones"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let a"><button mat-stroked-button class="table-action" (click)="openDetalle(a)"><mat-icon>visibility</mat-icon>Ver</button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="attCols"></tr><tr mat-row *matRowDef="let row; columns: attCols;"></tr>
              </table>
              <mat-paginator [length]="attTotal" [pageIndex]="attF.page" [pageSize]="attF.size" [pageSizeOptions]="[10,20,50,100]" (page)="onAttPage($event)"></mat-paginator>
            </mat-card>
          </section>

          <section *ngIf="token && view==='eventos'" class="page-section">
            <div class="section-header"><div><h2>Bitácora de eventos recibidos</h2><p>Histórico técnico con payload original, hash, estado e inferencia de evento.</p></div></div>
            <mat-card class="filter-card">
              <div class="filter-grid">
                <mat-form-field appearance="outline"><mat-label>Buscar</mat-label><input matInput [(ngModel)]="evtF.q" (keyup.enter)="loadEventos(0)"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>ID DAU</mat-label><input matInput [(ngModel)]="evtF.idDau"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Tipo evento</mat-label><mat-select [(ngModel)]="evtF.tipoEvento"><mat-option value="">Todos</mat-option><mat-option *ngFor="let t of tiposEvento" [value]="t">{{t}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Estado</mat-label><mat-select [(ngModel)]="evtF.estado"><mat-option value="">Todos</mat-option><mat-option value="PROCESADO">PROCESADO</mat-option><mat-option value="ERROR">ERROR</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha desde</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="evtF.fechaDesde"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha hasta</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="evtF.fechaHasta"></mat-form-field>
                <div class="filter-actions"><button mat-flat-button color="primary" (click)="loadEventos(0)"><mat-icon>search</mat-icon>Buscar</button><button mat-stroked-button (click)="clearEventos()">Limpiar</button></div>
              </div>
            </mat-card>
            <mat-card class="table-card">
              <table mat-table [dataSource]="eventos">
                <ng-container matColumnDef="fecha"><th mat-header-cell *matHeaderCellDef>Fecha</th><td mat-cell *matCellDef="let e">{{e.fechaRecepcion}}</td></ng-container>
                <ng-container matColumnDef="idDau"><th mat-header-cell *matHeaderCellDef>ID DAU</th><td mat-cell *matCellDef="let e"><strong>{{e.idDau}}</strong><small>{{e.idAtencion}}</small></td></ng-container>
                <ng-container matColumnDef="evento"><th mat-header-cell *matHeaderCellDef>Evento</th><td mat-cell *matCellDef="let e">{{displayTipoEvento(e.tipoEventoInferido)}}</td></ng-container>
                <ng-container matColumnDef="estado"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let e"><span class="status-chip" [ngClass]="statusClass(e.estadoProcesamiento)">{{displayEstadoProcesamiento(e.estadoProcesamiento)}}</span></td></ng-container>
                <ng-container matColumnDef="hash"><th mat-header-cell *matHeaderCellDef>Hash</th><td mat-cell *matCellDef="let e" class="hash-cell">{{e.hashPayload}}</td></ng-container>
                <ng-container matColumnDef="archivo"><th mat-header-cell *matHeaderCellDef>Archivo</th><td mat-cell *matCellDef="let e">{{cleanFile(e.nombreArchivo)}}</td></ng-container>
                <ng-container matColumnDef="acciones"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let e"><button mat-stroked-button class="table-action" (click)="openJson(e)"><mat-icon>data_object</mat-icon>JSON</button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="eventCols"></tr><tr mat-row *matRowDef="let row; columns: eventCols;"></tr>
              </table>
              <mat-paginator [length]="evtTotal" [pageIndex]="evtF.page" [pageSize]="evtF.size" [pageSizeOptions]="[10,20,50,100]" (page)="onEvtPage($event)"></mat-paginator>
            </mat-card>
          </section>

          <section *ngIf="token && view==='errores'" class="page-section">
            <div class="section-header"><div><h2>Errores de integración</h2><p>Eventos rechazados o procesados con error técnico/funcional.</p></div></div>
            <mat-card class="filter-card">
              <div class="filter-grid">
                <mat-form-field appearance="outline"><mat-label>Buscar</mat-label><input matInput [(ngModel)]="errF.q" (keyup.enter)="loadErrores(0)"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>ID DAU</mat-label><input matInput [(ngModel)]="errF.idDau"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Desde</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="errF.fechaDesde"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Hasta</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="errF.fechaHasta"></mat-form-field>
                <div class="filter-actions"><button mat-flat-button color="primary" (click)="loadErrores(0)"><mat-icon>search</mat-icon>Buscar</button><button mat-stroked-button (click)="clearErrores()">Limpiar</button></div>
              </div>
            </mat-card>
            <mat-card class="table-card">
              <table mat-table [dataSource]="errores">
                <ng-container matColumnDef="fecha"><th mat-header-cell *matHeaderCellDef>Fecha</th><td mat-cell *matCellDef="let e">{{e.fechaRecepcion}}</td></ng-container>
                <ng-container matColumnDef="idDau"><th mat-header-cell *matHeaderCellDef>ID DAU</th><td mat-cell *matCellDef="let e">{{e.idDau}}</td></ng-container>
                <ng-container matColumnDef="error"><th mat-header-cell *matHeaderCellDef>Error</th><td mat-cell *matCellDef="let e">{{e.mensajeError}}</td></ng-container>
                <ng-container matColumnDef="archivo"><th mat-header-cell *matHeaderCellDef>Archivo</th><td mat-cell *matCellDef="let e">{{cleanFile(e.nombreArchivo)}}</td></ng-container>
                <ng-container matColumnDef="acciones"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let e"><button mat-stroked-button class="table-action" (click)="openJson(e)"><mat-icon>data_object</mat-icon>JSON</button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="errorCols"></tr><tr mat-row *matRowDef="let row; columns: errorCols;"></tr>
              </table>
              <mat-paginator [length]="errTotal" [pageIndex]="errF.page" [pageSize]="errF.size" [pageSizeOptions]="[10,20,50]" (page)="onErrPage($event)"></mat-paginator>
            </mat-card>
          </section>


          <section *ngIf="token && view==='gestion'" class="page-section">
            <div class="section-header">
              <div>
                <h2>Gestión Red de Urgencia</h2>
                <p>Reportería regional avanzada para consultas en la red y tiempos de espera.</p>
              </div>
              <button mat-flat-button color="primary" (click)="exportGestionCsv()"><mat-icon>download</mat-icon>Exportar CSV</button>
            </div>

            <mat-card class="filter-card gestion-card">
              <div class="gestion-module-switch">
                <button mat-stroked-button [class.active]="gestionModulo==='consultas'" (click)="setGestionModulo('consultas')">
                  <mat-icon>hub</mat-icon>Consultas en la Red
                </button>
                <button mat-stroked-button [class.active]="gestionModulo==='tiempos'" (click)="setGestionModulo('tiempos')">
                  <mat-icon>timer</mat-icon>Tiempos de Espera
                </button>
              </div>

              <mat-tab-group *ngIf="gestionModulo==='consultas'" [(selectedIndex)]="gestionTab" (selectedIndexChange)="onGestionTabChange($event)">
                <mat-tab label="Demanda"></mat-tab>
                <mat-tab label="Atenciones"></mat-tab>
              </mat-tab-group>
              <mat-tab-group *ngIf="gestionModulo==='tiempos'" [(selectedIndex)]="gestionTab" (selectedIndexChange)="onGestionTabChange($event)">
                <mat-tab label="Categorización"></mat-tab>
                <mat-tab label="Atención médica"></mat-tab>
                <mat-tab label="Egreso total"></mat-tab>
              </mat-tab-group>

              <div class="gestion-title-row">
                <div>
                  <h3>{{gestionTitle}}</h3>
                  <p>{{gestionSubtitle}}</p>
                </div>
                <span class="module-badge">{{gestionModulo === 'consultas' ? 'Consultas en la Red' : 'Tiempos de Espera'}}</span>
              </div>

              <div class="filter-grid advanced-grid">
                <mat-form-field appearance="outline"><mat-label>Dispositivo consulta</mat-label><mat-select multiple [(ngModel)]="gestionF.dispositivos"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.dispositivos?.length; else empty_dispositivos"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.dispositivos, dispositivos)">{{label}}</span></span><ng-template #empty_dispositivos>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let d of dispositivos" [value]="d.value">{{d.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Establecimiento</mat-label><mat-select multiple [(ngModel)]="gestionF.establecimientos"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.establecimientos?.length; else empty_establecimientos"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.establecimientos, establecimientos)">{{label}}</span></span><ng-template #empty_establecimientos>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let e of establecimientos" [value]="e.value">{{e.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Sexo</mat-label><mat-select multiple [(ngModel)]="gestionF.sexos"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.sexos?.length; else empty_sexos"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.sexos, sexos)">{{label}}</span></span><ng-template #empty_sexos>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let s of sexos" [value]="s.value">{{s.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Categorización</mat-label><mat-select multiple [(ngModel)]="gestionF.categorias"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.categorias?.length; else empty_categorias"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.categorias, categorias)">{{label}}</span></span><ng-template #empty_categorias>Todas</ng-template></mat-select-trigger><mat-option *ngFor="let c of categorias" [value]="c.value">{{c.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Origen</mat-label><mat-select multiple [(ngModel)]="gestionF.origenes"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.origenes?.length; else empty_origenes"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.origenes, origenes)">{{label}}</span></span><ng-template #empty_origenes>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let o of origenes" [value]="o.value">{{o.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Grupo diagnóstico</mat-label><mat-select multiple [(ngModel)]="gestionF.gruposDiagnostico"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.gruposDiagnostico?.length; else empty_gruposDiagnostico"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.gruposDiagnostico, gruposDiagnostico)">{{label}}</span></span><ng-template #empty_gruposDiagnostico>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let g of gruposDiagnostico" [value]="g.value">{{g.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Tramo horario</mat-label><mat-select multiple [(ngModel)]="gestionF.tramosHorarios"><mat-select-trigger><span class="select-trigger-chips" *ngIf="gestionF.tramosHorarios?.length; else empty_tramosHorarios"><span class="trigger-chip" *ngFor="let label of multiLabels(gestionF.tramosHorarios, tramosHorarios)">{{label}}</span></span><ng-template #empty_tramosHorarios>Todos</ng-template></mat-select-trigger><mat-option *ngFor="let t of tramosHorarios" [value]="t.value">{{t.label}}</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Agrupar por</mat-label><mat-select [(ngModel)]="gestionF.agruparPor"><mat-option value="DIA">Día</mat-option><mat-option value="SEMANA">Semana estadística</mat-option><mat-option value="MES">Mes</mat-option></mat-select></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Edad desde</mat-label><input matInput type="number" [(ngModel)]="gestionF.edadDesde"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Edad hasta</mat-label><input matInput type="number" [(ngModel)]="gestionF.edadHasta"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha desde</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="gestionF.fechaDesde"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Fecha hasta</mat-label><input matInput placeholder="AAAA-MM-DD" [(ngModel)]="gestionF.fechaHasta"></mat-form-field>
                <div class="filter-actions"><button mat-flat-button color="primary" (click)="loadGestion(0)"><mat-icon>search</mat-icon>Buscar</button><button mat-stroked-button (click)="clearGestion()">Limpiar</button></div>
              </div>

              <div class="selected-filter-panel" *ngIf="selectedGestionFilters.length">
                <span class="selected-filter-title">Filtros aplicados</span>
                <mat-chip-set class="selected-filters">
                  <mat-chip *ngFor="let chip of selectedGestionFilters">{{chip}}</mat-chip>
                </mat-chip-set>
              </div>
            </mat-card>

            <div class="kpi-grid gestion-kpis">
              <mat-card class="kpi-card" *ngFor="let k of gestionKpis; let first = first" [class.primary]="first">
                <mat-card-content><mat-icon>{{k.icon}}</mat-icon><strong>{{k.value}}</strong><span>{{k.label}}</span></mat-card-content>
              </mat-card>
            </div>

            <mat-card class="chart-card executive-main-chart" *ngIf="gestionSeries?.length">
              <div class="chart-header">
                <div><h3>Serie agrupada</h3><p>{{gestionF.agruparPor === 'MES' ? 'Distribución mensual' : gestionF.agruparPor === 'SEMANA' ? 'Distribución por semana estadística' : 'Distribución diaria'}}</p></div>
                <span>{{gestionSeries.length}} periodos</span>
              </div>
              <div class="bar-list">
                <div class="bar-row executive" *ngFor="let s of gestionSeries">
                  <label>{{s.periodo}}</label>
                  <div class="bar-track"><div class="bar-fill" [style.width.%]="seriesWidth(s)"></div></div>
                  <strong>{{s.total}}</strong>
                </div>
              </div>
            </mat-card>

            <div class="executive-chart-grid" *ngIf="executiveCharts.length">
              <mat-card class="mini-chart-card" *ngFor="let chart of executiveCharts">
                <div class="mini-chart-header">
                  <div>
                    <h3>{{chart.title}}</h3>
                    <p>{{chart.subtitle}}</p>
                  </div>
                  <mat-icon>{{chart.icon}}</mat-icon>
                </div>
                <div class="donut-summary" *ngIf="chart.total">
                  <div class="donut-number">{{chart.total}}</div>
                  <span>registros</span>
                </div>
                <div class="bar-list compact">
                  <div class="bar-row mini" *ngFor="let item of chart.items">
                    <label>{{item.label}}</label>
                    <div class="bar-track"><div class="bar-fill" [style.width.%]="item.width"></div></div>
                    <strong>{{item.total}}</strong>
                  </div>
                </div>
              </mat-card>
            </div>

            <mat-card class="table-card">
              <table mat-table [dataSource]="gestionRows">
                <ng-container matColumnDef="idDau"><th mat-header-cell *matHeaderCellDef>ID DAU</th><td mat-cell *matCellDef="let r"><strong>{{r.idDau}}</strong><small>{{r.idAtencion}}</small></td></ng-container>
                <ng-container matColumnDef="establecimiento"><th mat-header-cell *matHeaderCellDef>Estab.</th><td mat-cell *matCellDef="let r">{{displayEstablecimiento(r.establecimiento)}}</td></ng-container>
                <ng-container matColumnDef="periodo"><th mat-header-cell *matHeaderCellDef>Periodo</th><td mat-cell *matCellDef="let r">{{r.periodo}}</td></ng-container>
                <ng-container matColumnDef="motivo"><th mat-header-cell *matHeaderCellDef>Motivo</th><td mat-cell *matCellDef="let r" class="wide-cell">{{r.motivo || '-'}}</td></ng-container>
                <ng-container matColumnDef="categoria"><th mat-header-cell *matHeaderCellDef>Cat.</th><td mat-cell *matCellDef="let r">{{displayCategoria(r.categoria)}}</td></ng-container>
                <ng-container matColumnDef="estado"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let r"><span class="status-chip" [ngClass]="statusClass(r.estado)">{{r.estado || '-'}}</span></td></ng-container>
                <ng-container matColumnDef="minutos"><th mat-header-cell *matHeaderCellDef>{{minColumnLabel}}</th><td mat-cell *matCellDef="let r">{{r.minutos_espera ?? '-'}}</td></ng-container>
                <tr mat-header-row *matHeaderRowDef="gestionCols"></tr><tr mat-row *matRowDef="let row; columns: gestionCols;"></tr>
              </table>
              <mat-paginator [length]="gestionTotal" [pageIndex]="gestionF.page" [pageSize]="gestionF.size" [pageSizeOptions]="[10,20,50,100]" (page)="onGestionPage($event)"></mat-paginator>
            </mat-card>
          </section>

          <section *ngIf="token && view==='usuarios'" class="page-section">
            <div class="section-header"><div><h2>Administración de usuarios</h2><p>Creación y control de cuentas para monitoreo, auditoría e integraciones.</p></div></div>
            <mat-card class="filter-card">
              <h3>Crear usuario</h3>
              <div class="form-grid">
                <mat-form-field appearance="outline"><mat-label>Username</mat-label><input matInput [(ngModel)]="newUser.username"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Clave</mat-label><input matInput type="password" [(ngModel)]="newUser.password"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Nombre completo</mat-label><input matInput [(ngModel)]="newUser.fullName"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Email</mat-label><input matInput [(ngModel)]="newUser.email"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Proveedor / origen</mat-label><input matInput [(ngModel)]="newUser.providerName"></mat-form-field>
                <mat-form-field appearance="outline"><mat-label>Rol</mat-label><mat-select [(ngModel)]="newUser.role"><mat-option *ngFor="let r of roles" [value]="r">{{r}}</mat-option></mat-select></mat-form-field>
                <mat-slide-toggle [(ngModel)]="newUser.enabled">Habilitado</mat-slide-toggle>
                <div class="filter-actions"><button mat-flat-button color="primary" (click)="createUser()"><mat-icon>person_add</mat-icon>Crear usuario</button></div>
              </div>
            </mat-card>
            <mat-card class="table-card">
              <table mat-table [dataSource]="usuarios">
                <ng-container matColumnDef="username"><th mat-header-cell *matHeaderCellDef>Usuario</th><td mat-cell *matCellDef="let u"><strong>{{u.username}}</strong><small>{{u.email}}</small></td></ng-container>
                <ng-container matColumnDef="fullName"><th mat-header-cell *matHeaderCellDef>Nombre</th><td mat-cell *matCellDef="let u">{{u.fullName}}</td></ng-container>
                <ng-container matColumnDef="providerName"><th mat-header-cell *matHeaderCellDef>Proveedor</th><td mat-cell *matCellDef="let u">{{u.providerName || '-'}}</td></ng-container>
                <ng-container matColumnDef="role"><th mat-header-cell *matHeaderCellDef>Rol</th><td mat-cell *matCellDef="let u"><span class="role-chip">{{u.role}}</span></td></ng-container>
                <ng-container matColumnDef="enabled"><th mat-header-cell *matHeaderCellDef>Estado</th><td mat-cell *matCellDef="let u">{{u.enabled ? 'Habilitado' : 'Deshabilitado'}}</td></ng-container>
                <ng-container matColumnDef="lastLogin"><th mat-header-cell *matHeaderCellDef>Último acceso</th><td mat-cell *matCellDef="let u">{{u.lastLogin || '-'}}</td></ng-container>
                <ng-container matColumnDef="acciones"><th mat-header-cell *matHeaderCellDef></th><td mat-cell *matCellDef="let u"><button mat-stroked-button class="table-action" (click)="editUser(u)"><mat-icon>edit</mat-icon>Editar</button><button mat-stroked-button class="table-action" (click)="toggleUser(u)">{{u.enabled ? 'Deshabilitar' : 'Habilitar'}}</button></td></ng-container>
                <tr mat-header-row *matHeaderRowDef="userCols"></tr><tr mat-row *matRowDef="let row; columns: userCols;"></tr>
              </table>
              <mat-paginator [length]="userTotal" [pageIndex]="userPage" [pageSize]="userSize" [pageSizeOptions]="[10,20,50]" (page)="onUserPage($event)"></mat-paginator>
            </mat-card>
          </section>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `
})
export class AppComponent {
  http = inject(HttpClient);
  snack = inject(MatSnackBar);
  dialog = inject(MatDialog);

  token = localStorage.getItem('token');
  username = localStorage.getItem('username') || 'admin';
  password = this.token ? '' : 'admin123';
  fullName = localStorage.getItem('fullName') || '';
  role = localStorage.getItem('role') || '';
  providerName = localStorage.getItem('providerName') || '';
  error = '';
  view: 'dashboard'|'atenciones'|'eventos'|'errores'|'usuarios'|'gestion' = 'dashboard';
  loading = false;
  isMobile = window.innerWidth <= 900;
  mobileMenuOpen = false;

  @HostListener('window:resize')
  onResize() {
    this.isMobile = window.innerWidth <= 900;
    if (!this.isMobile) this.mobileMenuOpen = false;
  }
  closeMobileMenu() { if (this.isMobile) this.mobileMenuOpen = false; }

  dash: any;
  estados = ['ADMISION', 'CATEGORIZADA', 'ATENCION_MEDICA', 'ALTA_MEDICA'];
  tiposEvento = ['01_ADMISION', '02_CATEGORIZACION', '03_ATENCION_MEDICA', '04_ALTA_MEDICA'];
  roles = ['ADMIN', 'INTEGRADOR', 'GESTOR_RED', 'VISUALIZADOR', 'AUDITOR'];

  attCols = ['idDau', 'establecimiento', 'admision', 'motivo', 'cat', 'estado', 'acciones'];
  eventCols = ['fecha', 'idDau', 'evento', 'estado', 'hash', 'archivo', 'acciones'];
  errorCols = ['fecha', 'idDau', 'error', 'archivo', 'acciones'];
  userCols = ['username', 'fullName', 'providerName', 'role', 'enabled', 'lastLogin', 'acciones'];
  gestionCols = ['idDau', 'establecimiento', 'periodo', 'motivo', 'categoria', 'estado', 'minutos'];

  atenciones: any[] = []; attTotal = 0;
  eventos: any[] = []; evtTotal = 0;
  errores: any[] = []; errTotal = 0;
  usuarios: any[] = []; userTotal = 0; userPage = 0; userSize = 20;
  gestionModulo: 'consultas'|'tiempos' = 'consultas'; gestionTab = 0; gestionRows: any[] = []; gestionResumen: any = {}; gestionSeries: any[] = []; gestionDistribuciones: any = {}; gestionTotal = 0;
  dispositivos = [{value: 'HCM', label: 'Hospital Clínico'}, {value: 'RAYEN', label: 'Rayen'}, {value: 'APS', label: 'APS'}, {value: 'HOSPITAL_COMUNITARIO', label: 'Hospital comunitario'}];
  establecimientos = [{value: 126100, label: '126100 - HCM'}, {value: 121105, label: '121105 - Puerto Natales'}, {value: 121102, label: '121102 - Porvenir'}, {value: 121108, label: '121108 - Puerto Williams'}];
  origenes = [{value: 'UEH', label: 'UEH'}, {value: 'UGO', label: 'UGO'}, {value: 'APS', label: 'APS'}, {value: 'SAMU', label: 'SAMU'}, {value: 'ESPONTANEO', label: 'Espontáneo'}];
  sexos = [{value: '01', label: 'Masculino'}, {value: '02', label: 'Femenino'}, {value: '99', label: 'No informado'}];
  categorias = [{value: '01', label: 'C1 · Riesgo vital'}, {value: '02', label: 'C2 · Emergencia'}, {value: '03', label: 'C3 · Urgencia'}, {value: '04', label: 'C4 · Menor urgencia'}, {value: '05', label: 'C5 · No urgencia'}];
  gruposDiagnostico = [{value: 'RESPIRATORIO', label: 'Respiratorio'}, {value: 'CARDIOVASCULAR', label: 'Cardiovascular'}, {value: 'SALUD_MENTAL', label: 'Salud mental'}, {value: 'TRAUMA', label: 'Trauma'}, {value: 'DIGESTIVO', label: 'Digestivo'}, {value: 'OTRO', label: 'Otro'}];
  tramosHorarios = [{value: 'AM', label: 'AM'}, {value: 'PM', label: 'PM'}, {value: 'NOCHE', label: 'Noche'}, {value: 'MADRUGADA', label: 'Madrugada'}];

  attF: any = { q: '', estado: '', establecimiento: '', categoria: '', fechaDesde: '', fechaHasta: '', page: 0, size: 20 };
  evtF: any = { q: '', idDau: '', tipoEvento: '', estado: '', fechaDesde: '', fechaHasta: '', page: 0, size: 20 };
  errF: any = { q: '', idDau: '', fechaDesde: '', fechaHasta: '', page: 0, size: 20 };
  newUser: any = { username: '', password: '', fullName: '', email: '', providerName: '', role: 'VISUALIZADOR', enabled: true };
  gestionF: any = { dispositivos: [], establecimientos: [], sexos: [], categorias: [], origenes: [], gruposDiagnostico: [], tramosHorarios: [], edadDesde: '', edadHasta: '', fechaDesde: '', fechaHasta: '', agruparPor: 'DIA', page: 0, size: 20 };

  get title() {
    return ({ dashboard: 'Dashboard operacional', atenciones: 'Monitor DAU', eventos: 'Bitácora de eventos', errores: 'Errores de integración', usuarios: 'Administración de usuarios', gestion: 'Gestión Red de Urgencia' } as any)[this.view];
  }
  get subtitle() {
    return ({ dashboard: 'Resumen regional', atenciones: 'Atenciones consolidadas', eventos: 'Trazabilidad técnica', errores: 'Control de rechazos', usuarios: 'Cuentas y roles', gestion: 'Reportería avanzada' } as any)[this.view];
  }
  get apiHost() { return API.replace('/api', '').replace('https://', '').replace('http://', ''); }

  ngOnInit() { if (this.token) this.loadDashboard(); }
  isAdmin() { return this.role === 'ADMIN'; }
  canAudit() { return this.role === 'ADMIN' || this.role === 'AUDITOR'; }
  statusClass(status: string) { return `status-${status || 'PENDIENTE'}`; }
  cleanFile(file: string) { return !file || file === 'string' ? 'Sin archivo informado' : file; }
  optionLabel(options: any[], value: any) { return options.find(o => String(o.value) === String(value))?.label || value || '-'; }
  multiTrigger(values: any[], options: any[], fallback: string) {
    if (!values?.length) return fallback;
    const first = this.optionLabel(options, values[0]);
    return values.length === 1 ? first : `${first} +${values.length - 1}`;
  }
  multiLabels(values: any[], options: any[]) {
    if (!values?.length) return [];
    const labels = values.slice(0, 2).map(v => this.optionLabel(options, v));
    if (values.length > 2) labels.push(`+${values.length - 2}`);
    return labels;
  }
  displayEstablecimiento(value: any) { return this.optionLabel(this.establecimientos, value); }
  displayCategoria(value: any) { return value ? this.optionLabel(this.categorias, value) : '-'; }
  displayEstado(value: any) {
    const map: any = { ADMISION: 'Admisión', CATEGORIZADA: 'Categorizada', ATENCION_MEDICA: 'Atención médica', ALTA_MEDICA: 'Alta médica', ERROR: 'Error', PROCESADO: 'Procesado', PENDIENTE: 'Pendiente' };
    return map[value] || value || '-';
  }
  displayEstadoProcesamiento(value: any) { return this.displayEstado(value); }
  displayTipoEvento(value: any) {
    const map: any = { '01_ADMISION': '01 · Admisión', '02_CATEGORIZACION': '02 · Categorización', '03_ATENCION_MEDICA': '03 · Atención médica', '04_ALTA_MEDICA': '04 · Alta médica' };
    return map[value] || value || '-';
  }

  login() {
    this.error = '';
    this.http.post<any>(`${API}/auth/login`, { username: this.username, password: this.password }).subscribe({
      next: r => {
        const d = r.data;
        localStorage.setItem('token', d.token);
        localStorage.setItem('username', d.username);
        localStorage.setItem('fullName', d.fullName || '');
        localStorage.setItem('role', d.role || '');
        localStorage.setItem('providerName', d.providerName || '');
        this.token = d.token; this.username = d.username; this.fullName = d.fullName || ''; this.role = d.role || ''; this.providerName = d.providerName || '';
        this.go('dashboard'); this.toast('Autenticado correctamente');
      },
      error: () => this.error = 'Credenciales no válidas o servicio no disponible.'
    });
  }
  logout() { localStorage.clear(); this.token = null; this.password = ''; this.view = 'dashboard'; }
  go(v: any) { this.view = v; this.refreshCurrent(); }
  refreshCurrent() { if (this.view === 'dashboard') this.loadDashboard(); if (this.view === 'atenciones') this.loadAtenciones(); if (this.view === 'eventos') this.loadEventos(); if (this.view === 'errores') this.loadErrores(); if (this.view === 'usuarios') this.loadUsers(); if (this.view === 'gestion') this.loadGestion(); }
  toast(message: string) { this.snack.open(message, 'OK', { duration: 2600 }); }

  loadDashboard() { this.http.get<any>(`${API}/dau/dashboard`).subscribe(r => this.dash = r.data); }

  loadAtenciones(page = this.attF.page) {
    this.attF.page = page;
    let params = appendParams(new HttpParams(), { ...this.attF });
    this.http.get<any>(`${API}/dau/atenciones`, { params }).subscribe(r => { const p = pageData(r); this.atenciones = p.rows; this.attTotal = p.total; });
  }
  onAttPage(e: PageEvent) { this.attF.page = e.pageIndex; this.attF.size = e.pageSize; this.loadAtenciones(); }
  clearAtenciones() { this.attF = { q: '', estado: '', establecimiento: '', categoria: '', fechaDesde: '', fechaHasta: '', page: 0, size: this.attF.size || 20 }; this.loadAtenciones(0); }

  openDetalle(row: any) {
    this.http.get<any>(`${API}/dau/atenciones/${row.idDau}/${row.idAtencion}`).subscribe(att => {
      this.http.get<any>(`${API}/dau/atenciones/${row.idDau}/${row.idAtencion}/eventos`, { params: new HttpParams().set('page', 0).set('size', 100) }).subscribe(ev => {
        this.dialog.open(DetailDialogComponent, { width: '1180px', maxWidth: '96vw', data: { attention: att.data, events: pageData(ev).rows } });
      });
    });
  }

  loadEventos(page = this.evtF.page) {
    this.evtF.page = page;
    let params = appendParams(new HttpParams(), { ...this.evtF });
    this.http.get<any>(`${API}/dau/eventos`, { params }).subscribe(r => { const p = pageData(r); this.eventos = p.rows; this.evtTotal = p.total; });
  }
  onEvtPage(e: PageEvent) { this.evtF.page = e.pageIndex; this.evtF.size = e.pageSize; this.loadEventos(); }
  clearEventos() { this.evtF = { q: '', idDau: '', tipoEvento: '', estado: '', fechaDesde: '', fechaHasta: '', page: 0, size: this.evtF.size || 20 }; this.loadEventos(0); }
  openJson(event: any) {
    this.http.get<any>(`${API}/dau/eventos/${event.id}`).subscribe(r => this.dialog.open(JsonDialogComponent, { width: '920px', maxWidth: '95vw', data: r.data }));
  }

  loadErrores(page = this.errF.page) {
    this.errF.page = page;
    let params = appendParams(new HttpParams(), { ...this.errF });
    this.http.get<any>(`${API}/dau/errores`, { params }).subscribe(r => { const p = pageData(r); this.errores = p.rows; this.errTotal = p.total; });
  }
  onErrPage(e: PageEvent) { this.errF.page = e.pageIndex; this.errF.size = e.pageSize; this.loadErrores(); }
  clearErrores() { this.errF = { q: '', idDau: '', fechaDesde: '', fechaHasta: '', page: 0, size: this.errF.size || 20 }; this.loadErrores(0); }


  setGestionModulo(modulo: 'consultas'|'tiempos') { this.gestionModulo = modulo; this.gestionTab = 0; this.loadGestion(0); }
  get gestionTitle() {
    if (this.gestionModulo === 'consultas') return ['Consultas en la Red · Demanda', 'Consultas en la Red · Atenciones'][this.gestionTab];
    return ['Tiempos de Espera · Categorización', 'Tiempos de Espera · Atención médica', 'Tiempos de Espera · Egreso total'][this.gestionTab];
  }
  get gestionSubtitle() {
    if (this.gestionModulo === 'consultas') return ['Total de DAU generados según filtros seleccionados.', 'Atenciones finalizadas con alta/egreso.'][this.gestionTab];
    return ['Tiempo entre admisión y primera categorización.', 'Tiempo entre admisión y atención médica.', 'Tiempo total entre admisión y alta/egreso.'][this.gestionTab];
  }
  get isTiempoReporte() { return this.gestionModulo === 'tiempos'; }
  get minColumnLabel() { return this.isTiempoReporte ? 'Min.' : 'Min.'; }
  gestionEndpoint() {
    if (this.gestionModulo === 'consultas') return ['/reportes/consultas-red/demanda/buscar','/reportes/consultas-red/atenciones/buscar'][this.gestionTab];
    return ['/reportes/tiempos-espera/categorizacion/buscar','/reportes/tiempos-espera/atencion/buscar','/reportes/tiempos-espera/egreso/buscar'][this.gestionTab];
  }
  gestionExportPath() {
    if (this.gestionModulo === 'consultas') return [['consultas-red','demanda'],['consultas-red','atenciones']][this.gestionTab];
    return [['tiempos-espera','categorizacion'],['tiempos-espera','atencion'],['tiempos-espera','egreso']][this.gestionTab];
  }
  get selectedGestionFilters() {
    const chips: string[] = [];
    const push = (label: string, values: any[], opts: any[]) => {
      if (values?.length) chips.push(`${label}: ${values.map(v => this.optionLabel(opts, v)).join(', ')}`);
    };
    push('Dispositivos', this.gestionF.dispositivos, this.dispositivos);
    push('Establecimientos', this.gestionF.establecimientos, this.establecimientos);
    push('Sexos', this.gestionF.sexos, this.sexos);
    push('Categorías', this.gestionF.categorias, this.categorias);
    push('Orígenes', this.gestionF.origenes, this.origenes);
    push('Diagnósticos', this.gestionF.gruposDiagnostico, this.gruposDiagnostico);
    push('Tramos', this.gestionF.tramosHorarios, this.tramosHorarios);
    if (this.gestionF.edadDesde !== '' || this.gestionF.edadHasta !== '') chips.push(`Edad: ${this.gestionF.edadDesde || 0}-${this.gestionF.edadHasta || '∞'}`);
    if (this.gestionF.fechaDesde || this.gestionF.fechaHasta) chips.push(`Fechas: ${this.gestionF.fechaDesde || 'inicio'} a ${this.gestionF.fechaHasta || 'hoy'}`);
    return chips;
  }
  get gestionKpis() {
    const total = this.gestionResumen?.total || this.gestionTotal || 0;
    if (this.isTiempoReporte) return [
      { icon: 'summarize', value: total, label: 'Total registros' },
      { icon: 'timer', value: this.gestionResumen?.promedio_minutos ?? '-', label: 'Promedio min.' },
      { icon: 'linear_scale', value: this.gestionResumen?.mediana_minutos ?? '-', label: 'Mediana min.' },
      { icon: 'south', value: this.gestionResumen?.minimo_minutos ?? '-', label: 'Mínimo min.' },
      { icon: 'north', value: this.gestionResumen?.maximo_minutos ?? '-', label: 'Máximo min.' }
    ];
    return [
      { icon: 'summarize', value: total, label: this.gestionTab === 0 ? 'DAU generados' : 'Atenciones finalizadas' },
      { icon: 'event', value: this.promedioPorPeriodo(), label: 'Promedio por periodo' },
      { icon: 'local_hospital', value: this.distintos('establecimiento'), label: 'Establecimientos' },
      { icon: 'category', value: this.displayCategoria(this.frecuente('categoria')), label: 'Categoría frecuente' },
      { icon: 'fact_check', value: this.displayEstado(this.frecuente('estado')), label: 'Estado frecuente' }
    ];
  }
  promedioPorPeriodo() { const n = this.gestionSeries?.length || 0; const total = this.gestionResumen?.total || 0; return n ? Math.round((total / n) * 10) / 10 : '-'; }
  distintos(campo: string) { return new Set((this.gestionRows || []).map(r => r[campo]).filter(v => v !== null && v !== undefined && v !== '')).size || '-'; }
  frecuente(campo: string) {
    const counts: any = {}; (this.gestionRows || []).forEach(r => { const v = r[campo] || '-'; counts[v] = (counts[v] || 0) + 1; });
    return Object.keys(counts).sort((a,b) => counts[b] - counts[a])[0] || '-';
  }
  get executiveCharts() {
    const defs: any[] = [
      { key: 'establecimientos', title: 'Establecimientos', subtitle: 'Distribución por origen', icon: 'local_hospital', mapper: (v:any) => this.displayEstablecimiento(v) },
      { key: 'categorias', title: 'Categorización', subtitle: 'Complejidad de atención', icon: 'emergency', mapper: (v:any) => this.displayCategoria(v) },
      { key: 'tramosHorarios', title: 'Tramos horarios', subtitle: 'Demanda por horario', icon: 'schedule', mapper: (v:any) => this.displayTramo(v) },
      { key: 'gruposDiagnostico', title: 'Grupos diagnósticos', subtitle: 'Perfil clínico', icon: 'monitor_heart', mapper: (v:any) => this.displayGrupoDiagnostico(v) }
    ];
    return defs.map(def => {
      const raw = this.gestionDistribuciones?.[def.key] || [];
      const max = Math.max(...raw.map((x:any) => Number(x.total || 0)), 1);
      const total = raw.reduce((sum:number, x:any) => sum + Number(x.total || 0), 0);
      return { ...def, total, items: raw.slice(0, 6).map((x:any) => ({ label: def.mapper(x.codigo), total: x.total, width: Math.max((Number(x.total || 0) / max) * 100, 3) })) };
    }).filter(c => c.items.length);
  }

  displayTramo(value: any) {
    const map: any = { AM: 'AM · Mañana', PM: 'PM · Tarde', NOCHE: 'Noche', MADRUGADA: 'Madrugada', SIN_DATO: 'Sin dato' };
    return map[value] || value || 'Sin dato';
  }

  displayGrupoDiagnostico(value: any) {
    const map: any = { RESPIRATORIO: 'Respiratorio', CARDIOVASCULAR: 'Cardiovascular', SALUD_MENTAL: 'Salud mental', TRAUMA: 'Trauma / lesiones', DIGESTIVO: 'Digestivo', OTROS: 'Otros' };
    return map[value] || value || 'Sin dato';
  }

  seriesWidth(s: any) { const max = Math.max(...(this.gestionSeries || []).map((x:any) => Number(x.total || 0)), 1); return Math.max((Number(s.total || 0) / max) * 100, 2); }
  gestionRequest(page = this.gestionF.page) {
    return { ...this.gestionF, page, size: this.gestionF.size || 20,
      edadDesde: this.gestionF.edadDesde === '' ? null : Number(this.gestionF.edadDesde),
      edadHasta: this.gestionF.edadHasta === '' ? null : Number(this.gestionF.edadHasta)
    };
  }
  onGestionTabChange(index: number) { this.gestionTab = index; this.loadGestion(0); }
  loadGestion(page = this.gestionF.page) {
    this.gestionF.page = page;
    this.http.post<any>(`${API}${this.gestionEndpoint()}`, this.gestionRequest(page)).subscribe({
      next: r => {
        const d = r.data || r;
        this.gestionResumen = d.resumen || {};
        this.gestionSeries = d.series || [];
        this.gestionDistribuciones = d.distribuciones || {};
        this.gestionRows = d.detalle || [];
        this.gestionTotal = d.page?.totalElements || this.gestionRows.length || 0;
      },
      error: e => this.toast(e?.error?.message || 'No fue posible cargar el reporte')
    });
  }
  onGestionPage(e: PageEvent) { this.gestionF.page = e.pageIndex; this.gestionF.size = e.pageSize; this.loadGestion(); }
  clearGestion() { this.gestionF = { dispositivos: [], establecimientos: [], sexos: [], categorias: [], origenes: [], gruposDiagnostico: [], tramosHorarios: [], edadDesde: '', edadHasta: '', fechaDesde: '', fechaHasta: '', agruparPor: 'DIA', page: 0, size: this.gestionF.size || 20 }; this.loadGestion(0); }
  exportGestionCsv() {
    const [modulo, submodulo] = this.gestionExportPath();
    this.http.post(`${API}/reportes/${modulo}/${submodulo}/exportar-csv`, this.gestionRequest(0), { responseType: 'text' }).subscribe({
      next: csv => {
        if (!csv || csv.trim().length === 0) { this.toast('CSV generado sin datos para los filtros seleccionados'); return; }
        const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = `${modulo}-${submodulo}.csv`; a.click(); URL.revokeObjectURL(url);
      },
      error: () => this.toast('No fue posible exportar CSV')
    });
  }

  loadUsers(page = this.userPage) {
    this.userPage = page;
    const params = new HttpParams().set('page', this.userPage).set('size', this.userSize);
    this.http.get<any>(`${API}/admin/users`, { params }).subscribe(r => { const p = pageData(r); this.usuarios = p.rows; this.userTotal = p.total; });
  }
  onUserPage(e: PageEvent) { this.userPage = e.pageIndex; this.userSize = e.pageSize; this.loadUsers(); }
  createUser() {
    if (!this.newUser.username || !this.newUser.password || !this.newUser.fullName) { this.toast('Complete username, clave y nombre.'); return; }
    this.http.post<any>(`${API}/admin/users`, this.newUser).subscribe({
      next: () => { this.toast('Usuario creado'); this.newUser = { username: '', password: '', fullName: '', email: '', providerName: '', role: 'VISUALIZADOR', enabled: true }; this.loadUsers(); },
      error: e => this.toast(e?.error?.message || 'No fue posible crear el usuario')
    });
  }

  editUser(u: any) {
    const fullName = prompt('Nombre completo', u.fullName || ''); if (fullName === null) return;
    const email = prompt('Email', u.email || ''); if (email === null) return;
    const providerName = prompt('Proveedor / origen', u.providerName || ''); if (providerName === null) return;
    const role = prompt('Rol: ADMIN, INTEGRADOR, GESTOR_RED, VISUALIZADOR, AUDITOR', u.role || 'VISUALIZADOR'); if (role === null) return;
    const body = { username: u.username, fullName, email, providerName, role, enabled: u.enabled };
    this.http.put<any>(`${API}/admin/users/${u.id}`, body).subscribe({
      next: () => {
        const newPass = prompt('Nueva clave opcional. Deje vacío para no cambiarla.', '');
        if (newPass && newPass.trim()) {
          this.http.patch<any>(`${API}/admin/users/${u.id}/password`, null, { params: new HttpParams().set('password', newPass.trim()) }).subscribe(() => { this.toast('Usuario y clave actualizados'); this.loadUsers(); });
        } else { this.toast('Usuario actualizado'); this.loadUsers(); }
      },
      error: e => this.toast(e?.error?.message || 'No fue posible editar el usuario')
    });
  }
  toggleUser(u: any) { this.http.patch<any>(`${API}/admin/users/${u.id}/enabled`, null, { params: new HttpParams().set('enabled', !u.enabled) }).subscribe(() => { this.toast('Usuario actualizado'); this.loadUsers(); }); }
}

bootstrapApplication(AppComponent, {
  providers: [provideAnimations(), provideHttpClient(withInterceptors([authInterceptor]))]
}).catch(err => console.error(err));
