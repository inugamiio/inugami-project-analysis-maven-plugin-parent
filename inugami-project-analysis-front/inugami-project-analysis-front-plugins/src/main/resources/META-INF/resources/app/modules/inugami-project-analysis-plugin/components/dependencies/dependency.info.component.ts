import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { Dependency, DependencyInfo } from '../../models/dependencies.check';
import { isNull, isNotNull } from '../../../inugami-api/services/utils';

export const DEPENDENCY_INFO_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DependencyInfoComponent),
    multi: true
};


@Component({
    selector: 'dependency-info',
    directives: [],
    providers: [DEPENDENCY_INFO_COMPONENT_VALUE_ACCESSOR],
    template: `
<span [class]="getClass()" *ngIf="hasValue">
    <a [href]="link" target="blank">
        <span [class]="getIconClass()" [title]="title"></span>
    </a>
</span>
`
})
export class DependencyInfoComponent implements OnInit {

    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    @Input() type: string;
    private innerValue: any;
    private info: DependencyInfo;

    private hasValue: boolean = false;
    private link: string;
    private title: string;
    private level: string;


    /**************************************************************************
    * CONSTRUCTOR
    **************************************************************************/

    /**************************************************************************
    * API
    **************************************************************************/
    public getClass() {
        return ['dependency-info',this.type].join(' ');
    }

    public getIconClass() {
        return ['icon', this.type, this.level].join(' ');
    }

    /*****************************************************************************
    * IMPLEMENTS ControlValueAccessor
    *****************************************************************************/
    writeValue(value: any) {
        if (value !== this.innerValue) {
            if (isNotNull(value)) {
                this.hasValue = isNotNull(value)
            }
            this.innerValue = value;
        }

        if (this.hasValue) {
            this.link = isNull(this.innerValue.link) ? '#' : this.innerValue.link;
            this.title = isNull(this.innerValue.comment) ? this.type : this.innerValue.comment;
            this.level = isNull(this.innerValue.level) ? '' : this.innerValue.level;
        }
    }

    registerOnChange(fn: any) {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any) {
        this.onTouchedCallback = fn;
    }
}