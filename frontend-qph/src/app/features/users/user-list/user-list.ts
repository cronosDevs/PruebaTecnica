import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { UserService } from '../../../core/services/user';
import { AuthService } from '../../../core/services/auth';
import { User } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, TableModule, ButtonModule, TagModule, ConfirmDialogModule, ToastModule, TooltipModule],
  providers: [ConfirmationService, MessageService],
  templateUrl: './user-list.html',
  styleUrls: ['../../products/product-list/product-list.scss']
})
export class UserListComponent implements OnInit {

  private userService = inject(UserService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  users: User[] = [];
  loading = false;
  currentUsername = this.authService.getUser()?.username;

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudieron cargar los usuarios'
        });
        this.loading = false;
      }
    });
  }

  goToCreate(): void {
    this.router.navigate(['/users/new']);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/users/edit', id]);
  }

  confirmToggle(user: User): void {
    const activar = !user.activo;
    this.confirmationService.confirm({
      message: activar
        ? `¿Activar a "${user.nombreCompleto}"?`
        : `¿Desactivar a "${user.nombreCompleto}"? No podrá iniciar sesión.`,
      header: activar ? 'Activar usuario' : 'Desactivar usuario',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: activar ? 'Sí, activar' : 'Sí, desactivar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: activar ? '' : 'p-button-danger',
      accept: () => this.changeStatus(user, activar)
    });
  }

  private changeStatus(user: User, activo: boolean): void {
    this.userService.changeStatus(user.id, activo).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: activo ? 'Usuario activado' : 'Usuario desactivado'
        });
        this.loadUsers();
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: err.error?.detail ?? 'No se pudo cambiar el estado del usuario'
        });
      }
    });
  }
}
