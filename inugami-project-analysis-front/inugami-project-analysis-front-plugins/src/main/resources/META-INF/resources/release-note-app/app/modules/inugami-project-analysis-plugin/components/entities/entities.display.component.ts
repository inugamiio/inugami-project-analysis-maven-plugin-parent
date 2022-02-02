import { Component, forwardRef,Input,SecurityContext,Inject  } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { highlightEvent, fireEvent } from '../../../inugami-api/models/events';
import { MessageComponent } from '../message.component';

export const ENTITIES_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntitiesDisplayComponent),
    multi: true
};


@Component({
    selector: 'entities-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/entities/entities.display.component.html',
    directives: [MessageComponent],
    providers: [ENTITIES_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class EntitiesDisplayComponent{
    @Input()  title         : string;
    private hasValues : boolean = false;
    private innerValue: any;
    private highlightInit : boolean = false;
    private eventHighlight : Event =  new Event('highlight');

    constructor(@Inject(DomSanitizer) private domSanitizer:DomSanitizer){
    }
   
    initHighlight(){
        if(!this.highlightInit){
            fireEvent(highlightEvent);
            this.highlightInit=true;
        }    
        return "";    
    }

    renderScript(value:string){
        return this.domSanitizer.sanitize(SecurityContext.HTML,value);
    }
    /*****************************************************************************
    * IMPLEMENTS ControlValueAccessor
    *****************************************************************************/
    writeValue(value: any) {
        if (value !== this.innerValue) {
            this.hasValues = value!=null && value.length>0;
            this.innerValue = value;
        }
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }
}