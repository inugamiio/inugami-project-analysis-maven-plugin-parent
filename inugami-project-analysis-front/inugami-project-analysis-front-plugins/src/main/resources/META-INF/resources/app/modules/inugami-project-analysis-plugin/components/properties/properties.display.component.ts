import { Component, forwardRef, Input, Inject, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';
export const PROPERTIES_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => PropertiesDisplayComponent),
    multi: true
};


@Component({
    selector: 'properties-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/properties/properties.display.component.html',
    directives: [MessageComponent],
    providers: [PROPERTIES_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class PropertiesDisplayComponent implements OnInit {
    @Input() title: string;
    private hasValues: boolean = false;
    private mandatory: string;
    private conditionalBean: string;
    private innerValue: any;

    constructor(@Inject(MessageService) private messageService: MessageService) {
    }

    ngOnInit() {
        this.mandatory = this.messageService.getMessage('mandatory');
        this.conditionalBean = this.messageService.getMessage('conditional.bean');
    }
    getClass(property: any) {
        let result = [];
        if (property != undefined && property != null) {
            if (property.mandatory) {
                result.push('mandatory');
            }
            if (property.useForConditionalBean) {
                result.push('useForConditionalBean');
            }
        }
        return result.join(" ");
    }


    /*****************************************************************************
    * IMPLEMENTS ControlValueAccessor
    *****************************************************************************/
    writeValue(value: any) {
        if (value !== this.innerValue) {
            this.hasValues = value != null && value.length > 0;
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