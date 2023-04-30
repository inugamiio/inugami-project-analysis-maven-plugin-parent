import { Component, forwardRef, Input, Inject, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';
export const GLOSSARY_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => GlossaryDisplayComponent),
    multi: true
};


@Component({
    selector: 'glossary-display',
    templateUrl: './app/modules/inugami-project-analysis-plugin/components/glossary/glossary.display.component.html',
    directives: [MessageComponent],
    providers: [GLOSSARY_DISPLAY_COMPONENT_VALUE_ACCESSOR]
})
export class GlossaryDisplayComponent implements OnInit {
    @Input() title: string;
    private innerValue: any;

    constructor(@Inject(MessageService) private messageService: MessageService) {
    }

    ngOnInit() {
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