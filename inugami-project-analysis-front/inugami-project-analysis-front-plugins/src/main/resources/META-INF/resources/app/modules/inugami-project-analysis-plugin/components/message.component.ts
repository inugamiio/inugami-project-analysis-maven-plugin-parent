import { Component, OnInit, Input, Inject } from '@angular/core';
import { MessageService } from '../../inugami-api/services/message.service';

@Component({
    selector: 'msg',
    template: `{{message}}`,
    providers: []
})
export class MessageComponent implements  OnInit{
    @Input() private key: string;
    @Input()  values      : any[];

    private message: string ;


    constructor(@Inject(MessageService) private messageService: MessageService) {
    }

    ngOnInit(){
        this.message = this.messageService.getMessage(this.key, this.values);
    }
}