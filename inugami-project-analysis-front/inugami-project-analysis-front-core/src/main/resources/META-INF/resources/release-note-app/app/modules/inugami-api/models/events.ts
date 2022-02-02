export const highlightEvent : Event =  new Event('highlight');

export const fireEvent = (event:Event, callback?:any) => {
    document.dispatchEvent(event);

    if(callback != null){
        callback();
    }
}