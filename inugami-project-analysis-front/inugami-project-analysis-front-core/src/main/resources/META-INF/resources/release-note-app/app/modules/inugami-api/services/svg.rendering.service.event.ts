import * as d3 from 'd3';
import { Observable, Observer, Subscriber } from 'rxjs'
import { isNotNull, isNull } from './utils';
import { svg } from './svg.rendering.service';
import { Position } from '../models/svg.models';

export const mousePosition : Position = {x:0, y:0};

class HandlerEvent<T>{
    observable: Observable<T> = null;
    observer: Observer;

    constructor(private eventName: string,
        private processor?: Function) {
        this.eventName = eventName;

        this.observable = new Observable((observer) => {
            this.observer = observer;
        });

        let currentEvent = eventName;
        document.addEventListener(eventName, (data) => {
            if(isNotNull(this.observer)){
                let realData = isNull(this.processor) ? data : this.processor(data);
                this.observer.next(realData);
            }
        });
    }
}


const localObservers = {
    onMouseMove: new HandlerEvent<Position>("mousemove", (event) => {
        return { x: event.pageX, y: event.pageY };
    })
};

export const event = {

    mousePosition: (): Position => {
        return {
            x: document['mouseX'],
            y: document['mouseY']
        }
    },

    onMouseMove: localObservers.onMouseMove.observable
};


const mouseTracker = event.onMouseMove.subscribe({
    next: (mousePos) => {
        mousePosition.x =  mousePos.x;
        mousePosition.y =  mousePos.y;
    }
});