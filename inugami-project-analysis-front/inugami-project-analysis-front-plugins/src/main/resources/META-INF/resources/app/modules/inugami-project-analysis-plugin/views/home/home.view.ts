import { Component, OnInit, Inject } from '@angular/core';
import { ReleaseNoteComponent } from '../../components/release-note/release.note.component';
import { ReleaseNote } from '../../models/release.note';
import { HttpService } from '../../../inugami-api/services/http/http.service';
import { DependenciesCheckService } from '../../services/dependencies.check.service';
@Component({
  template: `
<ul *ngIf="releases" class="releases">
    <li *ngFor="let release of releases; let i = index">
        <release-note  [(ngModel)]="releases[i]"></release-note>
    </li>
</ul>
        `,
  directives: [ReleaseNoteComponent]
})
export class HomeView implements OnInit {

  /**************************************************************************
  * ATTRIBUTES
  **************************************************************************/
  private releases: ReleaseNote[] = [];

  /**************************************************************************
  * CONSTRUCTOR
  **************************************************************************/

  constructor(
    @Inject(HttpService) private httpService: HttpService,
    @Inject(DependenciesCheckService) private dependenciesCheckService: DependenciesCheckService
  ) {

  }

  /**************************************************************************
  * INITIALIZE
  **************************************************************************/
  ngOnInit() {
    this.httpService.get('http://localhost/analysis/data/release-notes.json')
      .then(response => {
        this.releases = response.body;
      })
  }

}
