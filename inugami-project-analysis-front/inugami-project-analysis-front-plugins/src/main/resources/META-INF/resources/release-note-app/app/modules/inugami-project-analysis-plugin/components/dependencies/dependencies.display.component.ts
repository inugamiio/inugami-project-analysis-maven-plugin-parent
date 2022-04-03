import { Component, forwardRef, Input, Inject, OnInit } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { isNotNull, isNull } from '../../../inugami-api/services/utils';
import { MessageComponent } from '../message.component';
import { DependenciesCheckService } from '../../services/dependencies.check.service';
import { Dependency, DependencyInfo } from '../../models/dependencies.check';
//import { DependencyInfoComponent } from './dependency.info.component';

export const DEPENDENCIES_DISPLAY_COMPONENT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DependenciesDisplayComponent),
    multi: true
};


@Component({
    selector: 'dependencies-display',
    directives: [MessageComponent],
    providers: [DEPENDENCIES_DISPLAY_COMPONENT_VALUE_ACCESSOR],
    template: `
<div class="dependencies-display" *ngIf="hasValues">
    <h4>{{title}}</h4>
    <table class="table">
        <thead>
          <tr>
            <th scope="col"><msg key="groupid"></msg></th>
            <th scope="col"><msg key="artifactid"></msg></th>
            <th scope="col"><msg key="type"></msg></th>
            <th scope="col"><msg key="version"></msg></th>
            <th scope="col"><msg key="details"></msg></th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let item of data; let i = index" [class]="getClass(item)">
            <td [innerHTML]="item.dependency.groupId"></td>
            <td [innerHTML]="item.dependency.artifactId"></td>
            <td [innerHTML]="item.dependency.type"></td>
            <td [innerHTML]="item.dependency.version"></td>
            <td [class]="getClass(item)">
                <span class="dependency-info" *ngIf="item.isNotEmpty">
                    <span *ngFor="let type of infoTypes">
                        <dependency-info [type]="type" [(ngModel)]="item.info[type]" *ngIf="item.info[type]!=null"></dependency-info>
                    </span>
                </span>
                
                <span class="icon brand"></span>
            </td>
          </tr>          
        </tbody>
      </table>
</div>
`
})
export class DependenciesDisplayComponent implements OnInit {

    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    @Input() title: string;
    private hasValues: boolean = false;
    private innerValue: any;

    private data: any[] = [];
    private infoTypes = [];
    private dependenciesCheck: any = {};

    /**************************************************************************
    * CONSTRUCTOR
    **************************************************************************/
    constructor(
        @Inject(DependenciesCheckService) private dependenciesCheckService: DependenciesCheckService
    ) { }

    ngOnInit() {
        this.infoTypes = this.dependenciesCheckService.getType()
    }
    /**************************************************************************
    * API
    **************************************************************************/
    public getClass(item: any) {
        let result = [];
        if (isNotNull(item.dependency)) {
            if (isNotNull(item.dependency.groupId)) {
                result = item.dependency.groupId.split('.');
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

        if (this.hasValues) {
            for (let dependency of this.innerValue) {
                let isNotEmpty = false;
                let info = {};
                if(isNotNull(this.infoTypes)){
                    for(let type of this.infoTypes){
                        let infoDependency =this.dependenciesCheckService.getDependencyInfo(dependency, type);
                        info[type]=infoDependency;
                        if(isNotNull(infoDependency)){
                            isNotEmpty = true;
                        }
                    }
                }


                this.data.push({
                    "dependency": dependency,
                    "info": info,
                    "isNotEmpty" : isNotEmpty
                });
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