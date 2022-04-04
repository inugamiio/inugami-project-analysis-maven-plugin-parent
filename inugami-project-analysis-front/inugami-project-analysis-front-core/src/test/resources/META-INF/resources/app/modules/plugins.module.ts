import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { InugamiApiModule } from './inugami-api/inugami-api.module';
import { InugamiProjectAnalysisPluginModule } from './inugami-project-analysis-plugin/inugami-project-analysis-plugin.module';

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    InugamiApiModule,
    InugamiProjectAnalysisPluginModule
  ]
})
export class PluginsModule { }
