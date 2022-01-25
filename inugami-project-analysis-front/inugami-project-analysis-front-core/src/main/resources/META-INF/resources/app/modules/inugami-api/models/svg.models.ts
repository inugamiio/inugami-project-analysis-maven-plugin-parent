export interface TransformationInfo {
    x:number,
    y:number,
    scaleX:number,
    scaleY:number
}

export interface Dimension {
    height    : number,
    width     : number,
    font      : number
}

export interface Position{
    x      : number,
    y      : number
    cmd?   : string
}


export interface Size{
    bottom    : number,
    height    : number,
    left      : number,
    right     : number,
    top       : number,
    width     : number,
    x         : number,
    y         : number,
    fontratio : number
}
