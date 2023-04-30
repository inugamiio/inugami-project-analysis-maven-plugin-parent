import { NgModule} from '@angular/core';
import { CommonModule } from '@angular/common';  
import { BrowserModule } from '@angular/platform-browser';
import {FormsModule}  from '@angular/forms'
import { RouterModule } from '@angular/router';
import { InugamiApiModule } from '../inugami-api/inugami-api.module';
import { registerComponent } from '../inugami-api/services/components.service';
import { HomeView } from './views/home/home.view';

import { MessageComponent } from './components/message.component';

import { ReleaseNoteComponent } from './components/release-note/release.note.component';
import { RestServiceComponent } from './components/rest-service-component/rest.service.component';
import { RestServiceDisplayComponent } from './components/rest-service-component/rest.servce.display.component';
import { DynamicComponentLoader } from './components/dynamic-component-loader/dynamic.component.loader';
import { PropertiesComponent } from './components/properties/properties.component';
import { PropertiesDisplayComponent } from './components/properties/properties.display.component';
import { ErrorCodesComponent } from './components/error-codes/error.codes.component';
import { ErrorCodesDisplayComponent } from './components/error-codes/error.codes.display.component';
import { DependenciesComponent } from './components/dependencies/dependencies.component';
import { DependenciesDisplayComponent } from './components/dependencies/dependencies.display.component';
import { DependencyInfoComponent } from './components/dependencies/dependency.info.component';
import { FlywayComponent } from './components/flyway/flyway.component';
import { FlywayDisplayComponent } from './components/flyway/flyway.display.component';
import { EntitiesComponent } from './components/entities/entities.component';
import { EntitiesDisplayComponent } from './components/entities/entities.display.component';
import { ChangeComponent } from './components/change/change.component';
import { ProjectDependencyGraphComponent } from './components/dependencies/project.dependency.graph.component';


import { GlossaryComponent } from './components/glossary/glossary.component';
import { GlossaryDisplayComponent } from './components/glossary/glossary.display.component';

@NgModule({
  declarations: [
    HomeView,
    MessageComponent,
    ReleaseNoteComponent,
    RestServiceComponent,
    DynamicComponentLoader,
    RestServiceDisplayComponent,
    PropertiesComponent,
    PropertiesDisplayComponent,
    ErrorCodesComponent,
    ErrorCodesDisplayComponent,
    DependenciesComponent,
    DependenciesDisplayComponent,
    DependencyInfoComponent,
    FlywayComponent,
    FlywayDisplayComponent,
    EntitiesComponent,
    EntitiesDisplayComponent,
    ChangeComponent,
    ProjectDependencyGraphComponent,
    GlossaryComponent,
    GlossaryDisplayComponent
  ],
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    InugamiApiModule,
    RouterModule.forRoot([
        { path: '', component: HomeView , pathMatch:'full'}
    ])
  ],
  export : [
    HomeView,
    MessageComponent,
    ReleaseNoteComponent,
    RestServiceComponent,
    RestServiceDisplayComponent,
    PropertiesComponent,
    PropertiesDisplayComponent,
    ErrorCodesComponent,
    ErrorCodesDisplayComponent,
    DependenciesComponent,
    DependenciesDisplayComponent,
    DependencyInfoComponent,
    FlywayComponent,
    FlywayDisplayComponent,
    EntitiesComponent,
    EntitiesDisplayComponent,
    ChangeComponent,
    ProjectDependencyGraphComponent,
    GlossaryComponent,
    GlossaryDisplayComponent
  ],
  entryComponents: [
    RestServiceComponent,
    PropertiesComponent,
    ErrorCodesComponent,
    DependenciesComponent,
    FlywayComponent,
    EntitiesComponent,
    GlossaryComponent
  ],
  providers: [
    HomeView
  ],
  bootstrap: []
})
export class InugamiProjectAnalysisPluginModule { 

  constructor(){
    registerComponent("exposedService",RestServiceComponent);
    registerComponent("consumedService",RestServiceComponent);
    registerComponent("properties",PropertiesComponent);
    registerComponent("errorCodes",ErrorCodesComponent);
    registerComponent("dependencies",DependenciesComponent);
    registerComponent("dependencies_project",DependenciesComponent);
    registerComponent("flyway",FlywayComponent);
    registerComponent("entities",EntitiesComponent);
    registerComponent("glossary",GlossaryComponent);
    
  }
 
}
