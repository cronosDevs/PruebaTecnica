import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { UserService } from '../../../core/services/user';
import { Role, UserRequest } from '../../../shared/models/user.model';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, InputTextModule, ToastModule],
  providers: [MessageService],
  templateUrl: './user-form.html',
  styleUrls: ['../../products/product-form/product-form.scss', './user-form.scss']
})
export class UserFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private messageService = inject(MessageService);

  form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[A-Za-z0-9._-]+$/)]],
    password: ['', [Validators.minLength(8), Validators.maxLength(72), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)]],
    nombreCompleto: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    rol: ['', Validators.required]
  });

  roles: Role[] = [];
  isEdit = false;
  userId?: number;
  loading = false;
  submitting = false;

  ngOnInit(): void {
    this.userService.getRoles().subscribe(roles => this.roles = roles);

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.userId = Number(id);
      this.form.controls.username.disable();
      this.loadUser(this.userId);
    } else {
      this.form.controls.password.addValidators(Validators.required);
      this.form.controls.password.updateValueAndValidity();
    }
  }

  private loadUser(id: number): void {
    this.loading = true;
    this.userService.getById(id).subscribe({
      next: (user) => {
        this.form.patchValue({
          username: user.username,
          nombreCompleto: user.nombreCompleto,
          email: user.email,
          rol: user.rol
        });
        this.loading = false;
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo cargar el usuario'
        });
        this.loading = false;
      }
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const value = this.form.getRawValue();
    const data: UserRequest = {
      nombreCompleto: value.nombreCompleto,
      email: value.email,
      rol: value.rol,
      password: value.password || null
    };
    if (!this.isEdit) {
      data.username = value.username;
    }

    const request = this.isEdit
      ? this.userService.update(this.userId!, data)
      : this.userService.create(data);

    request.subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: this.isEdit ? 'Usuario actualizado' : 'Usuario creado'
        });
        setTimeout(() => this.router.navigate(['/users']), 1500);
      },
      error: (err) => {
        const errores = err.error?.errores;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: errores ? Object.values(errores).join('. ') : (err.error?.detail ?? 'Error al guardar el usuario')
        });
        this.submitting = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/users']);
  }
}
