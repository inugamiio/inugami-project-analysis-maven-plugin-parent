import { Component, forwardRef,Input,SecurityContext,Inject  } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { isNotNull } from '../../../inugami-api/services/utils';
import { highlightEvent, fireEvent } from '../../../inugami-api/models/events';
import { MessageComponent } from '../message.component';

export const FLYWAY_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FlywayDisplayComponent),
    multi: true
};


@Component({
    selector: 'flyway-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/flyway/flyway.display.component.html',
    directives: [MessageComponent],
    providers: [FLYWAY_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class FlywayDisplayComponent{
    @Input()  title         : string;
    private hasValues : boolean = false;
    private innerValue: any;
    private highlightInit : boolean = false;

    constructor(@Inject(DomSanitizer) private domSanitizer:DomSanitizer){
    }

    getLanguage(type:string){
        let currentType = "plain";

        if(isNotNull(type)){
            if(type.toLocaleLowerCase().indexOf("sql")!=-1){
                currentType = "sql";
            }
        }

        return "language-"+currentType;
    }

    initHighlight(){
        if(!this.highlightInit){
            fireEvent(highlightEvent);
            this.highlightInit = true;
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