import { Component, forwardRef, OnInit, Inject } from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { MessageComponent } from '../message.component';
import { MessageService } from '../../../inugami-api/services/message.service';
import { ReleaseNote,Issue } from '../../models/release.note';
import { DynamicComponentLoader } from '../dynamic-component-loader/dynamic.component.loader';
import { ChangeComponent } from '../change/change.component';
import { ProjectDependencyGraphComponent } from '../dependencies/project.dependency.graph.component';

export const RELEASE_NOTE_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => ReleaseNoteComponent),
  multi: true
};


@Component({
  selector: 'release-note',
  templateUrl: './app/modules/inugami-project-analysis-plugin/components/release-note/release.note.component.html',
  directives: [DynamicComponentLoader, MessageComponent, ChangeComponent,ProjectDependencyGraphComponent],
  providers: [RELEASE_NOTE_VALUE_ACCESSOR]
})
export class ReleaseNoteComponent implements ControlValueAccessor, OnInit {

  /*****************************************************************************
  * ATTRIBUTES
  *****************************************************************************/
  private innerValue: ReleaseNote = null;
  private showDetail: boolean = false;
  private showDetailLabel: string = "show detail";
  private loading: boolean = false;

  /*****************************************************************************
  * INIT
  *****************************************************************************/
  constructor(@Inject(MessageService) private messageService: MessageService) {
  }

  ngOnInit() {
    this.showDetailLabel = this.messageService.getMessage('detail.show');
  }
  
  /*****************************************************************************
  * API
  *****************************************************************************/
  public getAuthors() {
    var authors = [];
    var rawAuthors = null;
    if (this.innerValue != null) {
      rawAuthors = this.innerValue.authors;
    }

    if (rawAuthors != null) {
      for (var i = 0; i < rawAuthors.length; i++) {
        var author = rawAuthors[i];
        if (authors.indexOf(author.name) == -1) {
          authors.push(author.name);
        }
      }
    }
    return authors.join(', ');
  }

  public toggleDetail() {
    if (this.showDetail) {
      this.showDetailLabel = this.messageService.getMessage('detail.show');
    } else {
      setTimeout(() => {
        this.loading = true;
        this.showDetailLabel = this.messageService.getMessage('detail.hide');
      }, 200);

    }
    this.showDetail = !this.showDetail;
  }

  public loadingStop() {
    this.loading = false;
    return "release-note-end"
  }
  hasContent(data: any) {
    return data != undefined && data != null && data.length > 0;
  }

  resolveIssueLabel(issue:Issue):string{
    if(issue!=null && issue.labels !=null){

      let result = [];
      for(let item in issue.labels){
        result.push(issue.labels[item].toLowerCase());
      }

      return result.join(" ");
    }
    return "default";
  }

  public sortData(){
    if(this.innerValue==undefined || this.innerValue==null){
       return;
    }

    this.sortIssues();
  }

  public sortIssues(){
    if(this.innerValue.issues==undefined || this.innerValue.issues==null){
      return;
   }

   this.innerValue.issues.sort((ref,value)=>{
     let refHash = this.buildIssueHash(ref);
     let valueHash = this.buildIssueHash(value);
     return refHash.localeCompare(valueHash);
   });
  }

  public buildIssueHash(issue:any):string{
    if(issue==undefined|| issue==null){
      return "";
    }
    let value = [];
    if(issue.labels != undefined && issue.labels!=null){
      issue.labels.sort();
      value.push(issue.labels.join("_"));
    }else{
      value.push("undefined");
    }

    value.push(issue.name==undefined || issue.name==null ? "undefined":issue.name);
    return value.join(":");
  }

  /*****************************************************************************
  * IMPLEMENTS ControlValueAccessor
  *****************************************************************************/
  writeValue(value: any) {
    if (value !== this.innerValue) {
      this.innerValue = value;
      this.sortData();
     
    }
  }

  registerOnChange(fn: any) {
    this.onChangeCallback = fn;
  }
  registerOnTouched(fn: any) {
    this.onTouchedCallback = fn;
  }
}
