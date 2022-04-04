import { Injectable } from '@angular/core';
import { isNotNull , isNull} from './utils';

@Injectable({
    providedIn: 'root'
})
export class MessageService {
    private messages: any = document["MESSAGES"];
    private language: string = navigator.language.split('-')[0];

    public defineLanguage(language: string) {
        this.language = isNotNull(language) ? language : 'en';
    }
    public getMessage(key: string, values?:any[]) {
        let result = null;
        if (isNotNull(key) && isNotNull(this.messages)) {
            if(isNotNull(this.messages[this.language])){
                result = this.messages[this.language][key];
            }
            if(isNull(result)){
                result = this.messages['en'][key];
            }
            if(isNull(result)){
                result = ['??', key, '??'].join('');
            }else{
                result = this.format(result, values);
            }
        }
        return result;
    }
   
    public format(message:string ,values:any[]){
        var formatted = message;
        if(isNotNull(values)){
          for (var i = 0; i < values.length; i++) {
              var regexp = new RegExp('\\{'+i+'\\}', 'gi');
              formatted = formatted.replace(regexp, values[i]);
            }
        }
        return formatted;
      }
}