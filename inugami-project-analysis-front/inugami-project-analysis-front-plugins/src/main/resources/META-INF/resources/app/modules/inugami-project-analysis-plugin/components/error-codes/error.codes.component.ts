import { Component, Inject, OnInit } from '@angular/core';
import { ComponentDisplay } from '../../../inugami-api/components/component_display';
import { differentialHasContent } from '../../../inugami-api/services/utils';
import { ErrorCodesDisplayComponent } from './error.codes.display.component';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';

@Component({
    selector      : 'properties',
    template: `
<fieldset class="properties" *ngIf="hasContent">
  <h3><msg key="{{title}}"></msg></h3>
  <div class="differential">
    <error-codes-display [title]="deletedValueTitle" [(ngModel)]="innerValue.deletedValues"></error-codes-display>
    <error-codes-display [title]="newValueTitle" [(ngModel)]="innerValue.newValues"></error-codes-display>
    <error-codes-display [title]="sameValueTitle" [(ngModel)]="innerValue.sameValues"></error-codes-display>
  </div>
</fieldset>
        `,
    directives    : [ErrorCodesDisplayComponent, MessageComponent ],
    providers     : []
  })
  export class ErrorCodesComponent implements ComponentDisplay,OnInit {
    private innerValue : any;
    private title : string;
    private hasContent : boolean = false;

    private deletedValueTitle: string;
    private newValueTitle: string;
    private sameValueTitle: string;
  
    constructor(@Inject(MessageService) private messageService: MessageService) {
    }
  
    ngOnInit(){
      this.deletedValueTitle = this.messageService.getMessage(this.title+'.deleted');
      this.newValueTitle = this.messageService.getMessage(this.title+'.new');
      this.sameValueTitle = this.messageService.getMessage(this.title+'.same');
  }

    public initialize(context:string, value: any){
        this.innerValue = value;
        this.title=context;
        this.hasContent = differentialHasContent(value);
    }

  }