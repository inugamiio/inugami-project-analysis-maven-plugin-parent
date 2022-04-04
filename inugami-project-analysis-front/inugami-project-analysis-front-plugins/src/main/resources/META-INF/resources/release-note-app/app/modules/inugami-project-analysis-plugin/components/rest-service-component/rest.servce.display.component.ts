import { Component, forwardRef,Input,SecurityContext,Inject  } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { highlightEvent, fireEvent } from '../../../inugami-api/models/events';
import { MessageComponent } from '../message.component';

export const REST_SERVICE_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RestServiceDisplayComponent),
    multi: true
};


@Component({
    selector: 'rest-service-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/rest-service-component/rest.servce.display.component.html',
    directives: [MessageComponent],
    providers: [REST_SERVICE_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class RestServiceDisplayComponent{
    @Input()  title         : string;

    private innerValue: any;
    private hasValues : boolean = false;
    private showDetail : boolean[] = [];
    private highlightInit : boolean = false;

    constructor(@Inject(DomSanitizer) private domSanitizer:DomSanitizer){
    }

    initHighlight(){
        if(!this.highlightInit){
            fireEvent(highlightEvent);
            this.highlightInit=true;
        }    
        return "";    
    }
    getServiceType(service:any){
        let result ="";
        if(service.type != undefined && service.type!="REST"){
            result=service.type;
        }
        return result;
    }

    getServiceClass(service:any){
        let result = [];

        if(service.verb != undefined){
            result.push(service.verb);
        }
        if(service.type != undefined){
            result.push(service.type);
        }
        return result.join(" ");
    }

    toggleDetail(index:number){
        this.showDetail[index] = !this.showDetail[index];
    }

    getDisplayDetailClass(index:number){
        return ['display-detail-icon', this.showDetail[index]?'hide':'show'].join(" ");
    }

    getDisplayDetail(index:number){
        return ['display-detail', this.showDetail[index]?'show':'hide'].join(" ");
    }

    renderMethodName(name:string){
        let result = "";
        if(name!=undefined && name!=null){
            result = name.split(":").join(" ");
        }
        return result;
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
        if(this.hasValues){
            for(let item of this.innerValue ){
                this.showDetail.push(false);
            }
        }
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }
}