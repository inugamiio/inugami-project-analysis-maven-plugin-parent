import { Component, forwardRef,Input  } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { isNotNull } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';

export const ERROR_CODES_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ErrorCodesDisplayComponent),
    multi: true
};


@Component({
    selector: 'error-codes-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/error-codes/error.codes.display.component.html',
    directives: [MessageComponent],
    providers: [ERROR_CODES_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class ErrorCodesDisplayComponent{
    @Input()  title         : string;
    private hasValues : boolean = false;
    private innerValue: any;


    public getClass(errorCode : any){
        let result = [];
        if(isNotNull(errorCode)){
            if(isNotNull(errorCode.type)){
                result.push(errorCode.type)
            }
        }
        return result.join(" ");
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