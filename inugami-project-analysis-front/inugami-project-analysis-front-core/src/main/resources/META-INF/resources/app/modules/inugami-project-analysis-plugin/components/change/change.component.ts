import { Component, forwardRef, Input } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { isNotNull, isEmpty } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';
export const CHANGE_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ChangeComponent),
    multi: true
};


@Component({
    selector: 'change',
    directives: [MessageComponent],
    providers: [CHANGE_COMPONENT_VALUE_ACCESSOR],
    template: `
<fieldset class="change" *ngIf="hasValue" >
    <h3><msg key="change"></msg>:</h3>
    <span class="change-row new-change" *ngIf="newChange >0">
        <label><msg key="new"></msg></label>
        <span class="value" [innerHTML]="newChange"></span>
    </span>
    <span class="change-row deleted-change"  *ngIf="deletedChange >0">
        <label><msg key="deleted"></msg></label>
        <span class="value" [innerHTML]="deletedChange"></span>
    </span>
</fieldset>
`
})
export class ChangeComponent {

    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    private innerValue: any;
    private hasValue : boolean = false;
    private newChange : number = 0;
    private deletedChange : number = 0;

    /**************************************************************************
    * API
    **************************************************************************/
    private initializeData(){
        for(let diffType of Object.keys(this.innerValue.differentials)){
            let diff = this.innerValue.differentials[diffType];
            this.newChange += isEmpty(diff.newValues)?0:diff.newValues.length;
            this.deletedChange += isEmpty(diff.deletedValues)?0:diff.deletedValues.length;
        }
    }    

    /*****************************************************************************
    * IMPLEMENTS ControlValueAccessor
    *****************************************************************************/
    writeValue(value: any) {
        if (value !== this.innerValue) {
            this.hasValue = isNotNull(value) && isNotNull(value.differentials)
            this.innerValue = value;
        }

        if(this.hasValue){
            this.initializeData();
        }
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }
}