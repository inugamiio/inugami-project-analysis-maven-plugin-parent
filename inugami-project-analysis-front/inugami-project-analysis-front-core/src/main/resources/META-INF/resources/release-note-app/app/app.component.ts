import { Component,OnInit } from '@angular/core';
import {CONFIG} from './env'
@Component({
  selector: 'app-root',
  templateUrl: CONFIG.APP_ROOT_HTML,
  styleUrls: CONFIG.APP_ROOT_CSS
})
export class AppComponent implements OnInit {
    constructor(){
    }

    ngOnInit() {
      document.dispatchEvent(new CustomEvent("onInit", {}));
    }
   
}
