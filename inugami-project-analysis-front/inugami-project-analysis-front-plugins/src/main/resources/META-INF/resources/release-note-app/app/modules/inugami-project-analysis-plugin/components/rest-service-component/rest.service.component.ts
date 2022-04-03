import { Component, Inject, OnInit } from '@angular/core';
import { ComponentDisplay } from '../../../inugami-api/components/component_display';
import { RestServiceDisplayComponent } from './rest.servce.display.component';
import { differentialHasContent } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';

@Component({
  selector: 'rest-service',
  template: `
<fieldset class="rest-service" *ngIf="hasContent">
  <h3><msg key="{{title}}"></msg></h3>
  <div class="differential">
    <rest-service-display [title]="deletedValueTitle" [(ngModel)]="innerValue.deletedValues"></rest-service-display>
    <rest-service-display [title]="newValueTitle" [(ngModel)]="innerValue.newValues"></rest-service-display>
    <rest-service-display [title]="sameValueTitle" [(ngModel)]="innerValue.sameValues"></rest-service-display>
  </div>
</fieldset>
        `,
  directives: [RestServiceDisplayComponent],
  providers: []
})
export class RestServiceComponent implements ComponentDisplay, OnInit {
  private innerValue: any;
  private title: string;
  private hasContent: boolean;

  private deletedValueTitle: string;
  private newValueTitle: string;
  private sameValueTitle: string;

  constructor(@Inject(MessageService) private messageService: MessageService) {
  }

  ngOnInit() {
    this.deletedValueTitle = this.messageService.getMessage(this.title + '.deleted');
    this.newValueTitle = this.messageService.getMessage(this.title + '.new');
    this.sameValueTitle = this.messageService.getMessage(this.title + '.same');
  }
  public initialize(context: string, value: any) {
    this.innerValue = value;
    this.title = context;
    this.hasContent = differentialHasContent(value);
  }

}