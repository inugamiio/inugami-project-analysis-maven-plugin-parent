import { Injectable, Inject } from '@angular/core';
import { HttpHeaders, RequestOptions } from '@angular/common/http';
import { SessionScope } from '../scopes/session.scope';
import { isNotEmpty, isNotNull, isNull } from '../utils';
import { HEADERS } from './header.constants';

@Injectable({
  providedIn: 'root'
})
export class HeaderServices {

  /**************************************************************************
  * CONSTRUCTORS
  **************************************************************************/
  constructor(@Inject(SessionScope) private session: SessionScope) {
  }

  /**************************************************************************
  * API
  **************************************************************************/
  public buildHeader(headerInfos:any): RequestOptions {
    let headerData = isNull(headerInfos) ? {} : headerInfos;
    headerData[HEADERS.deviceIdentifier] = this.session.getDeviceIdentifier();
    
    if(isNotEmpty(this.session.getCorrelationId())){
      headerData[HEADERS.correlationId] = this.session.getCorrelationId();
    }

    if(isNotNull(headerInfos)){
      for(let key of Object.keys(headerInfos)){
        headerData[key]=headerInfos[key];
      }
   }

    return new HttpHeaders(headerData);
  }



}
