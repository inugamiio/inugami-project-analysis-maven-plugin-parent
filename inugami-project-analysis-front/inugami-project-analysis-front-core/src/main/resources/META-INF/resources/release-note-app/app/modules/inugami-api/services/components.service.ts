import { Injectable } from '@angular/core';

import { ComponentDisplay } from '../components/component_display';
const COMPONENTS = {};

export const registerComponent = (section, component)=>{
    let components = COMPONENTS[section];
    if (components == undefined || components == null) {
        COMPONENTS[section] = [];
        components = COMPONENTS[section];
    }
    components.push(component);
}

@Injectable({
    providedIn: 'root',
  })
export class ComponentsServices {


    public getComponents(section: string) {
        let result = COMPONENTS[section];
        return result == undefined || result == null ? [] : result;
    }
}