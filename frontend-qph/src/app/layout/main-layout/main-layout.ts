import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonModule, TooltipModule],
  templateUrl: './main-layout.html',
  styleUrls: ['./main-layout.scss']
})
export class MainLayoutComponent {

  private authService = inject(AuthService);

  user = this.authService.getUser();
  isAdmin = this.authService.isAdmin();

  logout(): void {
    this.authService.logout();
  }
}
