import { Component, Inject, OnInit } from '@angular/core';
import { ComponentDisplay } from '../../../inugami-api/components/component_display';
import { EntitiesDisplayComponent } from './entities.display.component';
import { differentialHasContent } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';

@Component({
  selector: 'flyway',
  template: `
<fieldset class="flyway" *ngIf="hasContent">
  <h3><msg key="{{title}}"></msg></h3>
  <div class="differential">
    <entities-display [title]="deletedValueTitle" [(ngModel)]="innerValue.deletedValues"></entities-display>
    <entities-display [title]="newValueTitle" [(ngModel)]="innerValue.newValues"></entities-display>
    <entities-display [title]="sameValueTitle" [(ngModel)]="innerValue.sameValues"></entities-display>
  </div>
</fieldset>
        `,
  directives: [EntitiesDisplayComponent, MessageComponent],
  providers: []
})
export class EntitiesComponent implements ComponentDisplay, OnInit {
  private innerValue: any;
  private title: string;
  private hasContent: boolean = false;


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