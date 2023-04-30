import { Component, Inject, OnInit } from '@angular/core';
import { ComponentDisplay } from '../../../inugami-api/components/component_display';
import { GlossaryDisplayComponent } from './glossary.display.component';
import { differentialHasContent } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';

@Component({
  selector: 'glossary',
  template: `
<fieldset class="glossary" *ngIf="hasContent">
  <h3><msg key="{{title}}"></msg></h3>
  <div class="glossary">
    <glossary-display [(ngModel)]="innerValue"></glossary-display>
  </div>
</fieldset>
        `,
  directives: [GlossaryDisplayComponent],
  providers: []
})
export class GlossaryComponent implements ComponentDisplay, OnInit {
  private innerValue: any;
  private title: string;
  private hasContent: boolean = false;


  constructor(@Inject(MessageService) private messageService: MessageService) {
  }

  ngOnInit() {
    
  }

  public initialize(context: string, value: any) {
    this.innerValue = value;
    this.title = context;
    this.hasContent = value!= null;
  }

}