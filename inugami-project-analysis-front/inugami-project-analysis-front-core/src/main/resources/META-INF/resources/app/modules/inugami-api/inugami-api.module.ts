//TODO:generate by servlet
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ComponentsServices } from './services/components.service';
import { MessageService } from './services/message.service';
import { HeaderServices } from './services/http/header.services';
import { HttpService } from './services/http/http.service';

@NgModule({
  declarations: [
  ],
  import : [
    BrowserModule
  ],
  export : [
    ComponentsServices,
    MessageService,
    HeaderServices,
    HttpService
  ],
  entryComponents: [
    
  ],
})
export class InugamiApiModule { 
}
