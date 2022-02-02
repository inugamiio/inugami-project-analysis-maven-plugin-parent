import { Injectable, Inject } from '@angular/core';
import { HttpClient, HttpResponse, HttpHeaders, HttpRequest } from '@angular/common/http';
import { SessionScope } from '../scopes/session.scope';
import { HeaderServices } from './header.services';
import { HEADERS } from './header.constants';
import { isNotNull , isNull} from '../utils';


@Injectable({
    providedIn: 'root'
})
export class HttpService {

    /***************************************************************************
     * CONSTRUCTORS
     ***************************************************************************/
    constructor(
        @Inject(HttpClient) private http: HttpClient,
        @Inject(SessionScope) private session: SessionScope,
        @Inject(HeaderServices) private headerServices: HeaderServices
        ) { }

    /***************************************************************************
    * API
    ***************************************************************************/

    public get(url: string, header?: any): Promise<any> {
        let options = this.headerServices.buildHeader(header);

        return this.http.get(url, { "headers": options, observe: "response" })
            .toPromise()
            .then(res => {
                this.defineCorrelationId(res);
                return res;
            })
            .catch(this.handleError);
    }


    public post(url: string, data?, header?: any): Promise<any> {
        let options = this.headerServices.buildHeader(header);
        return this.http
            .post(url, JSON.stringify(data), { "headers": options, observe: "response" })
            .toPromise()
            .then(res => {
                this.defineCorrelationId(res);
                return res;
            })
            .catch(this.handleError);
    }

    public put(url: string, data, header?: any): Promise<any> {
        let options = this.headerServices.buildHeader(header);
        return this.http
            .put(url, JSON.stringify(data), { "headers": options, observe: "response" })
            .toPromise()
            .then(res => {
                this.defineCorrelationId(res);
                return res;
            })
            .catch(this.handleError);
    }

    public patch(url: string, data, header?: any): Promise<any> {
        let options = this.headerServices.buildHeader(header);
        return this.http
            .patch(url, JSON.stringify(data), { "headers": options, observe: "response" })
            .toPromise()
            .then(res => {
                this.defineCorrelationId(res);
                return res;
            })
            .catch(this.handleError);
    }

    public delete(url: string, data?, header?: any): Promise<any> {
        let options = this.headerServices.buildHeader(header);

        return this.http
            .delete(url, { "headers": options, observe: "response" })
            .toPromise()
            .then(res => {
                this.defineCorrelationId(res);
                return res;
            })
            .catch(this.handleError);
    }


    /**************************************************************************
    * TOOLS
    **************************************************************************/
    private defineCorrelationId(response:HttpResponse){
        let correlationId = null;
        if(isNotNull(response) && isNotNull(response.headers)){
            correlationId = response.headers.get(HEADERS.correlationId);
        }
        if(isNotNull(correlationId)){
            this.session.setCorrelationId(correlationId);
        }
    }
    /**************************************************************************
    * HANDLING ERRORS
    **************************************************************************/
    private handleError(error: any): Promise<any> {

        let errorData = {
            status: error.status || 500,
            error_code:isNull(error.headers)?"err-0":error.headers.get("error_code"),
            error_message:isNull(error.headers)?null:error.headers.get("error_message"),
            error_exception_message:isNull(error.headers)?null:error.headers.get("error_exception_message"),
            error_type:isNull(error.headers)?null:error.headers.get("error_type"),
            data: null
        }

        if (isNotNull(error._body)) {
            let json = null;
            try {
                json = JSON.parse(error._body);
                errorData.data = json;
            } catch (err) {
                errorData.data = error._body + " \n" + error.statusText;
                errorData.data = errorData.data.replace(/&quot;/g, '"')
            }

        }
        console.error(errorData);
        return Promise.reject(errorData);
    }
}