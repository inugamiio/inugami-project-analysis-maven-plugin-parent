import {
    Component, forwardRef, ViewChild,
    ViewContainerRef,Inject,ComponentFactoryResolver,Renderer2
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ComponentsServices } from './../../../inugami-api/services/components.service';
import { ComponentDisplay } from '../../../inugami-api/components/component_display';
export const DYNAMIC_COMPONENT_LOADER_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DynamicComponentLoader),
    multi: true
};



@Component({
    selector: 'dynamic-component-loader',
    template: `
<div [ngClass]="'dynamic-component-loader'">
    <div class="content">
        <div #compoDisplayContent ></div>
    </div>
</div>
    `,
    directives: [],
    providers: [DYNAMIC_COMPONENT_LOADER_VALUE_ACCESSOR]
})
export class DynamicComponentLoader implements ControlValueAccessor {


    /**************************************************************************
     * ATTRIBUTES
     **************************************************************************/
    private innerValue: any;
    private sections: string[]

    @ViewChild('compoDisplayContent', { read: ViewContainerRef }) content: ViewContainerRef;


    /***************************************************************************
     * CONSTRUCTOR
    ***************************************************************************/
    constructor(
        @Inject(ComponentsServices) private service: ComponentsServices,
        @Inject(ComponentFactoryResolver) private componentFactoryResolver: ComponentFactoryResolver,
        @Inject(Renderer2) private render: Renderer2) {
 
    }

    /**************************************************************************
     * INIT
     **************************************************************************/
    private loadComponentDisplay() {
        for(let section of this.sections){
            let components =this.service.getComponents(section);

            for(let componentDef of components){
                let componentFactory = this.componentFactoryResolver.resolveComponentFactory(componentDef);
                let componentRef     =  this.content.createComponent(componentFactory);
                let component       = componentRef._component;
                this.initializeComponent(section,component,this.innerValue[section]);
                componentRef.changeDetectorRef.detectChanges();
            }
        }
    }

    private initializeComponent(section:string, component:ComponentDisplay, value:any){
        component.initialize(section,value);
    }

    /***************************************************************************
    * IMPLEMENTS ControlValueAccessor
    ***************************************************************************/
    writeValue(value: any) {
        this.innerValue = value;
        if (value != undefined && value != null) {
            let rawSections = Object.keys(value).sort();

            this.sections = [];
            if(rawSections.indexOf('dependencies_project')!=-1){
                this.sections.push('dependencies_project');
            }

            let hasErrorCode = false;
            let hasDependencies = false;

            for(let rawSection of rawSections){
                if(rawSection != 'dependencies_project'){
                    if(rawSection == 'errorCodes'){
                        hasErrorCode = true;
                    }
                    else if(rawSection == 'dependencies'){
                        hasDependencies = true;
                    }
                    else{
                        this.sections.push(rawSection);
                    }
                }
            }
            
            if(hasErrorCode){
                this.sections.push('errorCodes');
            }

            if(hasDependencies){
                this.sections.push('dependencies');
            }
            this.loadComponentDisplay();
        }

    }

    registerOnChange(fn: any) { }
    registerOnTouched(fn: any) { }
}