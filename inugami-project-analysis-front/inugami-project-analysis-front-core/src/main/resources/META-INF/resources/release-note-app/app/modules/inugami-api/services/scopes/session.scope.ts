import { Injectable }   from '@angular/core';
import { isNull,isEmpty, isNotEmpty ,buildUid } from '../utils';
import { HEADERS } from '../http/header.constants';
@Injectable({
  providedIn: 'root',
})
export class SessionScope {
    /**************************************************************************
    * ATTRIBUTES
    **************************************************************************/
    private deviceIdentifier      : string = null;
    private correlationId         : string = null;


    /**************************************************************************
    * TRACKING
    **************************************************************************/
    public getDeviceIdentifier(){
      if(isNull(this.deviceIdentifier)){
        this.deviceIdentifier = localStorage.getItem(HEADERS.deviceIdentifier);

        if(isNull(this.deviceIdentifier)){
            this.deviceIdentifier = buildUid();
            localStorage.setItem(HEADERS.deviceIdentifier, this.deviceIdentifier);
        }
      }
      return this.deviceIdentifier;
    }

    public getCorrelationId(){
      return this.correlationId;
    }
    public setCorrelationId(value:string){
      if(isEmpty(this.correlationId) && isNotEmpty(value)){
        this.correlationId=value;
      }
    }

    public resetCorrelationId(){
      this.correlationId=null;
    }


}
